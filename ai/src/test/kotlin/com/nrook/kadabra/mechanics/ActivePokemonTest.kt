package com.nrook.kadabra.mechanics

import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.*
import com.nrook.kadabra.info.testdata.EARTHQUAKE
import org.junit.Test

class ActivePokemonTest {

  private val CHARIZARD: Species = Species(
      PokemonId("charizard"),
      "Charizard",
      6,
      listOf(PokemonType.FIRE, PokemonType.FLYING),
      GenderPossibilities.MALE_OR_FEMALE,
      mapOf(
          Stat.HP to 78,
          Stat.ATTACK to 84,
          Stat.DEFENSE to 78,
          Stat.SPECIAL_ATTACK to 109,
          Stat.SPECIAL_DEFENSE to 85,
          Stat.SPEED to 100
      ),
      AbilitySet(AbilityId("Blaze"), null, AbilityId("Solar Power")),
      1, 1, setOf(), null)

  @Test
  fun getStat() {
    val evs = EvSpread(mapOf(
        Stat.HP to 43,
        Stat.ATTACK to 0,
        Stat.DEFENSE to 0,
        Stat.SPECIAL_ATTACK to 252,
        Stat.SPECIAL_DEFENSE to 0,
        Stat.SPEED to 199
    ))
    val ivs = IvSpread(mapOf(
        Stat.HP to 31,
        Stat.ATTACK to 28,
        Stat.DEFENSE to 31,
        Stat.SPECIAL_ATTACK to 31,
        Stat.SPECIAL_DEFENSE to 31,
        Stat.SPEED to 31
    ))

    val thisCharizard: PokemonSpec = PokemonSpec(
        CHARIZARD,
        AbilityId("Blaze"),
        Gender.FEMALE,
        Nature.TIMID,
        evs, ivs, Level(100), listOf(EARTHQUAKE))

    val activeCharizard: ActivePokemon = ActivePokemon(CHARIZARD, thisCharizard, 1, Condition.OK, ImmutableSet.of())

    // Expected values computed with Pokemon Showdown on 2017-02-20
    assertThat(activeCharizard.getStat(Stat.HP)).isEqualTo(307)
    assertThat(activeCharizard.getStat(Stat.ATTACK)).isEqualTo(180)
    assertThat(activeCharizard.getStat(Stat.DEFENSE)).isEqualTo(192)
    assertThat(activeCharizard.getStat(Stat.SPECIAL_ATTACK)).isEqualTo(317)
    assertThat(activeCharizard.getStat(Stat.SPECIAL_DEFENSE)).isEqualTo(206)
    assertThat(activeCharizard.getStat(Stat.SPEED)).isEqualTo(313)
  }
}