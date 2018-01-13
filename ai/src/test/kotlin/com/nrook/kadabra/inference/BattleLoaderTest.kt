package com.nrook.kadabra.inference

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.inference.testing.EventFileBank
import com.nrook.kadabra.inference.testing.snipToTurn
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.mechanics.Condition
import com.nrook.kadabra.mechanics.arena.Player
import com.nrook.kadabra.teambuilder.TeamLoader
import org.junit.Before
import org.junit.Test

class BattleLoaderTest {

  lateinit var pokedex: Pokedex
  lateinit var battleLoader: BattleLoader
  lateinit var teamLoader: TeamLoader
  lateinit var eventBank: EventFileBank

  @Before
  fun setUp() {
    pokedex = getGen7Pokedex()
    battleLoader = BattleLoader(pokedex)
    teamLoader = TeamLoader(pokedex)
    eventBank = EventFileBank(pokedex)
  }

  @Test
  fun parseTeamPreviewBattle() {
    val teamSpecs = eventBank.TEAM_PREVIEW.black.map { it.toSpec() }
    val teamBuilderInfo = battleLoader.parseTeamPreviewBattle(
        teamSpecs, eventBank.TEAM_PREVIEW.events)

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

  @Test
  fun damageUpdatesWorkOnOurSide() {
    val teamSpecs = eventBank.SAMPLE.black.map { it.toSpec() }
    val info = battleLoader.parseBattle(teamSpecs, eventBank.SAMPLE.events)

    assertThat(info.us).isEqualTo(Player.WHITE)
    assertThat(info.ourSide.active).isNotNull()

    val ourActivePokemon = info.ourSide.active!!
    assertThat(ourActivePokemon.species.name).isEqualTo("Nihilego")
    assertThat(ourActivePokemon.hp).isEqualTo(222)
    assertThat(ourActivePokemon.condition).isEqualTo(Condition.PARALYSIS)

    assertThat(info.ourSide.bench).hasSize(4)
  }

  @Test
  fun damageUpdatesWorkOnTheirSide() {
    val events = snipToTurn(eventBank.SAMPLE.events, 6)
    val teamSpecs = eventBank.SAMPLE.black.map { it.toSpec() }

    val info = battleLoader.parseBattle(teamSpecs, events)

    assertThat(info.us).isEqualTo(Player.WHITE)
    assertThat(info.theirSide.active).isNotNull()

    val theirActivePokemon = info.theirSide.active!!
    assertThat(theirActivePokemon.species.name).isEqualTo("Raikou")
    assertThat(theirActivePokemon.hp).isEqualTo(HpFraction(20, 100))
    assertThat(theirActivePokemon.condition).isEqualTo(Condition.OK)

    assertThat(info.theirSide.bench).hasSize(4)
  }
}