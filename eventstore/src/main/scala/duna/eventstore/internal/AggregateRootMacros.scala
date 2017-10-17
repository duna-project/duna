package duna.eventstore.internal

import java.lang.reflect.Method
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect._
import scala.reflect.internal.util.Collections
import scala.reflect.macros.blackbox
import scala.util.{Failure, Success, Try}

import duna.eventstore.api._

object AggregateRootMacros {

  val idSetter: Method = classOf[AggregateRoot[_]]
    .getDeclaredMethod(NameTransformer.encode("_id_="), classOf[UUID])

  val versionSetter: Method = classOf[AggregateRoot[_]]
    .getDeclaredMethod(NameTransformer.encode("_version_="), classOf[UUID])

  def setAggregateId(aggregateRoot: AggregateRoot[_], id: UUID) =
    aggregateRoot._id = id

  def runtimeNewInstance[T <: AggregateRoot[T] : ClassTag]: T = {
    val runtimeClass = classTag[T].runtimeClass

    Try(runtimeClass.newInstance()) match {
      case Success(instance) => instance.asInstanceOf[T]
      case Failure(_) =>
        val constructor = runtimeClass.getConstructors.head
        val params: Array[AnyRef] = for (param <- constructor.getParameterTypes)
          yield {
            if (param.isPrimitive) {
              param.getSimpleName match {
                case "boolean" => Boolean.box(false)
                case _ => Int.box(0)
              }
            } else null
          }

        constructor.newInstance(params: _*).asInstanceOf[T]
    }
  }

  def newEntity[T <: AggregateRoot[T] : c.WeakTypeTag](c: blackbox.Context): c.Expr[Future[T]] = {
    import c.universe._

    newEntityWithUUID(c)(reify[UUID](UUID.randomUUID()))
  }

  def newEntityWithUUID[T <: AggregateRoot[T] : c.WeakTypeTag](c: blackbox.Context)
                                                              (id: c.Expr[UUID]): c.Expr[Future[T]] = {
    import c.universe._

    val constructor = weakTypeOf[T].decls
      .find {
        case m: MethodSymbol if m.isPrimaryConstructor => true
        case _ => false
      }

    if (constructor.isEmpty)
      return c.abort(c.enclosingPosition,
        s"Class ${weakTypeOf[T].typeSymbol.fullName} doesn't have a primary constructor.")

    val paramValues = constructor.get.asMethod.paramLists
      .map { pl =>
        pl.map {
          case s if s.typeSignature.resultType <:< typeOf[Boolean] => reify(false).tree
          case s if s.typeSignature.resultType <:< typeOf[AnyVal] => reify(0).tree
          case s if s.typeSignature.resultType <:< typeOf[Traversable[_]] =>
            val companionApply = s.typeSignature.dealias.companion.typeSymbol

            q"scala.util.Try($companionApply.apply()).toOption.getOrElse(null)"
          case _ => reify(null).tree
        }
      }

    val instance = c.Expr[T](q"new ${weakTypeOf[T]}(...$paramValues)")

    reify[Future[T]] {
      Future.successful {
        val result = instance.splice
        val newId = id.splice

        idSetter.invoke(result, newId)
        result
      }
    }
  }

  def existingEntityMacro[T <: AggregateRoot[T] : c.WeakTypeTag](c: blackbox.Context)
                                                                (id: c.Expr[UUID])
                                                                (ctag: c.Expr[ClassTag[T]],
                                                                 eventStore: c.Expr[EventStore]): c.Expr[Future[T]] = {
    import c.universe._

    val executionContext = c.Expr[ExecutionContext](c.inferImplicitValue(typeOf[ExecutionContext]))

    val newInstance = newEntityWithUUID[T](c)(id)

    reify[Future[T]] {
      eventStore.splice.get[T](id.splice)(ctag.splice)
        .flatMap({ eventList =>
          newInstance.splice.map { entity =>
            entity.consume(eventList)
          }(executionContext.splice)
        })(executionContext.splice)
    }
  }

  def processCommandMacro[T <: AggregateRoot[T] : c.WeakTypeTag](c: blackbox.Context)
                                                                (command: c.Expr[Command])
                                                                (eventStore: c.Expr[EventStore]): c.Expr[Future[T]] = {
    import c.universe._

    val executionContext = c.Expr[ExecutionContext](c.inferImplicitValue(typeOf[ExecutionContext]))
    val ctag = c.Expr[ClassTag[T]](c.inferImplicitValue(typeOf[ClassTag[_]]))

    val aggregateRoot = c.Expr[T](q"this.aggregateRoot")
    val newInstance = newEntityWithUUID[T](c)(c.Expr[UUID](q"this.aggregateRoot.id"))

    reify[Future[T]] {
      eventStore.splice
        .store[T](aggregateRoot.splice.id, aggregateRoot.splice.process(command.splice): _*)(ctag.splice)
        .flatMap { eventList =>
          newInstance.splice.map(_.consume(eventList))(executionContext.splice)
        }(executionContext.splice)
    }
  }
}
