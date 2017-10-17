package duna.eventstore.test

import duna.eventstore.SimpleEventStore
import duna.eventstore.api._
import duna.eventstore.test.support._
import org.scalatest.{AsyncFlatSpec, MustMatchers}

class EventStoreSpec extends AsyncFlatSpec
  with MustMatchers {

  private implicit val eventStore: EventStore = new SimpleEventStore

  it must "store events for an aggregate and reconstruct it" in {
    (for {
      purchaseOrder <- newEntity[PurchaseOrder] <== CreatePurchaseOrder("some customer")

      _ <- existing[PurchaseOrder](purchaseOrder.id)
        .process(AddOrderItem("first item", 1))
        .process(AddOrderItem("second item", 2))
        .process(AddOrderItem("third item", 3))

      _ <- existing[PurchaseOrder](purchaseOrder.id) <== ChangeCustomerName("modified customer")

      current <- existing[PurchaseOrder](purchaseOrder.id)
    } yield current).map { current =>
      current.orderItems.size must be(3)
      current.customer must be("modified customer")
    }
  }

}
