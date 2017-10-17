package duna.perf

import duna.eventstore.api.AggregateRoot

class PurchaseOrder() extends AggregateRoot[PurchaseOrder] {
  override def consume = ???

  override def process = ???
}
