package com.nrook.kadabra

import com.google.common.io.Resources
import com.nrook.kadabra.info.PokemonDefinition
import com.nrook.kadabra.teambuilder.TeamPickingStrategy
import com.nrook.kadabra.teambuilder.loadTeamFromResource
import io.grpc.Server
import io.grpc.ServerBuilder

private class JustGetTestTeam : TeamPickingStrategy {
  override fun pick(): List<PokemonDefinition> {
    return loadTeamFromResource(Resources.getResource("testTeam.txt"))
  }

}
val TEAM_SELECTOR : TeamPickingStrategy = JustGetTestTeam()

/**
 * Wrapper class for the AI server.
 */
class AiServer(val server: Server) {
  fun shutdown() {
    server.shutdown()
  }

  fun awaitTermination() {
    server.awaitTermination()
  }
}

fun createAndStartAiServer(port: Int): AiServer {
  val server = ServerBuilder.forPort(port)
      .addService(TeamService(mapOf("gen7ou" to TEAM_SELECTOR)))
      .addService(BattleService())
      .build()
  println("starting server on port " + port)
  server.start()
  return AiServer(server)
}
