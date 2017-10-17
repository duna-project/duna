package duna.eventstore.event

import duna.eventstore.api.EventStream

case class EventAggregate(version: Long,
                          eventStream: EventStream)