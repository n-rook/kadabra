package com.nrook.kadabra

import io.grpc.stub.StreamObserver

/**
 * Returns a randomly generated team.
 */
class TeamService: TeamServiceGrpc.TeamServiceImplBase() {

  override fun getTeam(request: TeamRequest, responseObserver: StreamObserver<TeamSpec>) {
    when (request.metagame) {
      "magikarp" -> {
        responseObserver.onNext(TeamSpec.newBuilder()
            .addPokemon(PokemonSpec.newBuilder().setSpecies("Magikarp"))
            .build()
        )
      }
      else -> {
        throw IllegalArgumentException("That's illegal!")
      }
    }
    responseObserver.onCompleted()
  }
}
