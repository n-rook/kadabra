package com.nrook.kadabra.mechanics

import com.nrook.kadabra.info.AbilityId
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.Species
import com.nrook.kadabra.info.Stat

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
    val level: Level = Level(100)
) {
  init {
    if (!species.ability.asSet().contains(ability)) {
      throw IllegalArgumentException("Ability $ability not available to ${species.name}")
    }
    if (!species.gender.possibilities.contains(gender)) {
      throw IllegalArgumentException("Gender $gender not available to ${species.name}")
    }
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
}

data class IvSpread(val values: Map<Stat, Int>) {
  init {
    if (values.size != 6) {
      throw IllegalArgumentException("Missing stat from ${values.keys} in IV definition")
    }
    if (values.values.any { it < 0 || it > 31 }) {
      throw IllegalArgumentException("Illegal IV value in $values")
    }
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
