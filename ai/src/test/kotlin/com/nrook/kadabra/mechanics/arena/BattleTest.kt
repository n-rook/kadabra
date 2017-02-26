package com.nrook.kadabra.mechanics.arena

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.AbilityId
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.info.testdata.*
import com.nrook.kadabra.mechanics.*
import org.junit.Before
import org.junit.Test
import java.util.*

class BattleTest {

  lateinit var charizardVsBlastoise: Battle

  @Before
  fun setUp() {
    val charizardSpec = PokemonSpec(
        CHARIZARD,
        AbilityId("Blaze"),
        Gender.FEMALE,
        Nature.ADAMANT,
        makeEvs(mapOf(Stat.ATTACK to 252, Stat.SPEED to 252, Stat.HP to 4)),
        MAX_IVS,
        Level(100),
        listOf(EARTHQUAKE)
    )
    val activeCharizard = newActivePokemonFromSpec(charizardSpec)

    val blastoiseSpec = PokemonSpec(
        BLASTOISE,
        AbilityId("Torrent"),
        Gender.FEMALE,
        Nature.MODEST,
        makeEvs(mapOf(Stat.SPECIAL_ATTACK to 252, Stat.SPEED to 252, Stat.HP to 4)),
        MAX_IVS,
        Level(100),
        listOf(SURF, TACKLE)
    )
    val activeBlastoise = newActivePokemonFromSpec(blastoiseSpec)


    val blackSide = Side(activeCharizard)
    val whiteSide = Side(activeBlastoise)

    charizardVsBlastoise = Battle(Random(), 1, blackSide, whiteSide, null, null, Phase.BEGIN, null)
  }

  @Test
  fun initialChoices() {
    assertThat(charizardVsBlastoise.choices(Player.BLACK)).containsExactly(MoveChoice(EARTHQUAKE))
    assertThat(charizardVsBlastoise.choices(Player.WHITE))
        .containsExactly(MoveChoice(SURF), MoveChoice(TACKLE))
  }

  @Test
  fun simulateFirstTurn() {
    val turn2 = charizardVsBlastoise.simulate(MoveChoice(EARTHQUAKE), MoveChoice(TACKLE))

    assertThat(turn2.turn).isEqualTo(2)
    assertThat(turn2.phase).isEqualTo(Phase.BEGIN)
    assertThat(turn2.blackChoice).isNull()
    assertThat(turn2.whiteChoice).isNull()

    // EQ deals between 90 and 106 damage to Blastoise, who has 300 HP
    assertThat(turn2.whiteSide.active.hp).isAtLeast(300 - 106)
    assertThat(turn2.whiteSide.active.hp).isAtMost(300 - 90)

    // Tackle does only 28 to 33  damage to Charizard
    assertThat(turn2.blackSide.active.hp).isAtLeast(298 - 33)
    assertThat(turn2.blackSide.active.hp).isAtMost(298 - 28)

    // With these moves, Charizard will win.
    assertThat(turn2.winner()).isNull()
    var simulation = turn2
    while (simulation.winner() == null) {
      simulation = simulation.simulate(MoveChoice(EARTHQUAKE), MoveChoice(TACKLE))
    }
    assertThat(simulation.winner()).isEqualTo(Player.BLACK)
  }
}
