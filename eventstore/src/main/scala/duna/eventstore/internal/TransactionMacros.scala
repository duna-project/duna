package duna.eventstore.internal

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

import duna.eventstore.api.{AggregateRoot, EventStore, FutureAggregateOperators}
import duna.eventstore.transaction.TransactionContext

object TransactionMacros {

  def transaction[T <: AggregateRoot[T]](block: => Future[T]): Future[T] = block

  def transformAtomicBlock[T <: AggregateRoot[T] : c.WeakTypeTag](c: blackbox.Context)
                                                                 (f: c.Tree): c.Tree = {
    import c.universe._

    checkEventStoreAccess(c)(f)

    val implicitValue = c.inferImplicitValue(typeOf[EventStore])

    val implicitEventStore = c.Expr[EventStore](implicitValue)
    val classType = weakTypeOf[T].typeSymbol

    object atomicBlockTransformer extends Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case access @ Select(_, _)
            if access.symbol.asTerm.typeSignature.resultType <:< typeOf[EventStore] =>
            q"$$tx$$context.eventStore"
          case _ => super.transform(tree)
        }
      }
    }

    val transformedTree = atomicBlockTransformer.transform(f)
    val block = c.Expr[Future[T]](c.untypecheck(transformedTree))
    val transactionContext = c.Expr[TransactionContext[T]](
      q"new duna.eventstore.transaction.TransactionContext[$classType]($implicitEventStore)")

    val executionContext = c.Expr[ExecutionContext](c.inferImplicitValue(typeOf[ExecutionContext]))

    val newAggregateInstance = AggregateRootMacros.newEntity[T](c)

    reify[Future[T]] {
      {
        val $tx$context = transactionContext.splice

        block.splice
          .flatMap({ _: T =>
            newAggregateInstance.splice
              .flatMap(instance => $tx$context.commit(instance))(executionContext.splice)
          })(executionContext.splice)
          .recover(PartialFunction { e: Throwable =>
            $tx$context.rollback(e)
          })(executionContext.splice)
      }
    }.tree
  }

  private def checkEventStoreAccess[T <: AggregateRoot[T] : c.WeakTypeTag](c: blackbox.Context)
                                                                          (f: c.Tree): Boolean = {
    import c.universe._

    val extClassName = typeOf[FutureAggregateOperators[_]].typeSymbol.fullName
    val aggregateType = weakTypeOf[T].typeSymbol

    object transactionCodeTraverser extends Traverser {
      override def traverse(tree: c.universe.Tree): Unit = tree match {
        case q"TransactionMacros.this.transaction { ..$body }" =>
          c.abort(tree.pos, "atomic blocks cannot be nested")

        case q"$clazz[$tp](..$_)(..$_)" if clazz.symbol.fullName == extClassName
          && tp.symbol.fullName != aggregateType.fullName =>
          c.abort(tree.pos, s"cannot process $tp in an atomic block reserved for ${aggregateType.fullName}")

        case _ => super.traverse(tree)
      }
    }

    transactionCodeTraverser.traverse(f)

    true
  }
}
