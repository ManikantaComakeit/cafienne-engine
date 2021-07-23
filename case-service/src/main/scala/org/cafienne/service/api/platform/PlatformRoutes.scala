/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.platform

import io.swagger.v3.oas.annotations.security.SecurityRequirement

import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.route.CommandRoute
import org.cafienne.service.db.query.PlatformQueries
import org.cafienne.system.CaseSystem

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/platform")
class PlatformRoutes(platformQueries: PlatformQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends CommandRoute {
  override val prefix: String = "platform"

  addSubRoute(new PlatformRoute(platformQueries))
}
