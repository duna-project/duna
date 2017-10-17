package duna.eventstore.api

import java.util.UUID

import scala.concurrent.Future
import scala.language.experimental.macros
import scala.reflect.ClassTag

import duna.eventstore.internal.AggregateRootMacros

trait AggregateRoot[T] { this: T with AggregateRoot[T] =>

  private[eventstore] var _id: UUID = _

  private[eventstore] var _version: UUID = _

  final def id: UUID = _id

  final def version: UUID = _version

  def consume: PartialFunction[Event, T]

  final def consume(eventList: EventList): T = {
    eventList.foldLeft(this) { (instance, event) =>
      val newInstance = instance.consume.applyOrElse(event, (_: Event) => instance)
      newInstance.asInstanceOf[AggregateRoot[T]]._id = instance.id
      newInstance.asInstanceOf[AggregateRoot[T]]._version = event.version
      newInstance.asInstanceOf[T with AggregateRoot[T]]
    }
  }

  def process: PartialFunction[Command, EventList]

}

object AggregateRoot {

//  implicit class AggregateRootOperators[T <: AggregateRoot[T] : ClassTag](val aggregateRoot: T) {
//
//    final def <==(command: Command)(implicit eventStore: EventStore)
//    : Future[T] = macro AggregateRootMacros.processCommandMacro[T]
//
//  }
}
