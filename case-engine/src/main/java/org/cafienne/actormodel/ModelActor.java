package org.cafienne.actormodel;

import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotMetadata;
import org.cafienne.actormodel.command.BootstrapCommand;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.EngineVersionChanged;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.actormodel.handler.*;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.CommandFailureListener;
import org.cafienne.actormodel.response.CommandResponseListener;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.instance.debug.DebugJsonAppender;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.cafienne.infrastructure.serialization.DeserializationFailure;
import org.cafienne.json.Value;
import org.cafienne.processtask.actorapi.command.ProcessCommand;
import org.cafienne.system.CaseSystem;
import org.cafienne.system.health.HealthMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class ModelActor extends AbstractPersistentActor {

    private final static Logger logger = LoggerFactory.getLogger(ModelActor.class);
    /**
     * The tenant in which this model is ran by the engine.
     */
    private String tenant;
    /**
     * The identifier of the model. Is expected to be unique. However, in practice it is derived from the Actor's path.
     */
    private final String id;
    /**
     * Reference to handler for the current message being processed by the ModelActor.
     */
    private MessageHandler currentMessageHandler;
    /**
     * User context of current message
     */
    private UserIdentity currentUser;
    /**
     * Flag indicating whether the model actor runs in debug mode or not
     */
    private boolean debugMode = Cafienne.config().actor().debugEnabled();

    /**
     * Registration of listeners that are interacting with (other) models through this case.
     */
    private final Map<String, Responder> responseListeners = new HashMap<>();

    /**
     * CaseScheduler is a lightweight manager to schedule asynchronous works for this Case instance.
     */
    private final CaseScheduler scheduler;
    /**
     * The moment of last modification of the case, i.e., the moment at which the last correctly handled command was completed
     */
    private Instant lastModified;
    /**
     * The moment the next transaction is started; will be used to fill the LastModified event and also inbetween timestamps in events.
     */
    private Instant transactionTimestamp;


    /**
     * The version of the engine that this case currently uses; this defaults to what comes from the BuildInfo.
     * If a ModelActor is recovered by Akka, then the version will be overwritten in {@link ModelActor#setEngineVersion(CafienneVersion)}.
     * Whenever then a new incoming message is handled by the Case actor - one leading to events, i.e., state changes, then
     * the actor will insert a new event EngineVersionChanged.
     * For new Cases, the CaseDefinitionApplied event will generate the current version
     */
    private CafienneVersion engineVersion;

    protected final CaseSystem caseSystem;

    protected ModelActor(CaseSystem caseSystem) {
        this.caseSystem = caseSystem;
        this.id = self().path().name();
        this.scheduler = new CaseScheduler(this);
    }

    abstract protected boolean supportsCommand(Object msg);

    abstract protected boolean supportsEvent(Object msg);

    public CafienneVersion getEngineVersion() {
        return this.engineVersion;
    }

    public void setEngineVersion(CafienneVersion version) {
        this.engineVersion = version;
    }

    /**
     * Returns the id of the parent of this model, i.e., the one that created this model
     * and maintains it's lifecycle. Should return null or an empty string if there is none.
     *
     * @return
     */
    public String getParentActorId() {
        return "";
    }

    /**
     * Returns the id of the parent of this model, i.e., the one that created this model
     * and maintains it's lifecycle. Should return null or an empty string if there is none.
     *
     * @return
     */
    public String getRootActorId() {
        return getId();
    }

    /**
     * Returns the Guid of the model instance
     *
     * @return
     */
    public String getId() {
        return id;
    }

    @Override
    public String persistenceId() {
        return this.id;
    }

    /**
     * Switch debug mode of the ModelActor.
     * When debug mode is enabled, log messages will be added to a DebugEvent that is persisted upon handling
     * an incoming message or recovery.
     *
     * @param debugMode
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Returns true if the model actor runs in debug mode, false otherwise.
     *
     * @return
     */
    public boolean debugMode() {
        return this.debugMode;
    }

    /**
     * Returns the user context of the current command, event or response
     *
     * @return
     */
    public UserIdentity getCurrentUser() {
        return currentUser;
    }

    public final void setCurrentUser(UserIdentity user) {
        this.currentUser = user;
    }

    /**
     * Returns the scheduler for handling async work (from ProcessTasks and TimerEvent executions).
     * This is just a simple wrapper/delegator for a 'real' scheduler, allowing more fine-grained control
     * over async work in case of restarting an actor due to an error.
     *
     * @return
     */
    public CaseScheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder().match(Object.class, this::handleRecovery).build();
    }

    protected void handleRecovery(Object event) {
        // Steps:
        // 1. Set tenant if not yet available
        // 2. Check whether this is a valid type of ModelEvent for this type of ModelActor
        //    a. If so, run the recovery handler for it
        //    b. Ignore DebugEvents and RecoveryCompleted message
        //    c. In all other cases print warn statements, with a special check for other ModelEvents

        // Step 1
        // Step 2
        if (supportsEvent(event) || event instanceof EngineVersionChanged) {
            if (tenant == null && event instanceof ModelEvent) {
                tenant = ((ModelEvent) event).getTenant();
            }
            // Step 2a.
            runHandler(createRecoveryHandler((ModelEvent) event));
        } else if (event instanceof DebugEvent) {
            // Step 2b.
            // No recovery from debug events ...
        } else if (event instanceof RecoveryCompleted) {
            // Step 2b.
            recoveryCompleted();
        } else if (event instanceof ModelEvent) {
            // Step 2c. Weird: ModelEvents in recovery of other models??
            getLogger().warn("Received unexpected recovery event of type " + event.getClass().getName() + " in actor of type " + getClass().getName());
        } else if (event instanceof DeserializationFailure) {
            // Step 2c. Weird: ModelEvents in recovery of other models??
            getLogger().error("Event Deserialization Failure: " + event);
        } else {
            // Step 2c.
            getLogger().warn("Received unknown event of type " + event.getClass().getName() + " during recovery: " + event);
        }
    }

    protected void recoveryCompleted() {
        getLogger().info("Recovery of " + getClass().getSimpleName() + " " + getId() + " completed");
    }

    @Override
    public final Receive createReceive() {
        return receiveBuilder().match(Object.class, msg -> {

//            System.out.println(this.getClass().getSimpleName() + ": Received a msg of type " + msg.getClass().getSimpleName());

            // Steps:
            // 1. Remove self cleaner
            // 2. Handle message
            // 3. Set a new self cleaner (basically resets the timer)
            clearSelfCleaner();
            runHandler(createMessageHandler(msg));
            enableSelfCleaner();
        }).build();
    }

    /**
     * SelfCleaner provides a mechanism to have the ModelActor remove itself from memory after a specific idle period.
     */
    private Cancellable selfCleaner = null;

    protected void clearSelfCleaner() {
        // Receiving message should reset the self-cleaning timer
        if (selfCleaner != null) selfCleaner.cancel();
    }

    protected void enableSelfCleaner() {
        // Now set the new selfCleaner
        long idlePeriod = Cafienne.config().actor().idlePeriod();
        FiniteDuration duration = Duration.create(idlePeriod, TimeUnit.MILLISECONDS);
        selfCleaner = getScheduler().schedule(duration, () -> {
            getLogger().debug("Removing actor " + getClass().getSimpleName() + " " + getId() + " from memory, as it has been idle for " + (idlePeriod / 1000) + " seconds");
//            System.out.println("Removing actor " + getClass().getSimpleName() + " " + getId() + " from memory, as it has been idle for " + (idlePeriod / 1000) + " seconds");
            self().tell(PoisonPill.getInstance(), self());
        });
    }

    private MessageHandler createMessageHandler(Object msg) {
        if (supportsCommand(msg)) {
            if (inNeedOfTenantInformation()) {
                if (msg instanceof BootstrapCommand) {
                    this.tenant = ((BootstrapCommand) msg).tenant();
                } else {
                    return new NotConfiguredHandler(this, msg);
                }
            }
            ModelCommand command = (ModelCommand) msg;
            command.setActor(this);
            CommandHandler c = createCommandHandler(command);
            return c;
        } else if (msg instanceof ModelResponse) {
            if (inNeedOfTenantInformation()) {
                // We cannot handle responses if we have not been properly initialized.
                return new NotConfiguredHandler(this, msg);
            }
            return createResponseHandler((ModelResponse) msg);
        } else if (msg.getClass().getPackage().equals(SnapshotMetadata.class.getPackage())) {
            return createAkkaSystemMessageHandler(msg);
        } else {
            return createInvalidMessageHandler(msg);
        }
    }

    protected boolean inNeedOfTenantInformation() {
        return tenant == null;
    }

    /**
     * Execute the lifecycle in handling the incoming message:
     * - run security checks
     * - if no issues from there, invoke process method
     * - and finally invoke complete method.
     *
     * @param handler
     */
    private void runHandler(MessageHandler handler) {
        this.currentMessageHandler = handler;
        // First process, then complete
        this.currentMessageHandler.process();
        this.currentMessageHandler.complete();
    }

    /**
     * Basic handler for commands received in this ModelActor.
     * Be careful in overriding it.
     *
     * @param command
     */
    protected CommandHandler createCommandHandler(ModelCommand command) {
        return new CommandHandler(this, command);
    }

    /**
     * Basic handler for response messages received from other ModelActors in this ModelActor.
     * Be careful in overriding it.
     *
     * @param response
     */
    protected ResponseHandler createResponseHandler(ModelResponse response) {
        return new ResponseHandler(this, response);
    }

    /**
     * Basic handler for wrongly typed messages received in this ModelActor.
     * Be careful in overriding it.
     *
     * @param message
     */
    protected InvalidMessageHandler createInvalidMessageHandler(Object message) {
        return new InvalidMessageHandler(this, message);
    }

    /**
     * Handler for akka system messages (e.g. SnapshotOffer, SnapshotSaveSuccess, RecoveryCompleted, etc)
     *
     * @param message
     * @return
     */
    protected AkkaSystemMessageHandler createAkkaSystemMessageHandler(Object message) {
        return new AkkaSystemMessageHandler(this, message);
    }

    /**
     * Basic handler of events upon recovery of this ModelActor.
     * Be careful in overriding it.
     *
     * @param event
     */
    protected RecoveryEventHandler createRecoveryHandler(ModelEvent event) {
        return new RecoveryEventHandler(this, event);
    }

    /**
     * Adds an event to the current message handler
     *
     * @param event
     * @param <EV>
     * @return
     */
    public <EV extends ModelEvent> EV addEvent(EV event) {
        return currentMessageHandler.addEvent(event);
    }

    public Responder getResponseListener(String msgId) {
        synchronized (responseListeners) {
            return responseListeners.remove(msgId);
        }
    }

    /**
     * askCase allows inter-case communication. One case (or, typically, a plan item's special logic) can ask another case to execute
     * a command, and when the response is received back from the other case, the handler is invoked with that response.
     * Note that nothing will be sent to the other actor when recovery is running.
     *
     * @param command
     * @param left    Listener to handle response failures.
     * @param right   Optional listener to handle response success.
     */
    public void askCase(CaseCommand command, CommandFailureListener left, CommandResponseListener... right) {
        askModel(command, left, right);
    }

    /**
     * Similar to {@link #askCase(CaseCommand, CommandFailureListener, CommandResponseListener...)}
     *
     * @param command
     * @param left
     * @param right
     */
    public void askProcess(ProcessCommand command, CommandFailureListener left, CommandResponseListener... right) {
        askModel(command, left, right);
    }

    public void askModel(ModelCommand command, CommandFailureListener left, CommandResponseListener... right) {
        if (recoveryRunning()) {
//            System.out.println("Ignoring request to send command of type " + command.getClass().getName()+" because recovery is running");
            return;
        }
        synchronized (responseListeners) {
            responseListeners.put(command.getMessageId(), new Responder(left, right));
        }
        caseSystem.router().tell(command, self());
    }

    /**
     * Returns the tenant in which the model is running.
     *
     * @return
     */
    public final String getTenant() {
        return tenant;
    }

    /**
     * Method for a MessageHandler to persist it's events
     *
     * @param events
     * @param <T>
     */
    public <T> void persistEvents(List<T> events) {
        persistEventsAndThenReply(events, null);
    }

    /**
     * Model actor can send a reply to a command with this method
     *
     * @param response
     */
    public void reply(ModelResponse response) {
        // Always reset the transaction timestamp before replying. Even if there is no reply.
        resetTransactionTimestamp();
        if (response == null) {
            // Double check there is a response.
            return;
        }

        if (getLogger().isDebugEnabled() || EngineDeveloperConsole.enabled()) {
            String msg = "Sending response of type " + response.getClass().getSimpleName() + " from " + this;
            getLogger().debug(msg);
            EngineDeveloperConsole.debugIndentedConsoleLogging(msg);
        }
        response.setLastModified(getLastModified());
        response.getRecipient().tell(response, self());
    }

    public <T> void replyAndThenPersistEvents(List<T> events, ModelResponse response) {
        reply(response);
        persistEvents(events);
    }

    /**
     * Method for a MessageHandler to persist it's events, and after that send the (optional) reply.
     * If there are no events, the reply will be sent immediately.
     *
     * @param events
     * @param response
     * @param <T>
     */
    public <T> void persistEventsAndThenReply(List<T> events, ModelResponse response) {
        if (getLogger().isDebugEnabled() || EngineDeveloperConsole.enabled()) {
            StringBuilder msg = new StringBuilder("\n------------------------ PERSISTING " + events.size() + " EVENTS IN " + this);
            events.forEach(e -> msg.append("\n\t" + e));
            getLogger().debug(msg + "\n");
            EngineDeveloperConsole.debugIndentedConsoleLogging(msg + "\n");
        }
        resetTransactionTimestamp();
        if (events.isEmpty()) {
            reply(response);
            return;
        } else {
            T lastEvent = events.get(events.size() - 1);
            persistAll(events, e -> {
                HealthMonitor.writeJournal().isOK();
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug(this.getDescription() + " - persisted event [" + lastSequenceNr() + "] of type " + e.getClass().getName());
                }
                if (e == lastEvent) {
                    reply(response);
                }
            });
        }
    }

    /**
     * If the command handler has changed ModelActor state, but then ran into an unhandled exception,
     * the actor will remove itself from memory and start again.
     *
     * @param handler
     * @param exception
     */
    public void failedWithInvalidState(MessageHandler handler, Throwable exception) {
        this.getScheduler().clearSchedules(); // Remove all schedules.
        if (exception instanceof CommandException) {
            getLogger().error("Restarting " + this + ". Handling msg of type " + handler.msg.getClass().getName() + " resulted in invalid state.");
            getLogger().error("  Cause: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
        } else {
            getLogger().error("Encountered failure in handling msg of type " + handler.msg.getClass().getName() + "; restarting " + this, exception);
        }
        this.supervisorStrategy().restartChild(self(), exception, true);
    }

    private void handlePersistFailure(Throwable cause, Object event, long seqNr) {
        // This code is invoked when there is a problem in connecting to the database while persisting events.
        //  Can also happen when a serialization of an event to JSON fails. In that case, recovery of the case seems not to work,
        //  whereas if we break e.g. Cassandra connection, it properly recovers after having invoked context().stop(self()).
        //  Not sure right now what the reason is for this.
        HealthMonitor.writeJournal().hasFailed(cause);
        getLogger().error("Failure in " + getClass().getSimpleName() + " " + getId() + " during persistence of event " + seqNr + " of type " + event.getClass().getName() + ". Stopping instance.", cause);
        if (currentMessageHandler instanceof CommandHandler) {
            ModelCommand command = ((CommandHandler) currentMessageHandler).getCommand();
            reply(new CommandFailure(command, new Exception("Handling the request resulted in a system failure. Check the server logs for more information.")));
        }
        context().stop(self());
    }

    @Override
    public void onPersistFailure(Throwable cause, Object event, long seqNr) {
        handlePersistFailure(cause, event, seqNr);
    }

    @Override
    public void onPersistRejected(Throwable cause, Object event, long seqNr) {
        handlePersistFailure(cause, event, seqNr);
    }

    /**
     * Add debug info to the case if debug is enabled.
     * If the case runs in debug mode (or if Log4J has debug enabled for this logger),
     * then the appender's debugInfo method will be invoked to store a string in the log.
     *
     * @param appender
     */
    public void addDebugInfo(DebugStringAppender appender) {
        currentMessageHandler.addDebugInfo(appender, getLogger());
    }

    public void addDebugInfo(DebugStringAppender appender, Value<?> json) {
        currentMessageHandler.addDebugInfo(appender, json, getLogger());
    }

    public void addDebugInfo(DebugJsonAppender appender) {
        currentMessageHandler.addDebugInfo(appender, getLogger());
    }

    public void addDebugInfo(DebugStringAppender appender, Exception exception) {
        currentMessageHandler.addDebugInfo(appender, exception, getLogger());
    }

    /**
     * Returns the moment at which the last modification to the case was done. I.e., the moment at which a command was completed that resulted into
     * events needing to be persisted.
     *
     * @return
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Returns the moment at which the last modification to the case was done. I.e., the moment at which a command was completed that resulted into
     * events needing to be persisted.
     *
     * @return
     */
    public Instant getTransactionTimestamp() {
        if (transactionTimestamp == null) {
            transactionTimestamp = Instant.now();
        }
        return transactionTimestamp;
    }

    public void resetTransactionTimestamp() {
        transactionTimestamp = null;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns the logger of the model actor
     *
     * @return
     */
    protected Logger getLogger() {
        return logger;
    }

    public String getDescription() {
        return this.getClass().getSimpleName() + "[" + getId() + "]";
    }

    @Override
    public String toString() {
        return this.getDescription();
    }
}
