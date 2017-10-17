package duna.eventstore.transaction

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect._

import duna.eventstore.api.{AggregateRoot, Event, EventList, EventStore}

class TransactionEventStore[A <: AggregateRoot[A] : ClassTag](scopedEventStore: EventStore)
                                                             (implicit executionContext: ExecutionContext)
  extends EventStore {

  private[transaction] var data: EventList = List[Event]()
  private[transaction] var entityId: UUID = _

  override def get[T <: AggregateRoot[T] : ClassTag](id: UUID): Future[EventList] = {
    require(classTag[T] == classTag[A])

    if (entityId == null) {
      scopedEventStore
        .get[T](id)
        .map { events =>
          entityId = id
          events
        }
    } else if (id != entityId) Future.successful(List.empty)
    else Future.successful(data)
  }

  override def getOption[T <: AggregateRoot[T] : ClassTag](id: UUID): Future[Option[EventList]] = {
    require(classTag[T] == classTag[A])

    if (entityId == null) {
      scopedEventStore
        .get[T](id)
        .map { events =>
          entityId = id
          Some(events)
        }
    } else if (id != entityId) Future.successful(None)
    else Future.successful(Some(data))
  }

  override def subscribe[T <: AggregateRoot[T] : ClassTag](id: UUID) = ???

  override def store[T <: AggregateRoot[T] : ClassTag](id: UUID, events: Event*): Future[EventList] = {
    require(classTag[T] == classTag[A])
    require(entityId == null || entityId == id)

    entityId = id

    data ++= events

    Future.successful(data)
  }

  override def getAt[T <: AggregateRoot[T] : ClassTag](id: UUID, instant: Instant) = ???

  override def getOptionAt[T <: AggregateRoot[T] : ClassTag](id: UUID, instant: Instant) = ???
}
