package com.nrook.kadabra

import com.nrook.kadabra.ai.client.ClientAi
import com.nrook.kadabra.proto.ActionRequest
import com.nrook.kadabra.proto.ActionResponse
import com.nrook.kadabra.proto.BattleServiceGrpc
import com.nrook.kadabra.proto.LeadChoice
import com.nrook.kadabra.proto.LeadRequest
import io.grpc.stub.StreamObserver

/**
 * Controls a Pokemon battle.
 */
class BattleService(val ai: ClientAi): BattleServiceGrpc.BattleServiceImplBase() {

  override fun chooseLead(request: LeadRequest, responseObserver: StreamObserver<LeadChoice>) {
    handleResponse({ai.pickLead()}, responseObserver)
  }

  override fun selectAction(request: ActionRequest, responseObserver: StreamObserver<ActionResponse>) {
    handleResponse({ai.pickAction(request)}, responseObserver)
  }
}
