package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.inference.testing.EventFile
import com.nrook.kadabra.inference.testing.EventFileBank
import com.nrook.kadabra.inference.testing.EventFileWithBlackTeam
import com.nrook.kadabra.inference.testing.EventFileWithWhiteTeam
import com.nrook.kadabra.inference.testing.snipToTurn
import com.nrook.kadabra.inference.testing.snipUntilChoice
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.TeamPokemon
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

  private fun parseBattle(e: EventFile): OngoingBattle {
    val team: ImmutableList<TeamPokemon>
    when (e) {
      is EventFileWithWhiteTeam -> {
        team = e.white
      }
      is EventFileWithBlackTeam -> {
        team = e.black
      }
      else -> throw IllegalArgumentException("Can only parse battle with one team!")
    }
    return battleLoader.parseBattle(team.map { it.toSpec() }, e.events)
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
  fun turnUpdates() {
    val teamSpecs = eventBank.SAMPLE.black.map { it.toSpec() }
    val info = battleLoader.parseBattle(teamSpecs, eventBank.SAMPLE.events)
    assertThat(info.turn).isEqualTo(7)
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

  @Test
  fun beginningOfTurnPhaseDetected() {
    val info = parseBattle(eventBank.SAMPLE)
    assertThat(info.phase).isEqualTo(DecisionPhase.BEGIN)
  }

  @Test
  fun endOfTurnPhaseDetected() {
    val info = battleLoader.parseBattle(
        eventBank.SAMPLE.black.map { it.toSpec() },
        snipUntilChoice(eventBank.SAMPLE.events, "17"))
    assertThat(info.phase).isEqualTo(DecisionPhase.END)
  }

  @Test
  fun uTurnFirst() {
    val info = battleLoader.parseBattle(
        eventBank.U_TURN_FIRST.white.map { it.toSpec() },
        eventBank.U_TURN_FIRST.events)
    assertThat(info.us).isEqualTo(Player.WHITE)
    assertThat(info.turn).isEqualTo(1)
    assertThat(info.phase).isEqualTo(DecisionPhase.FIRST_MOVE_SWITCH)

    assertThat(info.theirSide.active).isNotNull()
    val theirActivePokemon = info.theirSide.active!!
    assertThat(theirActivePokemon.species.name).isEqualTo("Snorlax")
    assertThat(theirActivePokemon.hp).isEqualTo(HpFraction(68, 100))
  }

  @Test
  fun uTurnSecond() {
    val info = battleLoader.parseBattle(
        eventBank.U_TURN_SECOND_IMMEDIATELY_AFTER_U_TURN_HITS.white.map { it.toSpec() },
        eventBank.U_TURN_SECOND_IMMEDIATELY_AFTER_U_TURN_HITS.events)
    assertThat(info.us).isEqualTo(Player.WHITE)
    assertThat(info.turn).isEqualTo(1)
    assertThat(info.phase).isEqualTo(DecisionPhase.SECOND_MOVE_SWITCH)

    assertThat(info.theirSide.active).isNotNull()
    val theirActivePokemon = info.theirSide.active!!
    assertThat(theirActivePokemon.species.name).isEqualTo("Jolteon")
    assertThat(theirActivePokemon.hp).isEqualTo(HpFraction(42, 100))

    assertThat(info.ourSide.active).isNotNull()
    val ourActivePokemon = info.ourSide.active!!
    assertThat(ourActivePokemon.species.name).isEqualTo("Scizor")
    assertThat(ourActivePokemon.hp).isEqualTo(252)
  }
}
