/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.service.akkahttp.cases.route.deprecated

import akka.http.scaladsl.server.Route
import org.cafienne.cmmn.actorapi.command.team.DeprecatedUpsert
import org.cafienne.cmmn.actorapi.command.team.removemember.{RemoveCaseTeamTenantRole, RemoveCaseTeamUser}
import org.cafienne.service.akkahttp.cases.model.CaseTeamAPI.Compatible._
import org.cafienne.service.akkahttp.cases.route.CasesRoute
import org.cafienne.system.CaseSystem

class DeprecatedCaseTeamRoute(override val caseSystem: CaseSystem) extends CasesRoute {
  override val addToSwaggerRoutes = false
  override def routes: Route = concat(putCaseTeamMember, deleteCaseTeamMember)

  def putCaseTeamMember: Route = put {
    caseInstanceSubRoute { (user, caseInstanceId) => {
      path("caseteam") {
        entity(as[BackwardCompatibleTeamMemberFormat]) { input =>
          askCase(user, caseInstanceId, caseOwner => new DeprecatedUpsert(caseOwner, caseInstanceId, input.upsertMemberData))
        }
      }
    }
    }
  }

  def deleteCaseTeamMember: Route = delete {
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("caseteam" / Segment) { memberId =>
        parameters("type".?) { memberType =>
          if (memberType.nonEmpty && memberType.get == "role") {
            askCase(user, caseInstanceId, tenantUser => new RemoveCaseTeamTenantRole(tenantUser, caseInstanceId, memberId))
          } else {
            askCase(user, caseInstanceId, tenantUser => new RemoveCaseTeamUser(tenantUser, caseInstanceId, memberId))
          }
        }
      }
    }
  }
}
