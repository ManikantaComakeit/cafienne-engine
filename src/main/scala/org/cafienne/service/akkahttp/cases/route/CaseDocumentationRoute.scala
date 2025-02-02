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

package org.cafienne.service.akkahttp.cases.route

import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.service.akkahttp.Headers
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseDocumentationRoute(override val caseSystem: CaseSystem) extends CasesRoute {
  override def routes: Route = concat(getPlanItemDocumentation, getCaseFileDocumentation)

  @Path("/{caseInstanceId}/documentation/planitems/{planItemId}")
  @GET
  @Operation(
    summary = "Get the documentation information from the plan item's definition",
    description = "Get the documentation information from the plan item's definition",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "planItemId", description = "Unique id of the planItem", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Plan item documentation found", responseCode = "200"),
      new ApiResponse(description = "Plan item not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItemDocumentation: Route = get {
    caseUser { user =>
      path(Segment / "documentation" / "planitems" / Segment) {
        (_, planItemId) => runQuery(caseQueries.getPlanItemDocumentation(planItemId, user))
      }
    }
  }

  @Path("/{caseInstanceId}/documentation/casefile")
  @GET
  @Operation(
    summary = "Get the casefile documentation",
    description = "Returns a list with the documentation for case file items that are documented in the definition of the case",
    tags = Array("case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Case file documentation", responseCode = "200"),
      new ApiResponse(description = "No case file found for the case instance", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCaseFileDocumentation: Route = get {
    caseUser { user =>
      path(Segment / "documentation" / "casefile") {
        caseInstanceId => runQuery(caseQueries.getCaseFileDocumentation(caseInstanceId, user))
      }
    }
  }
}
