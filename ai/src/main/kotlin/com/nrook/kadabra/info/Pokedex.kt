package com.nrook.kadabra.info

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import java.util.*

/**
 * The move ID of the unique move, Hidden Power.
 */
val HIDDEN_POWER: MoveId = MoveId("hiddenpower")

/**
 * A class which holds data about Pokemon mechanics.
 */
class Pokedex private constructor(
    private val speciesById: ImmutableMap<PokemonId, Species>,
    private val speciesByName: ImmutableMap<String, Species>,
    private val moveById: ImmutableMap<MoveId, Move>,
    private val abilityByUsageCode: ImmutableMap<String, AbilityId>) {

  companion object {

    /**
     * Construct a Pokedex from known data.
     */
    fun create(species: List<Species>, moves: List<Move>): Pokedex {
      val speciesById = Maps.uniqueIndex(species, {it!!.id})
      val speciesByName = Maps.uniqueIndex(species, {it!!.name})
      val movesById = Maps.uniqueIndex(moves, {it!!.id})
      val abilities = species.flatMap { it.ability.asSet() }
          .toSet()
      val abilitiesByUsageCode = Maps.uniqueIndex(abilities, {abilityIdToUsageCode(it!!)})

      return Pokedex(speciesById, speciesByName, movesById, abilitiesByUsageCode)
    }
  }

  fun getSpeciesById(id: PokemonId): Species {
    return speciesById[id]!!
  }

  fun getSpeciesByName(name: String): Species {
    return speciesByName[name]!!
  }

  fun getMoveById(id: MoveId): Move {
    return moveById[id]!!
  }

  fun getMoveByUsageCode(usageCode: String): Move {
    val moveId = moveUsageCodeToId(usageCode)
    val move = moveById[moveId]
    return move ?: throw NoSuchElementException(
        "We cannot find a move with the ID ${moveId.str} (computed from $usageCode)")
  }

  /**
   * Returns an ability by its "usage code", the string to which it is referred to in usage data.
   *
   * Unlike PokemonId and MoveId, AbilityId are capitalized and spaced. For instance, Dugtrio's
   * signature ability is "Arena Trap", not "arenatrap". But in usage data, it's "arenatrap".
   */
  fun getAbilityByUsageCode(code: String): AbilityId {
    return abilityByUsageCode[code]!!
  }
}

private fun moveUsageCodeToId(usageCode: String): MoveId {
  if (usageCode.startsWith("hiddenpower")) {
    return HIDDEN_POWER
  }
  return MoveId(usageCode)
}

private fun abilityIdToUsageCode(id: AbilityId): String {
  return id.str.toLowerCase()
      .replace(Regex("[^a-z0-9]"), "")
}
