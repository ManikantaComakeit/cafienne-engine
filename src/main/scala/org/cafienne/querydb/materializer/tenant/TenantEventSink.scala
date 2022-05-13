package org.cafienne.querydb.materializer.tenant

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.cqrs.batch.EventBatch
import org.cafienne.querydb.materializer.{QueryDBEventSink, QueryDBOffsetStore}
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.event.TenantEvent

import scala.concurrent.Future

class TenantEventSink(val caseSystem: CaseSystem) extends QueryDBEventSink with LazyLogging {
  override val tag: String = TenantEvent.TAG

  override def getOffset: Future[Offset] = QueryDBOffsetStore(TenantEventSink.offsetName).getOffset

  override def createTransaction(batch: EventBatch): TenantTransaction = new TenantTransaction(batch, caseSystem.userCache)
}

object TenantEventSink {
  val offsetName = "TenantEventSink"
}
