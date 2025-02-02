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

package org.cafienne.service.akkahttp.tenant.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.querydb.materializer.LastModifiedRegistration
import org.cafienne.querydb.materializer.tenant.TenantReader
import org.cafienne.querydb.query.{TenantQueriesImpl, UserQueries}
import org.cafienne.service.akkahttp.Headers
import org.cafienne.tenant.actorapi.command.TenantCommand

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait TenantRoute extends CommandRoute with QueryRoute {
  val userQueries: UserQueries = new TenantQueriesImpl

  override val lastModifiedRegistration: LastModifiedRegistration = TenantReader.lastModifiedRegistration

  override val lastModifiedHeaderName: String = Headers.TENANT_LAST_MODIFIED

  def tenantUser(subRoute: TenantUser => Route): Route = {
    authenticatedUser { user =>
      pathPrefix(Segment) { group =>
        optionalHeaderValueByName(Headers.TENANT_LAST_MODIFIED) { lastModified =>
          onComplete(getTenantUser(user, group, lastModified)) {
            case Success(tenantUser) =>
              if (tenantUser.enabled) {
                subRoute(tenantUser)
              } else {
                complete(StatusCodes.Unauthorized, s"The user account ${tenantUser.id} has been disabled")
              }
            case Failure(t) => throw t
          }
        }
      }
    }
  }

  def getTenantUser(user: AuthenticatedUser, tenant: String, lastModified: Option[String]): Future[TenantUser] = {
    runSyncedQuery(userQueries.getTenantUser(user, tenant), lastModified)
  }

  def askTenant(command: TenantCommand): Route = {
    askModelActor(command)
  }
}
