package com.nrook.kadabra.usage

import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.read.getGen7Pokedex
import org.junit.BeforeClass
import org.junit.Test

/**
 * Heavyweight tests that check that all Pokemon, moves and abilities in the usage data are in
 * the Pokedex, too.
 */
class UsageDataPokedexCoherenceTest {

  companion object {
    lateinit var pokedex: Pokedex
    lateinit var usageData: UsageDataset

    @BeforeClass
    @JvmStatic
    fun loadDatasets() {
      pokedex = getGen7Pokedex()
      usageData = getOuUsageDataset()
    }
  }

  @Test
  fun allUsedPokemonAreKnown() {
    for (speciesName: String in usageData.data.keys) {
      pokedex.getSpeciesByName(speciesName)  // no exception
    }
  }

  @Test
  fun allMovesAreKnown() {
    for (usageData in usageData.data.values) {
      for (move in usageData.moves.keys) {
        pokedex.getMoveByUsageCode(move)  // no exception
      }
    }
  }

  @Test
  fun allAbilitiesAreKnown() {
    val abilityNames = usageData.data.values.flatMap { it.abilities }
        .map { it.ability }
        .toSet()
    for (abilityName: String in abilityNames) {
      pokedex.getAbilityByUsageCode(abilityName)
    }
  }
}