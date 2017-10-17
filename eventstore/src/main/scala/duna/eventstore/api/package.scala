package duna.eventstore

import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.ClassTag

import duna.eventstore.internal.{AggregateRootMacros, TransactionMacros}

package object api {

  type EventStream = List[_ <: Event]
  type EventList = List[_ <: Event]

  def newEntity[T <: AggregateRoot[T]]: Future[T] = macro AggregateRootMacros.newEntity[T]

  def newEntity[T <: AggregateRoot[T]](id: UUID): Future[T] = macro AggregateRootMacros.newEntityWithUUID[T]

  def existing[T <: AggregateRoot[T] : ClassTag](id: UUID)(implicit eventStore: EventStore): Future[T] =
  macro AggregateRootMacros.existingEntityMacro[T]

  def atomic[T <: AggregateRoot[T]](f: => Future[T]): Future[T] = macro TransactionMacros.transformAtomicBlock[T]

  implicit class FutureAggregateOperators[T <: AggregateRoot[T] : ClassTag](val future: Future[T]) {

    def <==(command: Command)(implicit eventStore: EventStore,
                              executionContext: ExecutionContext): Future[T] = process(command)

    def process(command: Command)(implicit eventStore: EventStore,
                                  executionContext: ExecutionContext): Future[T] = future.flatMap { aggregate =>
      val eventList = aggregate.process(command)

      eventStore
        .store(aggregate.id, eventList: _*)
        .map(_ => aggregate.consume(eventList))
    }
  }

}
