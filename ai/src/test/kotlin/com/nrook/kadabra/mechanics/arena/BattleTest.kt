package com.nrook.kadabra.mechanics.arena

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.*
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.mechanics.*
import com.nrook.kadabra.mechanics.rng.MoveDamagePolicy
import com.nrook.kadabra.mechanics.rng.RandomNumberGenerator
import com.nrook.kadabra.mechanics.rng.RandomPolicy
import com.nrook.kadabra.mechanics.testing.TestSpecBuilder
import org.junit.Before
import org.junit.Test
import java.util.*

class BattleTest {

  lateinit var pokedex: Pokedex
  lateinit var charizardSpec: PokemonSpec
  lateinit var blastoiseSpec: PokemonSpec
  lateinit var magcargoSpec: PokemonSpec
  lateinit var charizardVsBlastoise: Battle

  lateinit var flamethrower: Move
  lateinit var earthquake: Move
  lateinit var surf: Move
  lateinit var tackle: Move
  lateinit var context: BattleContext

  @Before
  fun setUp() {
    pokedex = getGen7Pokedex()
    charizardSpec = TestSpecBuilder.create(pokedex, "Charizard")
        .withNature(Nature.ADAMANT)
        .withEvSpread(Stat.ATTACK, Stat.SPEED, Stat.HP)
        .withMoves("flamethrower", "earthquake")
        .build()

    val activeCharizard = newActivePokemonFromSpec(charizardSpec)

    blastoiseSpec = TestSpecBuilder.create(pokedex, "Blastoise")
        .withNature(Nature.MODEST)
        .withEvSpread(Stat.SPECIAL_ATTACK, Stat.SPEED, Stat.HP)
        .withMoves("surf", "tackle", "earthquake")
        .build()
    val activeBlastoise = newActivePokemonFromSpec(blastoiseSpec)

    magcargoSpec = TestSpecBuilder.create(pokedex, "Magcargo")
        .withMoves("flamethrower")
        .build()

    val blackSide = Side(activeCharizard, ImmutableMap.of())
    val whiteSide = Side(activeBlastoise, ImmutableMap.of())

    val rng = RandomNumberGenerator(RandomPolicy(MoveDamagePolicy.ONE), Random())
    context = BattleContext(rng, debugLogger())
    charizardVsBlastoise = Battle(1, blackSide, whiteSide, null, null, Phase.BEGIN, null)

    flamethrower = pokedex.getMoveById(MoveId("flamethrower"))
    earthquake = pokedex.getMoveById(MoveId("earthquake"))
    surf = pokedex.getMoveById(MoveId("surf"))
    tackle = pokedex.getMoveById(MoveId("tackle"))
  }

  @Test
  fun initialChoices() {
    assertThat(charizardVsBlastoise.choices(Player.BLACK))
        .containsExactly(MoveChoice(flamethrower), MoveChoice(earthquake))
    assertThat(charizardVsBlastoise.choices(Player.WHITE))
        .containsExactly(MoveChoice(surf), MoveChoice(tackle), MoveChoice(earthquake))
  }

  @Test
  fun simulateFirstTurn() {
    val turn2 =
        simulateBattle(charizardVsBlastoise, context, MoveChoice(earthquake), MoveChoice(tackle))

    assertThat(turn2.turn).isEqualTo(2)
    assertThat(turn2.phase).isEqualTo(Phase.BEGIN)
    assertThat(turn2.blackChoice).isNull()
    assertThat(turn2.whiteChoice).isNull()

    // EQ deals between 90 and 106 damage to Blastoise, who has 300 HP
    assertThat(turn2.whiteSide.active.hp).isEqualTo(300 - 97)

    // Tackle does only 28 to 33  damage to Charizard
    assertThat(turn2.blackSide.active.hp).isEqualTo(298 - 30)

    // With these moves, Charizard will win.
    assertThat(turn2.winner()).isNull()
    var simulation = turn2
    while (simulation.winner() == null) {
      simulation = simulateBattle(simulation, context, MoveChoice(earthquake), MoveChoice(tackle))
    }
    assertThat(simulation.winner()).isEqualTo(Player.BLACK)
  }

  @Test
  fun simulateNotVeryEffectiveMove() {
    val turn2 =
        simulateBattle(charizardVsBlastoise, context, MoveChoice(flamethrower), MoveChoice(tackle))

    assertThat(turn2.turn).isEqualTo(2)
    assertThat(turn2.phase).isEqualTo(Phase.BEGIN)
    assertThat(turn2.blackChoice).isNull()
    assertThat(turn2.whiteChoice).isNull()

    // FT deals between 45 and 54 damage to Blastoise, since it's not very effective
    val damage = 300 - turn2.whiteSide.active.hp
    assertThat(damage).isEqualTo(49)
  }

  @Test
  fun simulateImmuneMove() {
    val turn2 =
        simulateBattle(charizardVsBlastoise, context, MoveChoice(earthquake), MoveChoice(earthquake))

    assertThat(turn2.turn).isEqualTo(2)
    assertThat(turn2.phase).isEqualTo(Phase.BEGIN)
    assertThat(turn2.blackChoice).isNull()
    assertThat(turn2.whiteChoice).isNull()

    // EQ shouldn't have done anything to Charizard.
    assertThat(turn2.blackSide.active.hp).isEqualTo(298)
  }

  @Test
  fun simulateSwitch() {
    val battle = startBattle(
        ImmutableList.of(charizardSpec), 0,
        ImmutableList.of(magcargoSpec, blastoiseSpec), 0,
        context)
    val turn2 = simulateBattle(
        battle,
        context,
        MoveChoice(flamethrower),
        SwitchChoice(blastoiseSpec.species.id))

    val newCharizard = turn2.blackSide.active
    assertThat(newCharizard.hp).isEqualTo(newCharizard.getStat(Stat.HP))

    val switchedOutMagcargo = turn2.whiteSide.bench[magcargoSpec.species.id]!!
    assertThat(switchedOutMagcargo.species.id).isEqualTo(PokemonId("magcargo"))
    assertThat(switchedOutMagcargo.hp).isEqualTo(switchedOutMagcargo.maxHp)

    val switchedInBlastoise = turn2.whiteSide.active
    assertThat(switchedInBlastoise.species.id).isEqualTo(PokemonId("blastoise"))
    // Blastoise got hit by Flamethrower
    assertThat(switchedInBlastoise.hp).isEqualTo(300 - 49)
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
        simulateBattle(beginning, context, MoveChoice(flamethrower), MoveChoice(surf))

    val magcargo = faintedMagcargoBattle.blackSide.active
    assertThat(magcargo.species).isEqualTo(pokedex.getSpeciesByName("Magcargo"))
    assertThat(magcargo.hp).isEqualTo(0)
    assertThat(magcargo.condition).isEqualTo(Condition.FAINT)
    assertThat(faintedMagcargoBattle.whiteSide.active.condition).isEqualTo(Condition.OK)

    assertThat(faintedMagcargoBattle.choices(Player.BLACK))
        .containsExactly(SwitchChoice(PokemonId("charizard")))
    assertThat(faintedMagcargoBattle.choices(Player.WHITE))
        .isEmpty()

    assertThat(faintedMagcargoBattle.phase).isEqualTo(Phase.END)

    // Switch to Charizard. The game should continue to the beginning of the next turn, where
    // Black has an empty bench.
    val turn2 = simulateBattle(
        faintedMagcargoBattle,
        context,
        SwitchChoice(charizardSpec.species.id),
        null)
    assertThat(turn2.turn).isEqualTo(2)
    assertThat(turn2.phase).isEqualTo(Phase.BEGIN)
    assertThat(turn2.blackSide.active.species).isEqualTo(charizardSpec.species)
    assertThat(turn2.blackSide.bench).hasSize(0)
  }

  // When more abilities are implemented, we should test that priority successfully controls
  // who switches in after fainting first.
}
