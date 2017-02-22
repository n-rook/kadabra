package com.nrook.kadabra.mechanics

import com.nrook.kadabra.info.Species
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.formulas.computeStat

data class ActivePokemon(
    val species: Species,
    val originalSpec: PokemonSpec,
    val hp: Int,
    val condition: Condition) {

  /**
   * Returns the current value of a given stat.
   */
  fun getStat(stat: Stat): Int {
    return computeStat(
        stat,
        baseStat = species.baseStats[stat]!!,
        iv = originalSpec.ivSpread.values[stat]!!,
        ev = originalSpec.evSpread.values[stat]!!,
        nature = originalSpec.nature,
        level = originalSpec.level
    )
  }
}

enum class Condition {
  OK,
  POISON,
  BURN,
  PARALYSIS,
  FREEZE,
  FAINT
}