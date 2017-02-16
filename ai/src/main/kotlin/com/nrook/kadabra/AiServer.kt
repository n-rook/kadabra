package com.nrook.kadabra

import com.google.common.io.Resources
import com.google.gson.GsonBuilder
import com.nrook.kadabra.ai.Ai
import com.nrook.kadabra.info.PokemonDefinition
import com.nrook.kadabra.teambuilder.TeamPickingStrategy
import com.nrook.kadabra.teambuilder.UsageDatasetTeamPicker
import com.nrook.kadabra.teambuilder.loadTeamFromResource
import com.nrook.kadabra.usage.UsageDataset
import com.nrook.kadabra.usage.registerDeserializers
import io.grpc.Server
import io.grpc.ServerBuilder
import java.io.InputStreamReader
import java.util.*

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

private fun getUsageData(): UsageDataset {
  val gson = registerDeserializers(GsonBuilder())
      .create()
  val resource = Resources.getResource("gen7pokebankou-1695.json")

  return gson.fromJson(InputStreamReader(resource.openStream()), UsageDataset::class.java)
}

fun createAndStartAiServer(port: Int): AiServer {
  val teamSelector = UsageDatasetTeamPicker.create(Random(), getUsageData(), 0.005)

  val server = ServerBuilder.forPort(port)
      .addService(TeamService(mapOf("gen7pokebankou" to teamSelector)))
      .addService(BattleService(Ai()))
      .build()
  println("starting server on port " + port)
  server.start()
  return AiServer(server)
}
