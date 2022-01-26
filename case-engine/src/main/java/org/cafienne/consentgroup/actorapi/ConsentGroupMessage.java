/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.consentgroup.actorapi;

import org.cafienne.actormodel.message.UserMessage;
import org.cafienne.consentgroup.ConsentGroupActor;

/**
 * ConsentGroupMessages are generated by the {@link ConsentGroupActor}.
 */
public interface ConsentGroupMessage extends UserMessage {
    @Override
    default Class<ConsentGroupActor> actorClass() {
        return ConsentGroupActor.class;
    }
}
