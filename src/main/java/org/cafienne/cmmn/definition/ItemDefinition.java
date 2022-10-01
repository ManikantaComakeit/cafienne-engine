/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItemType;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Interface to generalize across PlanItemDefinition and DiscretionaryItemDefinition (and CasePlanDefinition)
 */
public interface ItemDefinition extends DefinitionElement {
    ItemControlDefinition getPlanItemControl();

    PlanItemDefinitionDefinition getPlanItemDefinition();

    default Collection<EntryCriterionDefinition> getEntryCriteria() {
        return new ArrayList<>();
    }

    Collection<ExitCriterionDefinition> getExitCriteria();

    default boolean isDiscretionary() {
        return false;
    }

    default PlanItemType getItemType() {
        return getPlanItemDefinition().getItemType();
    }

    default PlanItemStarter getStarter() {
        return PlanItemStarter.Later(this);
    }

    /**
     * Indication whether exit criteria are defined on this item, or if not, on any of it's parent elements
     * @return
     */
    default boolean hasExits() {
        return getExitCriteria().size() > 0;
    }
}
