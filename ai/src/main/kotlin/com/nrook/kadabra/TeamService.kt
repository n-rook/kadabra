package com.nrook.kadabra

import com.nrook.kadabra.proto.TeamRequest
import com.nrook.kadabra.proto.TeamServiceGrpc
import com.nrook.kadabra.proto.TeamSpec
import com.nrook.kadabra.teambuilder.TeamPickingStrategy
import io.grpc.stub.StreamObserver

/**
 * Returns a randomly generated team.
 */
class TeamService(val selectors : Map<String, TeamPickingStrategy>): TeamServiceGrpc.TeamServiceImplBase() {

  override fun getTeam(request: TeamRequest, responseObserver: StreamObserver<TeamSpec>) {
    val selector = selectors[request.metagame]
    if (selector == null) {
      throw IllegalArgumentException(
          "Unknown metagame ${request.metagame}. We only know about: ${selectors.keys.joinToString()}")
    }
    val team = TeamSpec.newBuilder()
        .addAllPokemon(selector.pick().map { p -> p.toSpec() })
        .build()
    responseObserver.onNext(team)
    responseObserver.onCompleted()
  }
}
