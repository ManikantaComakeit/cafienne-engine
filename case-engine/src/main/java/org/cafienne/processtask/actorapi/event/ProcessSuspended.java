package org.cafienne.processtask.actorapi.event;

import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ProcessSuspended extends ProcessInstanceEvent {
    public ProcessSuspended(ProcessTaskActor actor) {
        super(actor);
    }

    public ProcessSuspended(ValueMap json) {
        super(json);
    }
}
