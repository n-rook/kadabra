package com.nrook.kadabra.info

import com.google.common.truth.Truth
import com.nrook.kadabra.info.read.getGen7Pokedex
import org.junit.Before
import org.junit.Test

class PokedexTest {
  lateinit var pokedex: Pokedex

  @Before
  fun setUp() {
    pokedex = getGen7Pokedex()
  }

  @Test
  fun getSpeciesById() {
    val charizard = pokedex.getSpeciesById(PokemonId("charizard"))
    Truth.assertThat(charizard.types)
        .containsExactly(PokemonType.FIRE, PokemonType.FLYING)
  }

  @Test
  fun getSpeciesByName() {
    val alolanMarowak = pokedex.getSpeciesByName("Marowak-Alola")
    Truth.assertThat(alolanMarowak.types)
        .containsExactly(PokemonType.FIRE, PokemonType.GHOST)
  }

  @Test
  fun getMoveById() {
    val fireblast = pokedex.getMoveById(MoveId("fireblast"))
    Truth.assertThat(fireblast.type).isEqualTo(PokemonType.FIRE)
  }

  @Test
  fun getMoveByUsageCode() {
    val uturn = pokedex.getMoveByUsageCode("uturn")
    Truth.assertThat(uturn.type).isEqualTo(PokemonType.BUG)
  }

  @Test
  fun getMoveByUsageCodeHiddenPower() {
    val hp = pokedex.getMoveByUsageCode("hiddenpowerbug")
    Truth.assertThat(hp.id.str).isEqualTo("hiddenpower")
  }

  @Test
  fun getAbilityByUsageCode() {
    val beastBoost = pokedex.getAbilityByUsageCode("beastboost")
    Truth.assertThat(beastBoost.str).isEqualTo("Beast Boost")
  }
}