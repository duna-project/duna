package duna.eventstore.test

import scala.async.Async._

import duna.eventstore.SimpleEventStore
import duna.eventstore.api._
import duna.eventstore.test.support.{AddOrderItem, ChangeCustomerName, CreatePurchaseOrder, PurchaseOrder}
import org.scalatest.{AsyncFlatSpec, MustMatchers}

class TransactionContextSpec extends AsyncFlatSpec with MustMatchers {

  private implicit val eventStore: EventStore = new SimpleEventStore

  it must "store the events published in the transaction" in {
    val purchaseOrder = atomic[PurchaseOrder] { async {
      val entity = await (newEntity[PurchaseOrder] <== CreatePurchaseOrder("some customer"))

      await (existing[PurchaseOrder](entity.id) <== AddOrderItem("first item", 1))
      await (existing[PurchaseOrder](entity.id) <== AddOrderItem("second item", 2))
      await (existing[PurchaseOrder](entity.id) <== AddOrderItem("third item", 3))

      await (existing[PurchaseOrder](entity.id) <== ChangeCustomerName("modified customer"))
    } }

    purchaseOrder.map { entity =>
      entity.orderItems.size must be (3)
      entity.customer must be ("modified customer")
    }
  }
}
