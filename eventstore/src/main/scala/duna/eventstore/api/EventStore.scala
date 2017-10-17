package duna.eventstore.api

import java.time.Instant
import java.util.UUID

import scala.concurrent.Future
import scala.reflect.ClassTag

import org.reactivestreams.Subscriber

trait EventStore {

  def get[T <: AggregateRoot[T] : ClassTag](id: UUID): Future[EventList]

  def getOption[T <: AggregateRoot[T] : ClassTag](id: UUID): Future[Option[EventList]]

  def getAt[T <: AggregateRoot[T] : ClassTag](id: UUID, instant: Instant): Future[EventList]

  def getOptionAt[T <: AggregateRoot[T] : ClassTag](id: UUID, instant: Instant): Future[Option[EventList]]

  def subscribe[T <: AggregateRoot[T] : ClassTag](id: UUID): Subscriber[Event]

  def store[T <: AggregateRoot[T] : ClassTag](id: UUID, events: Event*): Future[EventList]

}
