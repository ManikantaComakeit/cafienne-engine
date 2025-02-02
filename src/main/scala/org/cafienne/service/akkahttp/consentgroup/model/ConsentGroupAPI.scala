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

package org.cafienne.service.akkahttp.consentgroup.model

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.consentgroup.actorapi.{ConsentGroup, ConsentGroupMember}
import org.cafienne.infrastructure.akkahttp.EntityReader.{EntityReader, entityReader}
import org.cafienne.service.akkahttp.ApiValidator
import org.cafienne.util.Guid

import scala.annotation.meta.field

object ConsentGroupAPI {

  implicit val consentGroupReader: EntityReader[ConsentGroupFormat] = entityReader[ConsentGroupFormat]
  implicit val consentGroupUserReader: EntityReader[ConsentGroupUserFormat] = entityReader[ConsentGroupUserFormat]

  case class ConsentGroupFormat(
                   @(Schema @field)(implementation = classOf[String], example = "Unique identifier of the group (optionally generated in the engine)")
                   id: Option[String],
                   members: Seq[ConsentGroupUserFormat]) {
    // Validate the list of members to not contain duplicates
    ApiValidator.runDuplicatesDetector("Consent group", "user", members.map(_.userId))
    ApiValidator.requireElements(members, "Setting consent group requires a list of users with at least one owner")

    def asGroup(tenant: String): ConsentGroup = {
      val groupId = id.fold(new Guid().toString)(id => id)
      ConsentGroup(groupId, tenant, members.map(_.asMember))
    }
  }

  case class ConsentGroupUserFormat(
                   @(Schema @field)(implementation = classOf[String], example = "User id of the consent group member")
                   userId: String,
                   @(Schema @field)(description = "Optional list of roles the user has within the consent group", example = "groupRole1, groupRole2")
                   roles: Set[String] = Set[String](),
                   @(Schema @field)(example = "Optional indicate of consent group ownership (defaults to false)", implementation = classOf[Boolean])
                   isOwner: Boolean = false) {
    ApiValidator.required(userId, "Consent group users must have a userId")
    def asMember: ConsentGroupMember = ConsentGroupMember(userId, roles = roles, isOwner = isOwner)
  }
}
