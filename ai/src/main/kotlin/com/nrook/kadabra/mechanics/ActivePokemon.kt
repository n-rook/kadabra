package com.nrook.kadabra.mechanics

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterables
import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.Species
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.formulas.computeStat

/**
 * An active Pokemon, on the battlefield.
 */
data class ActivePokemon(
    val species: Species,
    val originalSpec: PokemonSpec,
    val hp: Int,
    val condition: Condition,
    val effects: Set<PokemonEffect>) {

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
    return ActivePokemon(species, originalSpec, newHp, newCondition, effects)
  }

  /**
   * Convert this Pokemon into a benched Pokemon.
   *
   * This does shave off effects which only last while a Pokemon is out.
   */
  fun toBenched(): BenchedPokemon {
    return BenchedPokemon(species, originalSpec, hp, condition)
  }

  /**
   * Adds the given effect to this Pokemon.
   */
  fun withEffect(effect: PokemonEffect): ActivePokemon {
    val newEffects = ImmutableSet.copyOf(Iterables.concat(effects, ImmutableList.of(effect)))
    return ActivePokemon(species, originalSpec, hp, condition, newEffects)
  }

  /**
   * Clears an effect on this Pokemon.
   *
   * @throws IllegalStateException if the effect is not on this Pokemon.
   */
  fun clearEffect(effect: PokemonEffect): ActivePokemon {
    if (!effects.contains(effect)) {
      throw IllegalStateException("Effect $effect is not present!")
    }
    val newEffects = HashSet(effects)
    newEffects.remove(effect)
    return ActivePokemon(species, originalSpec, hp, condition, newEffects)
  }
}

fun newActivePokemonFromSpec(spec: PokemonSpec): ActivePokemon {
  return ActivePokemon(spec.species, spec, spec.getStat(Stat.HP), Condition.OK, ImmutableSet.of())
}

enum class Condition {
  OK,
  POISON,
  BAD_POISON,
  BURN,
  PARALYSIS,
  FREEZE,
  FAINT
}
