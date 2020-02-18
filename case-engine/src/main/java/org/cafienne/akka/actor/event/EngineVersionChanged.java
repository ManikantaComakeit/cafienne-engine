package org.cafienne.akka.actor.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.CafienneVersion;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

@Manifest
public class EngineVersionChanged extends ModelEvent {

    private final CafienneVersion version;

    private enum Fields {
        version
    }

    public EngineVersionChanged(ModelActor actor, CafienneVersion version) {
        super(actor);
        this.version = version;
    }

    public EngineVersionChanged(ValueMap json) {
        super(json);
        this.version = new CafienneVersion(readMap(json, Fields.version));
    }

    @Override
    public void updateState(ModelActor actor) {
        actor.setEngineVersion(this.version);
    }

    /**
     * Returns the version of the engine that is currently applied in the case
     * @return
     */
    public CafienneVersion version() {
        return version;
    }

    @Override
    public String toString() {
        return "Engine version changed to " + version;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        super.writeField(generator, Fields.version, version.json());
    }
}
