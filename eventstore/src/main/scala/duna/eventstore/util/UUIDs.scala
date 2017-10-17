package duna.eventstore.util

import java.time._
import java.util.UUID

import com.fasterxml.uuid.UUIDComparator

object UUIDs {

  private val epochToUnixDiff = 12219292800000L

  def unixTimestamp(uuid: UUID): Long = {
    uuid.timestamp() + epochToUnixDiff
  }

  implicit class UUIDExtension(val uuid: UUID) extends AnyVal {

    def <(other: UUID): Boolean = {
      UUIDComparator.staticCompare(uuid, other) < 0
    }

    def >(other: UUID): Boolean = {
      UUIDComparator.staticCompare(uuid, other) > 0
    }

    def <=(other: UUID): Boolean = {
      UUIDComparator.staticCompare(uuid, other) <= 0
    }

    def >=(other: UUID): Boolean = {
      UUIDComparator.staticCompare(uuid, other) >= 0
    }

    def ==(other: UUID): Boolean = {
      UUIDComparator.staticCompare(uuid, other) == 0
    }

    def dateTime: ZonedDateTime = {
      val instant = Instant.ofEpochMilli(uuid.timestamp() + epochToUnixDiff)

      ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
        .withZoneSameInstant(ZoneId.systemDefault())
    }
  }

}
