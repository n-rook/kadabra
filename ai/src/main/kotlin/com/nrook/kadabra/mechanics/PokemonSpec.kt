package com.nrook.kadabra.mechanics

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.nrook.kadabra.info.*
import com.nrook.kadabra.mechanics.formulas.computeStat

/**
 * Represents a specific Pokemon on a team.
 *
 * PokemonSpec as a data structure will not change throughout a battle. Even if a Pokemon changes
 * form through Mega Evolution, changes ability through Skill Swap, and so on, PokemonSpec does not
 * change. Such changes are better represented by PokemonStatus.
 */
data class PokemonSpec(
    val species: Species,
    val ability: AbilityId,
    val gender: Gender,
    val nature: Nature,
    val evSpread: EvSpread,
    val ivSpread: IvSpread = MAX_IVS,
    val level: Level = Level(100),
    val moves: List<Move>
) {

  init {
    if (!species.ability.asSet().contains(ability)) {
      throw IllegalArgumentException(
          "Ability $ability not available to ${species.name}. Its abilities are ${species.ability}")
    }
    if (!species.gender.possibilities.contains(gender)) {
      throw IllegalArgumentException("Gender $gender not available to ${species.name}")
    }
    if (moves.isEmpty()) {
      throw IllegalArgumentException("Pokemon must have moves.")
    }
    if (moves.size > 4) {
      throw IllegalArgumentException(
          "Pokemon must only have 4 moves, but this one has more: $moves")
    }
  }

  /**
   * Returns the value of a given stat.
   */
  fun getStat(stat: Stat): Int {
    return computeStat(
        stat,
        baseStat = species.baseStats[stat]!!,
        iv = ivSpread.values[stat]!!,
        ev = evSpread.values[stat]!!,
        nature = nature,
        level = level
    )
  }
}

data class EvSpread(val values: Map<Stat, Int>) {
  init {
    if (values.size != 6) {
      throw IllegalArgumentException("Missing stat from ${values.keys} in EV definition")
    }
    if (values.values.any { it < 0 || it > 252 }) {
      throw IllegalArgumentException("Illegal EV value in $values")
    }
    if (values.values.sum() > 510) {
      throw IllegalArgumentException("EVs $values too high; they sum to ${values.values.sum()}")
    }
  }

  operator fun get(stat: Stat): Int {
    return values[stat]!!
  }
}

/**
 * Generate an EV spread from incomplete data.
 */
fun makeEvs(values: Map<Stat, Int>): EvSpread {
  return EvSpread(Stat.values().associate{ it to (values[it]?:0) })
}

/**
 * A convenient test object that gives a Pokemon no EVs whatsoever.
 */
val NO_EVS = makeEvs(ImmutableMap.of())

data class IvSpread(val values: Map<Stat, Int>) {
  init {
    if (values.size != 6) {
      throw IllegalArgumentException("Missing stat from ${values.keys} in IV definition")
    }
    if (values.values.any { it < 0 || it > 31 }) {
      throw IllegalArgumentException("Illegal IV value in $values")
    }
  }

  operator fun get(stat: Stat): Int {
    return values[stat]!!
  }
}

data class Level(val value: Int) {
  init {
    if (value < 1 || value > 100) {
      throw IllegalArgumentException("Illegal level $value")
    }
  }
}

val MAX_IVS = IvSpread(Stat.values().associate { Pair(it, 31) })
