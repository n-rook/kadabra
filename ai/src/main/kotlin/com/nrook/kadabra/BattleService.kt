package com.nrook.kadabra

import com.nrook.kadabra.proto.BattleServiceGrpc
import com.nrook.kadabra.proto.LeadChoice
import com.nrook.kadabra.proto.LeadRequest
import io.grpc.stub.StreamObserver
import java.util.Random

private val random: Random = Random()

/**
 * Controls a Pokemon battle.
 */
class BattleService: BattleServiceGrpc.BattleServiceImplBase() {

  override fun chooseLead(request: LeadRequest, responseObserver: StreamObserver<LeadChoice>) {
    val leadResponse = LeadChoice.newBuilder()
        .setLeadIndex(random.nextInt(6) + 1)
        .build()
    responseObserver.onNext(leadResponse)
    responseObserver.onCompleted()
  }
}
