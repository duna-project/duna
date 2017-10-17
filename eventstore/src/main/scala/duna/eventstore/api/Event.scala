package duna.eventstore.api

import java.time.ZonedDateTime
import java.util.UUID

import duna.eventstore.util.UUIDs._

trait Event {

  private[eventstore] var _version: UUID = _

  final def version: UUID = _version

  final def committedAt: ZonedDateTime = _version.dateTime

}