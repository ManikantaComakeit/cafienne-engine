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

package org.cafienne.service.akkahttp.cases.model

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.infrastructure.akkahttp.EntityReader.{EntityReader, entityReader}

import scala.annotation.meta.field

object CaseMigrationAPI {
  implicit val migrationReader: EntityReader[MigrationDefinitionFormat] = entityReader[MigrationDefinitionFormat]

  @Schema(description = "Migrate definition of a case")
  case class MigrationDefinitionFormat(
                                        @(Schema@field)(description = "New definition of the case to be migrated", required = true, example = "Depending on the internally configured DefinitionProvider this can be a file name or the case model itself.", implementation = classOf[String])
                                        newDefinition: String = "", // by default an empty string to avoid nullpointers down the line
                                      )

}
