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

package org.cafienne.board;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.board.actorapi.command.BoardCommand;
import org.cafienne.board.actorapi.event.BoardCreated;
import org.cafienne.board.actorapi.event.BoardEvent;
import org.cafienne.board.actorapi.event.BoardModified;
import org.cafienne.board.actorapi.event.definition.BoardDefinitionEvent;
import org.cafienne.board.definition.BoardDefinition;
import org.cafienne.system.CaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class BoardActor extends ModelActor {
    private final static Logger logger = LoggerFactory.getLogger(BoardActor.class);

    private BoardDefinition definition = new BoardDefinition(this.getId());

    public BoardActor(CaseSystem caseSystem) {
        super(caseSystem);
    }

    @Override
    protected boolean supportsCommand(Object msg) {
        return msg instanceof BoardCommand;
    }

    @Override
    protected boolean supportsEvent(ModelEvent msg) {
        return msg instanceof BoardEvent;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public BoardDefinition getDefinition() {
        return definition;
    }

    public void updateState(BoardCreated boardCreated) {
        definition.updateState(boardCreated);
        this.setEngineVersion(boardCreated.engineVersion);
    }

    public void updateState(BoardDefinitionEvent event) {
        definition.updateState(event);
        // And now, with the updated definition, we should iterate through all our case instances
        //  and update their case definitions ...
        //  Probably only when recovery is not running ...
        addDebugInfo(() -> "Updated definition of board "+getId()+" to:  " + definition.getCaseDefinition().getDefinitionsDocument().getSource());

        if (recoveryFinished()) {
            System.out.println("Update case definitions of currently active flows");
        }
    }

    public void updateState(BoardModified event) {
        setLastModified(event.lastModified());
    }

    @Override
    protected void completeTransaction(IncomingActorMessage source) {
        addEvent(new BoardModified(this, source));
    }
}
