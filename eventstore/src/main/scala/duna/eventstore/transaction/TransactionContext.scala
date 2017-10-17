package duna.eventstore.transaction

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import duna.eventstore.api._

class TransactionContext[T <: AggregateRoot[T] : ClassTag](scopedEventStore: EventStore)
                                                          (implicit executionContext: ExecutionContext) {

  val eventStore = new TransactionEventStore[T](scopedEventStore)

  def commit(instance: T): Future[T] = {
    instance._id = eventStore.entityId

    scopedEventStore.store[T](eventStore.entityId, eventStore.data: _*)
      .map {
        _.foldLeft(instance) { (entity, event: Event) =>
          entity consume event
        }
      }
  }

  def rollback(e: Throwable): Nothing = {
    throw TransactionFailedException(e.getMessage, e)
  }

}
