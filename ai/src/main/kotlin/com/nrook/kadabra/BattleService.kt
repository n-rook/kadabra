package com.nrook.kadabra

import com.nrook.kadabra.ai.Ai
import com.nrook.kadabra.proto.*
import io.grpc.stub.StreamObserver

/**
 * Controls a Pokemon battle.
 */
class BattleService(val ai: Ai): BattleServiceGrpc.BattleServiceImplBase() {

  override fun chooseLead(request: LeadRequest, responseObserver: StreamObserver<LeadChoice>) {
    handleResponse({ai.pickLead()}, responseObserver)
  }

  override fun selectAction(request: ActionRequest, responseObserver: StreamObserver<ActionResponse>) {
    handleResponse({ai.pickStartOfTurnAction(request)}, responseObserver)
  }

  override fun selectSwitchAfterFaint(request: SwitchAfterFaintRequest, responseObserver: StreamObserver<SwitchAfterFaintResponse>) {
    handleResponse({ai.pickSwitchAfterFaintAction(request)}, responseObserver)
  }
}
