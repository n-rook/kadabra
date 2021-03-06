package com.nrook.kadabra.mechanics.arena

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.mechanics.rng.MoveDamagePolicy
import com.nrook.kadabra.mechanics.rng.RandomNumberGenerator
import com.nrook.kadabra.mechanics.rng.RandomPolicy
import com.nrook.kadabra.mechanics.testing.TestSpecBuilder
import org.junit.Before
import org.junit.Test
import java.util.*

class MoveTest {

  lateinit var pokedex: Pokedex
  lateinit var context: BattleContext

  @Before
  fun setUp() {
    pokedex = getGen7Pokedex()

    val rng = RandomNumberGenerator(RandomPolicy(MoveDamagePolicy.ONE), Random())
    context = BattleContext(rng, debugLogger())
  }

  @Test
  fun uTurnActsFirst() {
    val scizor = TestSpecBuilder.create(pokedex, "Scizor")
        .withMoves("uturn")
        .build()
    val tauros = TestSpecBuilder.create(pokedex, "Tauros")
        .withMoves("return")
        .build()
    val snorlax = TestSpecBuilder.create(pokedex, "Snorlax")
        .withMoves("facade")
        .build()

    val battle = startBattle(listOf(scizor, tauros), 0, listOf(snorlax), 0, context)
    val afterFirstUTurn = simulateBattle(
        battle,
        context,
        MoveChoice(pokedex.getMoveByName("U-turn")),
        MoveChoice(pokedex.getMoveByName("Facade")))

    assertThat(afterFirstUTurn.phase).isEqualTo(Phase.FIRST_MOVE_SWITCH)
    assertThat(afterFirstUTurn.blackSide.active.species.name).isEqualTo("Scizor")
    assertThat(afterFirstUTurn.choices(Player.BLACK))
        .containsExactly(SwitchChoice(tauros.species.id))
    assertThat(afterFirstUTurn.whiteSide.active.hp).isEqualTo(316)
    assertThat(afterFirstUTurn.choices(Player.WHITE)).isEmpty()

    val endOfTurn = simulateBattle(
        afterFirstUTurn, context, SwitchChoice(tauros.species.id), null)
    assertThat(endOfTurn.blackSide.active.species.name).isEqualTo("Tauros")
    assertThat(endOfTurn.blackSide.active.hp).isEqualTo(198)
  }

  @Test
  fun uTurnActsSecond() {
    val scizor = TestSpecBuilder.create(pokedex, "Scizor")
        .withMoves("uturn")
        .build()
    val tauros = TestSpecBuilder.create(pokedex, "Tauros")
        .withMoves("return")
        .build()
    val jolteon = TestSpecBuilder.create(pokedex, "Jolteon")
        .withMoves("thunderbolt")
        .build()

    val battle = startBattle(listOf(scizor, tauros), 0, listOf(jolteon), 0, context)
    val uTurnSwitch = simulateBattle(
        battle,
        context,
        MoveChoice(pokedex.getMoveByName("U-turn")),
        MoveChoice(pokedex.getMoveByName("Thunderbolt")))

    assertThat(uTurnSwitch.phase).isEqualTo(Phase.SECOND_MOVE_SWITCH)
    assertThat(uTurnSwitch.blackSide.active.species.name).isEqualTo("Scizor")
    assertThat(uTurnSwitch.choices(Player.BLACK))
        .containsExactly(SwitchChoice(tauros.species.id))
    assertThat(uTurnSwitch.blackSide.active.hp).isEqualTo(143)  // 138 damage
    assertThat(uTurnSwitch.whiteSide.active.hp).isEqualTo(117)  // 154 damage
    assertThat(uTurnSwitch.choices(Player.WHITE)).isEmpty()

    val endOfTurn = simulateBattle(
        uTurnSwitch, context, SwitchChoice(tauros.species.id), null)
    assertThat(endOfTurn.blackSide.active.species.name).isEqualTo("Tauros")
    assertThat(endOfTurn.blackSide.active.hp).isEqualTo(291)  // full
  }
}