package duna.perf

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import scala.reflect.NameTransformer

import duna.eventstore.api._
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
@Fork(2)
class BenchmarkReflection {

  val uuid = UUID.randomUUID()

  val map = new ConcurrentHashMap[String, List[Int]]()

  val method = classOf[AggregateRoot[_]].getDeclaredMethod(NameTransformer.encode("_id_="), classOf[UUID])

  @Setup
  def setup(): Unit = {
  }

//  @Benchmark
//  def uuidConstruction(blackhole: Blackhole): Unit = {
//    blackhole.consume(UUID.randomUUID())
//  }
//
  @Benchmark
  def entityReconstruction(blackhole: Blackhole): Unit = {
    val entity = newEntity[PurchaseOrder](uuid)

    method.invoke(entity, uuid)

    blackhole.consume(entity)
  }

//  @Benchmark
//  def entityReconstructionProvided(blackhole: Blackhole): Unit = {
//    blackhole.consume(newEntity[PurchaseOrder](uuid))
//  }

//  @Benchmark
//  def entityCopy(blackhole: Blackhole): Unit = {
//    blackhole.consume(entity.copy(customer = "asdasd"))
//  }
}
