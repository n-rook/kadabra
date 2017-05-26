package com.nrook.kadabra.mechanics.arena

import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.AbilityId
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.info.testdata.*
import com.nrook.kadabra.mechanics.*
import com.nrook.kadabra.mechanics.rng.REALISTIC_RANDOM_POLICY
import com.nrook.kadabra.mechanics.rng.RandomNumberGenerator
import org.junit.Before
import org.junit.Test
import java.util.*

class BattleTest {

  lateinit var charizardSpec: PokemonSpec
  lateinit var blastoiseSpec: PokemonSpec
  lateinit var magcargoSpec: PokemonSpec
  lateinit var charizardVsBlastoise: Battle
  lateinit var context: BattleContext

  @Before
  fun setUp() {
    charizardSpec = PokemonSpec(
        CHARIZARD,
        AbilityId("Blaze"),
        Gender.FEMALE,
        Nature.ADAMANT,
        makeEvs(mapOf(Stat.ATTACK to 252, Stat.SPEED to 252, Stat.HP to 4)),
        MAX_IVS,
        Level(100),
        listOf(FLAMETHROWER, EARTHQUAKE)
    )
    val activeCharizard = newActivePokemonFromSpec(charizardSpec)

    blastoiseSpec = PokemonSpec(
        BLASTOISE,
        AbilityId("Torrent"),
        Gender.FEMALE,
        Nature.MODEST,
        makeEvs(mapOf(Stat.SPECIAL_ATTACK to 252, Stat.SPEED to 252, Stat.HP to 4)),
        MAX_IVS,
        Level(100),
        listOf(SURF, TACKLE, EARTHQUAKE)
    )
    val activeBlastoise = newActivePokemonFromSpec(blastoiseSpec)

    magcargoSpec = PokemonSpec(
        MAGCARGO,
        AbilityId("Flame Body"),
        Gender.MALE,
        Nature.HARDY,
        makeEvs(ImmutableMap.of()),
        MAX_IVS,
        Level(100),
        listOf(FLAMETHROWER)
    )

    val blackSide = Side(activeCharizard, ImmutableMap.of())
    val whiteSide = Side(activeBlastoise, ImmutableMap.of())

    val rng = RandomNumberGenerator(REALISTIC_RANDOM_POLICY, Random())
    context = BattleContext(rng, debugLogger())
    charizardVsBlastoise = Battle(1, blackSide, whiteSide, null, null, Phase.BEGIN, null)
  }

  @Test
  fun initialChoices() {
    assertThat(charizardVsBlastoise.choices(Player.BLACK))
        .containsExactly(MoveChoice(FLAMETHROWER), MoveChoice(EARTHQUAKE))
    assertThat(charizardVsBlastoise.choices(Player.WHITE))
        .containsExactly(MoveChoice(SURF), MoveChoice(TACKLE), MoveChoice(EARTHQUAKE))
  }

  @Test
  fun simulateFirstTurn() {
    val turn2 =
        simulateBattle(charizardVsBlastoise, context, MoveChoice(EARTHQUAKE), MoveChoice(TACKLE))

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
      simulation = simulateBattle(simulation, context, MoveChoice(EARTHQUAKE), MoveChoice(TACKLE))
    }
    assertThat(simulation.winner()).isEqualTo(Player.BLACK)
  }

  @Test
  fun simulateNotVeryEffectiveMove() {
    val turn2 =
        simulateBattle(charizardVsBlastoise, context, MoveChoice(FLAMETHROWER), MoveChoice(TACKLE))

    assertThat(turn2.turn).isEqualTo(2)
    assertThat(turn2.phase).isEqualTo(Phase.BEGIN)
    assertThat(turn2.blackChoice).isNull()
    assertThat(turn2.whiteChoice).isNull()

    // FT deals between 45 and 54 damage to Blastoise, since it's not very effective
    val damage = 300 - turn2.whiteSide.active.hp
    assertThat(damage).isAtLeast(45)
    assertThat(damage).isAtMost(54)
  }

  @Test
  fun simulateImmuneMove() {
    val turn2 =
        simulateBattle(charizardVsBlastoise, context, MoveChoice(EARTHQUAKE), MoveChoice(EARTHQUAKE))

    assertThat(turn2.turn).isEqualTo(2)
    assertThat(turn2.phase).isEqualTo(Phase.BEGIN)
    assertThat(turn2.blackChoice).isNull()
    assertThat(turn2.whiteChoice).isNull()

    // EQ shouldn't have done anything to Charizard.
    assertThat(turn2.blackSide.active.hp).isEqualTo(298)
  }

  @Test
  fun simulateFaintAndSwitch() {
    // Magcargo vs Blastoise.
    val blackSide = Side(newActivePokemonFromSpec(magcargoSpec),
        ImmutableMap.of(charizardSpec.species.id, newBenchedPokemonFromSpec(charizardSpec)))
    val whiteSide = Side(newActivePokemonFromSpec(blastoiseSpec), ImmutableMap.of())
    val beginning = Battle(1, blackSide, whiteSide, null, null, Phase.BEGIN, null)

    // Wipe Magcargo out in one move with a 4x effective Surf.
    val faintedMagcargoBattle =
        simulateBattle(beginning, context, MoveChoice(FLAMETHROWER), MoveChoice(SURF))

    val magcargo = faintedMagcargoBattle.blackSide.active
    assertThat(magcargo.species).isEqualTo(MAGCARGO)
    assertThat(magcargo.hp).isEqualTo(0)
    assertThat(magcargo.condition).isEqualTo(Condition.FAINT)
    assertThat(faintedMagcargoBattle.whiteSide.active.condition).isEqualTo(Condition.OK)

    assertThat(faintedMagcargoBattle.choices(Player.BLACK))
        .containsExactly(SwitchChoice(CHARIZARD.id))
    assertThat(faintedMagcargoBattle.choices(Player.WHITE))
        .isEmpty()

    assertThat(faintedMagcargoBattle.phase).isEqualTo(Phase.END)

    // Switch to Charizard. The game should continue to the beginning of the next turn, where
    // Black has an empty bench.
    val turn2 = simulateBattle(faintedMagcargoBattle, context, SwitchChoice(CHARIZARD.id), null)
    assertThat(turn2.turn).isEqualTo(2)
    assertThat(turn2.phase).isEqualTo(Phase.BEGIN)
    assertThat(turn2.blackSide.active.species).isEqualTo(CHARIZARD)
    assertThat(turn2.blackSide.bench).hasSize(0)
  }

  // When more abilities are implemented, we should test that priority successfully controls
  // who switches in after fainting first.
}
