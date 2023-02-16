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

package org.cafienne.infrastructure.serialization.serializers;

import org.cafienne.board.actorapi.command.CreateBoard;
import org.cafienne.board.actorapi.command.definition.AddColumnDefinition;
import org.cafienne.board.actorapi.command.definition.UpdateBoardDefinition;
import org.cafienne.board.actorapi.command.definition.UpdateColumnDefinition;
import org.cafienne.board.actorapi.event.BoardCreated;
import org.cafienne.board.actorapi.event.BoardModified;
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionAdded;
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionUpdated;
import org.cafienne.board.actorapi.event.definition.BoardDefinitionUpdated;
import org.cafienne.board.actorapi.response.BoardCreatedResponse;
import org.cafienne.board.actorapi.response.BoardResponse;
import org.cafienne.board.actorapi.response.ColumnAddedResponse;
import org.cafienne.infrastructure.serialization.CafienneSerializer;

public class BoardSerializers {
    public static void register() {
        addBoardCommands();
        addBoardEvents();
        addBoardResponses();
    }

    private static void addBoardCommands() {
        CafienneSerializer.addManifestWrapper(CreateBoard.class, CreateBoard::new);
        CafienneSerializer.addManifestWrapper(UpdateBoardDefinition.class, UpdateBoardDefinition::deserialize);
        CafienneSerializer.addManifestWrapper(AddColumnDefinition.class, AddColumnDefinition::deserialize);
        CafienneSerializer.addManifestWrapper(UpdateColumnDefinition.class, UpdateColumnDefinition::deserialize);
    }

    private static void addBoardEvents() {
        CafienneSerializer.addManifestWrapper(BoardCreated.class, BoardCreated::new);
        CafienneSerializer.addManifestWrapper(BoardDefinitionUpdated.class, BoardDefinitionUpdated::new);
        CafienneSerializer.addManifestWrapper(ColumnDefinitionAdded.class, ColumnDefinitionAdded::new);
        CafienneSerializer.addManifestWrapper(ColumnDefinitionUpdated.class, ColumnDefinitionUpdated::new);
        CafienneSerializer.addManifestWrapper(BoardModified.class, BoardModified::new);
    }

    private static void addBoardResponses() {
        CafienneSerializer.addManifestWrapper(BoardResponse.class, BoardResponse::new);
        CafienneSerializer.addManifestWrapper(BoardCreatedResponse.class, BoardCreatedResponse::new);
        CafienneSerializer.addManifestWrapper(ColumnAddedResponse.class, ColumnAddedResponse::new);
    }
}
