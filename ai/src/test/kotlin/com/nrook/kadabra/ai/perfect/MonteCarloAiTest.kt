package com.nrook.kadabra.ai.perfect

import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.*
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.mechanics.*
import com.nrook.kadabra.mechanics.arena.*
import org.junit.Before
import org.junit.Test

class MonteCarloAiTest {

  lateinit var pokedex: Pokedex
  lateinit var charizardSpec: PokemonSpec
  lateinit var blastoiseSpec: PokemonSpec
  lateinit var venusaurSpec: PokemonSpec

  // A simple battle in which both combatants have 1 HP.
  // Here, Charizard can use flamethrower to finish the game, or belly drum to ensure a loss.
  lateinit var oneHpCharizardVsOneHpBlastoise: Battle

  @Before
  fun setUp() {
    pokedex = getGen7Pokedex()
    charizardSpec = PokemonSpec(
        pokedex.getSpeciesByName("Charizard"),
        AbilityId("Blaze"),
        Gender.MALE,
        Nature.HARDY,
        NO_EVS,
        MAX_IVS,
        Level(100),
        listOf(
            pokedex.getMoveById(MoveId("flamethrower")),
            pokedex.getMoveById(MoveId("bellydrum"))))
    blastoiseSpec = PokemonSpec(
        pokedex.getSpeciesByName("Blastoise"),
        AbilityId("Torrent"),
        Gender.MALE,
        Nature.HARDY,
        NO_EVS,
        MAX_IVS,
        Level(100),
        listOf(
            pokedex.getMoveById(MoveId("surf"))))
    venusaurSpec = PokemonSpec(
        pokedex.getSpeciesByName("Venusaur"),
        AbilityId("Overgrow"),
        Gender.MALE,
        Nature.HARDY,
        NO_EVS,
        MAX_IVS,
        Level(100),
        listOf(pokedex.getMoveById(MoveId("solarbeam"))))

    val almostDeadCharizard = newActivePokemonFromSpec(charizardSpec)
        .takeDamageAndMaybeFaint(charizardSpec.getStat(Stat.HP) - 1)
    val almostDeadBlastoise = newActivePokemonFromSpec(blastoiseSpec)
        .takeDamageAndMaybeFaint(blastoiseSpec.getStat(Stat.HP) - 1)
    oneHpCharizardVsOneHpBlastoise = Battle(
        2,
        Side(almostDeadCharizard, ImmutableMap.of()),
        Side(almostDeadBlastoise, ImmutableMap.of()),
        null,
        null,
        Phase.BEGIN,
        null)
  }

  @Test
  fun decide() {
    val almostDeadCharizard = newActivePokemonFromSpec(charizardSpec)
        .takeDamageAndMaybeFaint(charizardSpec.getStat(Stat.HP) - 1)
    val almostDeadBlastoise = newActivePokemonFromSpec(blastoiseSpec)
        .takeDamageAndMaybeFaint(blastoiseSpec.getStat(Stat.HP) - 1)
    val battle = Battle(
        2,
        Side(almostDeadCharizard, ImmutableMap.of()),
        Side(almostDeadBlastoise, ImmutableMap.of()),
        null,
        null,
        Phase.BEGIN,
        null)

    val ai = MonteCarloAi(2)  // 1 playout per choice
    val strategy = ai.decide(battle, Player.BLACK)
    assertThat(strategy.choices[MoveChoice(pokedex.getMoveById(MoveId("flamethrower")))])
        .isWithin(0.01).of(1.0)
  }

  @Test
  fun chooseLead() {
    // Just pick one randomly.
    val ai = MonteCarloAi(1)
    val leadChoice =
        ai.chooseLead(listOf(charizardSpec, blastoiseSpec), listOf(venusaurSpec), Player.BLACK)
    assertThat(leadChoice.choices).hasSize(2)
    assertThat(leadChoice.choices[0]).isWithin(0.01).of(0.5)
    assertThat(leadChoice.choices[1]).isWithin(0.01).of(0.5)
  }
}