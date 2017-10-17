package duna.eventstore.test.support

import duna.eventstore.api.Command

sealed trait PurchaseCommand extends Command

case class CreatePurchaseOrder(customer: String) extends PurchaseCommand

case class AddOrderItem(description: String, price: Int) extends PurchaseCommand

case class ChangeCustomerName(customer: String) extends PurchaseCommand

