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

package org.cafienne.storage.deletion.state

import akka.Done
import org.cafienne.storage.actormodel.CaseChildrenFinder
import org.cafienne.storage.deletion.ActorDataRemover
import org.cafienne.storage.querydb.CaseStorage

import scala.concurrent.Future

class CaseDeletionState(override val actor: ActorDataRemover) extends DeletionState with CaseChildrenFinder {
  override val dbStorage: CaseStorage = new CaseStorage

  override def clearQueryData(): Future[Done] = {
    dbStorage.deleteCase(metadata.actorId)
  }
}
