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

package org.cafienne.actormodel.command;

import akka.actor.ActorPath;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.JSONParseFailure;
import org.cafienne.json.JSONReader;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.util.Guid;

import java.io.IOException;
import java.io.StringWriter;

public abstract class BaseModelCommand<T extends ModelActor, U extends UserIdentity> implements ModelCommand {
    protected final String msgId;
    public final String actorId;
    protected transient T actor;

    /**
     * Store the user that issued the Command.
     */
    private final U user;

    protected BaseModelCommand(U user, String actorId) {
        // First, validate actor id to be akka compliant
        if (actorId == null) {
            throw new InvalidCommandException("Actor id cannot be null");
        }
        try {
            ActorPath.validatePathElement(actorId);
        } catch (Throwable t) {
            throw new InvalidCommandException("Invalid actor path " + actorId, t);
        }
        if (user == null || user.id() == null || user.id().trim().isEmpty()) {
            throw new InvalidCommandException("Tenant user cannot be null");
        }
        this.msgId = new Guid().toString();
        this.user = user;
        this.actorId = actorId;
    }

    protected BaseModelCommand(ValueMap json) {
        this.msgId = json.readString(Fields.messageId);
        this.actorId = json.readString(Fields.actorId);
        this.user = readUser(json.with(Fields.user));
    }

    /**
     * Model actor specific command to is responsible for deserializing user to appropriate type.
     * @param json
     * @return
     */
    protected abstract U readUser(ValueMap json);

    /**
     * Through this method, the command is made aware of the actor that is handling it.
     * @param actor
     */
    @Override
    public final void setActor(ModelActor actor) {
        this.actor = (T) actor;
    }

    @Override
    public final void validateCommand(ModelActor actor) {
        validate((T) actor);
    }

    @Override
    public final ModelResponse processCommand(ModelActor actor) {
        return process((T) actor);
    }

    /**
     * Note: this method will only return a sensible value when it is invoked from within the command handling context.
     * It is intended for command handlers to have more metadata when creating a ModelResponse.
     * @return
     */
    public T getActor() {
        return actor;
    }

    /**
     * Returns the user context for this command.
     *
     * @return
     */
    @Override
    public final U getUser() {
        return user;
    }

    /**
     * Returns a string with the identifier of the actor towards this command must be sent.
     * @return
     */
    @Override
    public final String getActorId() {
        return actorId;
    }

    /**
     * Returns the correlation id of this command, that can be used to relate a {@link CaseResponse} back to this
     * original command.
     *
     * @return
     */
    public String getMessageId() {
        return msgId;
    }

    /**
     * Before the Model Actor starts processing the command, it will first ask to validate the command.
     * Implementations may override this method to implement their own validation logic.
     * Implementations may throw the {@link InvalidCommandException} if they encounter a validation error
     *
     * @param modelActor
     * @throws InvalidCommandException If the command is invalid
     */
    public abstract void validate(T modelActor) throws InvalidCommandException;

    /**
     * Method to be implemented to handle the command.
     * @param modelActor
     * @return
     */
    public abstract ModelResponse process(T modelActor);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeModelCommand(generator);
    }

    protected void writeModelCommand(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.messageId, this.getMessageId());
        writeField(generator, Fields.actorId, this.getActorId());
        writeField(generator, Fields.user, user);
    }

    @Override
    public String getCommandDescription() {
        return getClass().getSimpleName();
    }

    public String toString() {
        return "Command [" + getCommandDescription() + "]" + super.toString();
    }

    public ValueMap rawJson() {
        JsonFactory factory = new JsonFactory();
        StringWriter sw = new StringWriter();
        try {
            JsonGenerator generator = factory.createGenerator(sw);
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            writeThisObject(generator);
            generator.close();
            Value<?> json = JSONReader.parse(sw.toString());
            return new ValueMap(Fields.type, CafienneSerializer.getManifestString(this), Fields.content, json);
        } catch (IOException | JSONParseFailure e) {
            return new ValueMap("message", "Could not make JSON out of command "+getClass().getName(), "exception", Value.convertThrowable(e));
        }
    }
}
