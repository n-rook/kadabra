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

  companion object {

    /**
     * Creates a PokemonSpec from a PokemonDefinition.
     *
     * TODO: This is ugly. Maybe this should be in some sort of central dataset class?
     */
    fun createFromPokemonDefinition(
        definition: PokemonDefinition,
        pokedex: Pokedex): PokemonSpec {
      val species = pokedex.getSpeciesByName(definition.species)

      // The problem here is that usage data is incompatible with pokedex data.
      // In the pokedex, Dugtrio has Arena Trap. In usage data, it has arenatrap.
      // Similarly, in usage data, Pokemon are listed by name, not by id.
      // This is too much work for a static factory method. It should be done somewhere else.

//      val species = pokedex[PokemonId(definition.species)]!!
      val gender = species.gender.possibilities.first()
      val nature: Nature = Nature.valueOf(definition.nature.name)
      val moves: List<Move> = ImmutableList.copyOf(
          definition.moves.map({pokedex.getMoveByUsageCode(it)}))

      return PokemonSpec(
          pokedex.getSpeciesByName(definition.species),
          pokedex.getAbilityByUsageCode(definition.ability),
          gender,
          nature,
          makeEvs(definition.evs),
          IvSpread(definition.ivs),
          Level(100),
          moves)
    }
  }

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
}

/**
 * Generate an EV spread from incomplete data. Primarily for tests.
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
}

data class Level(val value: Int) {
  init {
    if (value < 1 || value > 100) {
      throw IllegalArgumentException("Illegal level $value")
    }
  }
}

val MAX_IVS = IvSpread(Stat.values().associate { Pair(it, 31) })
