package org.cafienne.service.db.materializer.cases

import org.cafienne.cmmn.actorapi.event.CaseDefinitionApplied
import org.cafienne.service.db.record.CaseRoleRecord

object CaseInstanceRoleMerger {

  import scala.jdk.CollectionConverters._

  def merge(event: CaseDefinitionApplied): Seq[CaseRoleRecord] = {
    val caseDefinition = event.getDefinition()
    caseDefinition.getCaseTeamModel().getCaseRoles().asScala.toSeq.map(role => CaseRoleRecord(event.getCaseInstanceId, event.tenant, role.getName, assigned = false))
  }

}
