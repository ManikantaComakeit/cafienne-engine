package org.cafienne.board.state

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.command.CreateBoard
import org.cafienne.board.actorapi.command.flow.StartFlow
import org.cafienne.board.actorapi.event.definition.BoardDefinitionEvent
import org.cafienne.board.actorapi.event.flow.{BoardFlowEvent, FlowInitiated}
import org.cafienne.board.actorapi.event.{BoardCreated, BoardEvent, BoardModified}
import org.cafienne.board.actorapi.response.FlowStartedResponse
import org.cafienne.board.state.definition.BoardDefinition

import scala.collection.mutable

class BoardState(val board: BoardActor) extends StateElement with LazyLogging {
  val definition = new BoardDefinition(board, board.getId)
  val flows = new mutable.HashMap[String, FlowState]()

  // Tell our flows we've completed recovery, so that they can start follow up actions when necessary
  def recoveryCompleted(): Unit = {
    definition.recoveryCompleted()
    flows.foreach(_._2.recoveryCompleted())
  }

  def initialize(command: CreateBoard): Unit = {
    board.addEvent(new BoardCreated(board, command.title))
    // Be aware: create team is asynchronous, as it creates a Consent Group underneath.
    //  The createTeam is responsible for informing the sender with BoardCreatedResponse.
    definition.team.createTeam(Some(command), command.getUser)
  }

  def startFlow(command: StartFlow): FlowStartedResponse = {
    board.addEvent(new FlowInitiated(board, command.flowId, command.subject, command.data))
    val flow = flows(command.flowId) // FlowInitiated event has updateState that adds the flow to our map of flows.
    // This code is reached when the constructor is called, and hence the FlowInitiated event was triggered.
    //  If in recovery mode, we should not do any further action;
    //  But if in running mode, then let's create the underlying case
    flow.createCase(Some(command))
    null
  }

  def updateState(event: BoardEvent): Unit = event match {
    case event: BoardDefinitionEvent => handleDefinitionUpdate(event)
    case event: FlowInitiated => flows.getOrElseUpdate(event.flowId, new FlowState(board, event))
    case event: BoardFlowEvent => flows.get(event.flowId).fold({
      logger.error(s"Could not find flow with id ${event.flowId} for event of type ${event.getClass.getName}. This means that FlowInitiated event no longer exists for this flow?!")
    })(_.updateState(event))
    case _: BoardModified => // BoardModified extends ActorModified which updates the state of the actor
    case other => logger.warn(s"Board Definition cannot handle event of type ${other.getClass.getName}")
  }

  def handleDefinitionUpdate(event: BoardDefinitionEvent): Unit = {
    definition.updateState(event)
    // And now, with the updated definition, we should iterate through all our case instances
    //  and update their case definitions ...
    //  Probably only when recovery is not running ...
    addDebugInfo(() => "Updated definition of board " + board.getId + " to:  " + definition.getCaseDefinition().getDefinitionsDocument.getSource)

    if (board.recoveryFinished && flows.nonEmpty) {
      System.out.println("Update case definitions of currently active flows")
      // Now take all flows and update their definitions ...
    }
  }
}
