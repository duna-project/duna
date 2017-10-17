package duna.eventstore

import java.time.{Instant, ZoneId}
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import scala.concurrent.Future
import scala.reflect._

import com.fasterxml.uuid.Generators
import duna.eventstore.api.{AggregateRoot, Event, EventList, EventStore}

class SimpleEventStore extends EventStore {

  val data = new ConcurrentHashMap[(Class[_], UUID), EventList]()

  override def get[T <: AggregateRoot[T] : ClassTag](id: UUID): Future[EventList] = {
    val dataKey = classTag[T].runtimeClass -> id
    Future.successful(data.getOrDefault(dataKey, List()))
  }

  override def getOption[T <: AggregateRoot[T] : ClassTag](id: UUID): Future[Option[EventList]] = {
    val dataKey = classTag[T].runtimeClass -> id
    Future.successful(Option(data.get(dataKey)))
  }

  override def getAt[T <: AggregateRoot[T] : ClassTag](id: UUID, instant: Instant): Future[List[Event]] = {
    val dataKey = classTag[T].runtimeClass -> id
    Future.successful {
      data.getOrDefault(dataKey, List())
        .filter(_.committedAt.isBefore(instant.atZone(ZoneId.systemDefault())))
    }
  }

  override def getOptionAt[T <: AggregateRoot[T] : ClassTag](id: UUID, instant: Instant): Future[Option[List[Event]]] = {
    val dataKey = classTag[T].runtimeClass -> id
    Future.successful {
      Option(data.get(dataKey, List()))
        .map {
          _.filter(_.committedAt.isBefore(instant.atZone(ZoneId.systemDefault())))
        }
    }
  }

  override def subscribe[T <: AggregateRoot[T] : ClassTag](id: UUID) = ???

  override def store[T <: AggregateRoot[T] : ClassTag](id: UUID, events: Event*): Future[EventList] = {
    val dataKey = classTag[T].runtimeClass -> id

    val result = data.merge(dataKey, events.toList, { (listA, listB) =>
      val version = Generators.timeBasedGenerator().generate()

      events.foreach { event =>
        event._version = version
      }

      listA ++ listB
    })

    Future.successful(result)
  }
}
