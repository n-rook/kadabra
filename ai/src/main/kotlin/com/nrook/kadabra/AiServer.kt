package com.nrook.kadabra

import com.google.common.collect.ImmutableList
import com.google.common.io.Resources
import com.google.gson.GsonBuilder
import com.nrook.kadabra.ai.client.RandomClientAi
import com.nrook.kadabra.info.TeamPokemon
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.teambuilder.TeamLoader
import com.nrook.kadabra.teambuilder.TeamPickingStrategy
import com.nrook.kadabra.teambuilder.UsageDatasetTeamPicker
import com.nrook.kadabra.usage.UsageDataset
import com.nrook.kadabra.usage.registerDeserializers
import io.grpc.Server
import io.grpc.ServerBuilder
import java.io.InputStreamReader
import java.util.*

private class JustGetTestTeam(private val loader: TeamLoader) : TeamPickingStrategy {

  override fun pick(): List<TeamPokemon> {
    return loader.loadTeamFromResource("testTeam.txt")
  }

}

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
  val resource = Resources.getResource("gen7ou-1695-2017-03.json")

  return gson.fromJson(InputStreamReader(resource.openStream()), UsageDataset::class.java)
      .banPokemon(ImmutableList.of("Pheromosa", "Metagross-Mega", "Scolipede"))
}

fun createAndStartAiServer(port: Int): AiServer {
  val pokedex = getGen7Pokedex()
  val teamSelector = UsageDatasetTeamPicker.create(pokedex, Random(), getUsageData(), 0.005)

  val server = ServerBuilder.forPort(port)
      .addService(TeamService(mapOf("gen7ou" to teamSelector)))
      .addService(BattleService(RandomClientAi()))
      .build()
  println("starting server on port " + port)
  server.start()
  return AiServer(server)
}
