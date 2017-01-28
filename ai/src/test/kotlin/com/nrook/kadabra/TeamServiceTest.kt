package com.nrook.kadabra

import com.nrook.kadabra.info.PokemonDefinition
import com.nrook.kadabra.proto.PokemonSpec
import com.nrook.kadabra.proto.TeamRequest
import com.nrook.kadabra.proto.TeamServiceGrpc
import com.nrook.kadabra.teambuilder.TeamPickingStrategy
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class TeamServiceTest {
//  var server : Server? = null

  var inProcessChannel : ManagedChannel? = null
  var server : Server? = null

  private class MagikarpPicker: TeamPickingStrategy {
    override fun pick(): List<PokemonDefinition> {
      return listOf(PokemonDefinition(PokemonSpec.newBuilder()
          .setSpecies("Magikarp")
          .build()))
    }
  }

  @Before
  fun setUp() {
    val serverName : String = "Test server " + javaClass.canonicalName
    server = InProcessServerBuilder.forName(serverName)
        .addService(TeamService(mapOf("magikarp" to MagikarpPicker())))
        .directExecutor()
        .build()
    server?.start()
    inProcessChannel = InProcessChannelBuilder.forName(serverName).build()
  }

  @After
  fun tearDown() {
    inProcessChannel?.shutdownNow()
    server?.shutdown()
  }

  @Test
  fun magikarp() {
    val stub = TeamServiceGrpc.newBlockingStub(inProcessChannel)
    val team = stub.getTeam(TeamRequest.newBuilder().setMetagame("magikarp").build())
    assertEquals(1, team.pokemonCount)
    assertEquals("Magikarp", team.pokemonList[0].species)
  }

}