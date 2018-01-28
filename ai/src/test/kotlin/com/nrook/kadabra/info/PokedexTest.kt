package com.nrook.kadabra.info

import com.google.common.truth.Truth.assertThat
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
    assertThat(charizard.types)
        .containsExactly(PokemonType.FIRE, PokemonType.FLYING)
  }

  @Test
  fun getSpeciesByName() {
    val alolanMarowak = pokedex.getSpeciesByName("Marowak-Alola")
    assertThat(alolanMarowak.types)
        .containsExactly(PokemonType.FIRE, PokemonType.GHOST)
  }

  @Test
  fun getBaseSpecies() {
    val ashGreninja = pokedex.getSpeciesByName("Greninja-Ash")
    assertThat(ashGreninja.baseSpecies)
        .isEqualTo("Greninja")
  }

  @Test
  fun megaPokemonFormes() {
    val mawile = pokedex.getSpeciesByName("Mawile")
    val mawileMega = pokedex.getSpeciesByName("Mawile-Mega")
    assertThat(mawile.baseSpecies).isNull()
    assertThat(mawile.otherForms).containsExactly(mawileMega.id)
    assertThat(mawileMega.baseSpecies).isEqualTo(mawile.name)
    assertThat(mawileMega.otherForms).isEmpty()
  }

  @Test
  fun getMoveById() {
    val fireblast = pokedex.getMoveById(MoveId("fireblast"))
    assertThat(fireblast.type).isEqualTo(PokemonType.FIRE)
  }

  @Test
  fun getMoveByUsageCode() {
    val uturn = pokedex.getMoveByUsageCode("uturn")
    assertThat(uturn.type).isEqualTo(PokemonType.BUG)
  }

  @Test
  fun getMoveByUsageCodeHiddenPower() {
    val hp = pokedex.getMoveByUsageCode("hiddenpowerbug")
    assertThat(hp.id.str).isEqualTo("hiddenpower")
  }

  @Test
  fun getAbilityByUsageCode() {
    val beastBoost = pokedex.getAbilityByUsageCode("beastboost")
    assertThat(beastBoost.str).isEqualTo("Beast Boost")
  }
}