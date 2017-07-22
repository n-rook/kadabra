package com.nrook.kadabra

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.info.AbilityId
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.TeamPokemon
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.MAX_IVS
import com.nrook.kadabra.mechanics.NO_EVS
import com.nrook.kadabra.mechanics.Nature
import com.nrook.kadabra.proto.TeamRequest
import com.nrook.kadabra.proto.TeamServiceGrpc
import com.nrook.kadabra.teambuilder.TeamPickingStrategy
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TeamServiceTest {

  var inProcessChannel : ManagedChannel? = null
  var server : Server? = null

  lateinit var pokedex: Pokedex

  private class MagikarpPicker(private val pokedex: Pokedex): TeamPickingStrategy {
    override fun pick(): List<TeamPokemon> {
      val magikarp = TeamPokemon(
          pokedex.getSpeciesByName("Magikarp"),
          "Leftovers",
          AbilityId("Swift Swim"),
          null,
          Nature.ADAMANT,
          NO_EVS,
          MAX_IVS,
          Level(100),
          ImmutableList.of(pokedex.getMoveByName("Splash")))
      return ImmutableList.of(magikarp)
    }
  }

  @Before
  fun setUp() {
    pokedex = getGen7Pokedex()

    val serverName : String = "Test server " + javaClass.canonicalName
    server = InProcessServerBuilder.forName(serverName)
        .addService(TeamService(mapOf("magikarp" to MagikarpPicker(pokedex))))
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