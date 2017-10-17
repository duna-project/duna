package duna.eventstore.test.support

import duna.eventstore.api._

case class PurchaseOrder(customer: String,
                         orderItems: List[OrderItem])
  extends AggregateRoot[PurchaseOrder] {

  override def consume = {
    case e: PurchaseOrderCreated =>
      this.copy(e.customer, List.empty)

    case e: OrderItemAdded =>
      val orderItem = OrderItem(e.description, e.price)
      this.copy(orderItems = this.orderItems :+ orderItem)

    case e: CustomerNameChanged =>
      this.copy(customer = e.customer)
  }

  override def process = {
    case c: CreatePurchaseOrder =>
      List(PurchaseOrderCreated(c.customer))

    case c: AddOrderItem =>
      List(OrderItemAdded(c.description, c.price))

    case c: ChangeCustomerName =>
      List(CustomerNameChanged(c.customer))
  }
}

case class OrderItem(description: String,
                     price: Int)
  extends ValueObject