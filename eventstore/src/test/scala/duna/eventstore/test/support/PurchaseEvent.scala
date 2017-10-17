package duna.eventstore.test.support

import java.util.UUID

import duna.eventstore.api.Event

sealed trait PurchaseEvent extends Event

case class PurchaseOrderCreated(customer: String) extends PurchaseEvent

case class OrderItemAdded(description: String, price: Int) extends PurchaseEvent

case class CustomerNameChanged(customer: String) extends PurchaseEvent
