/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.timerservice;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.serialization.CafienneSerializable;
import org.cafienne.actormodel.serialization.Fields;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.actormodel.serialization.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

/**
 * TimerServiceEvents are generated by the {@link TimerService}.
 */
@Manifest
public class TimerJob implements CafienneSerializable {
    public final String caseInstanceId;
    public final Instant moment;
    public final String timerId;
    public final TenantUser user;

    public TenantUser getUser() {
        return user;
    }

    public TimerJob(ValueMap json) {
        this.caseInstanceId = readField(json, Fields.caseInstanceId);
        this.moment = readInstant(json, Fields.moment);
        this.timerId = readField(json, Fields.timerId);
        this.user = TenantUser.from(json.with(Fields.user));
    }

    @Override
    public String toString() {
        return "Timer[" + timerId + "] in case ["+caseInstanceId+"] on behalf of user " + user.id();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.timerId, timerId);
        writeField(generator, Fields.caseInstanceId, caseInstanceId);
        writeField(generator, Fields.moment, moment);
        generator.writeFieldName(Fields.user.toString());
        user.write(generator);
    }
}
