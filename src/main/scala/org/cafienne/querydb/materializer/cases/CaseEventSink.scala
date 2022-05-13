package org.cafienne.querydb.materializer.cases

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.querydb.materializer.{QueryDBEventSink, QueryDBOffsetStore}
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class CaseEventSink(val caseSystem: CaseSystem) extends QueryDBEventSink with LazyLogging {
  override val tag: String = CaseEvent.TAG

  override def getOffset: Future[Offset] = QueryDBOffsetStore(CaseEventSink.offsetName).getOffset

  override def createBatch(persistenceId: String): CaseEventBatch = new CaseEventBatch(this, persistenceId)
}

object CaseEventSink {
  val offsetName = "CaseEventSink"
}
