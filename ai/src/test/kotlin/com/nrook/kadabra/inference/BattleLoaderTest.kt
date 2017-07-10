package com.nrook.kadabra.inference

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.inference.testing.loadEventsFromResource
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.mechanics.arena.Player
import com.nrook.kadabra.teambuilder.TeamLoader
import org.junit.Before
import org.junit.Test

class BattleLoaderTest {

  lateinit var pokedex: Pokedex
  lateinit var teamLoader: TeamLoader

  @Before
  fun setUp() {
    pokedex = getGen7Pokedex()
    teamLoader = TeamLoader(pokedex)
  }

  @Test
  fun parseTeamPreviewBattle() {
    val teamBuilderEvents = loadEventsFromResource("teambuilder1.log")
    val teamDefinitions = teamLoader.loadTeamFromResource("teambuilder1_p2_team.txt")
    val teamSpecs = teamDefinitions.map { it.toSpec() }

    val teamBuilderInfo = BattleLoader(pokedex).parseTeamPreviewBattle(teamSpecs, teamBuilderEvents)

    assertThat(teamBuilderInfo.us).isEqualTo(Player.WHITE)

    assertThat(teamBuilderInfo.ourBench).hasSize(6)
    assertThat(teamBuilderInfo.theirBench).hasSize(6)

    val firstMemberOfOurTeam = teamBuilderInfo.ourBench[0]
    assertThat(firstMemberOfOurTeam.species.name).isEqualTo("Zapdos")

    val arcanine = teamBuilderInfo.ourBench.find { it.species.name == "Arcanine" }!!
    assertThat(arcanine.nickname.nickname).isEqualTo("Dog")
    assertThat(arcanine.originalSpec.gender).isEqualTo(Gender.FEMALE)

    val firstMemberOfTheirTeam = teamBuilderInfo.theirBench[0]
    assertThat(firstMemberOfTheirTeam.species.name).isEqualTo("Alakazam")
    assertThat(firstMemberOfTheirTeam.gender).isEqualTo(Gender.FEMALE)
  }
}