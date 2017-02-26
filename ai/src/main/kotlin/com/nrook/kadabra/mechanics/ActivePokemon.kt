package com.nrook.kadabra.mechanics

import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.Species
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.formulas.computeStat

data class ActivePokemon(
    val species: Species,
    val originalSpec: PokemonSpec,
    val hp: Int,
    val condition: Condition) {

  /**
   * The current moves available to this Pokemon.
   *
   * At some point I'll have to make this fancier to allow for disabled moves and PP-tracking.
   */
  val moves: List<Move>
    get() = this.originalSpec.moves

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

  /**
   * Take some damage, and change condition to FAINT if applicable.
   *
   * Don't call this on a Pokemon which has already fainted.
   */
  fun takeDamageAndMaybeFaint(damage: Int): ActivePokemon {
    if (condition == Condition.FAINT) {
      throw IllegalArgumentException("This Pokemon already fainted")
    }

    val newHp = Math.max(hp - damage, 0)
    val newCondition = if (newHp == 0) Condition.FAINT else condition
    return ActivePokemon(species, originalSpec, newHp, newCondition)
  }
}

fun newActivePokemonFromSpec(spec: PokemonSpec): ActivePokemon {
  return ActivePokemon(spec.species, spec, spec.getStat(Stat.HP), Condition.OK)
}

enum class Condition {
  OK,
  POISON,
  BURN,
  PARALYSIS,
  FREEZE,
  FAINT
}
