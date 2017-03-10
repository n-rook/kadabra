package com.nrook.kadabra.mechanics.formulas

import com.nrook.kadabra.info.Effectiveness
import com.nrook.kadabra.info.PokemonType

/**
 * How effective an attack is against an opponent.
 */
enum class TypeDamage {
  QUADRUPLED,
  DOUBLED,
  NORMAL,
  HALF,
  QUARTER,
  NONE;

  /**
   * Adjusts the damage value passed in.
   */
  fun apply(damage: Int): Int {
    return when (this) {
      TypeDamage.QUADRUPLED -> damage * 4
      TypeDamage.DOUBLED -> damage * 2
      TypeDamage.NORMAL -> damage
      TypeDamage.HALF -> damage / 2
      TypeDamage.QUARTER -> damage / 4
      TypeDamage.NONE -> 0
    }
  }
}

/**
 * Returns how effective a move is against a given set of types.
 *
 * This will have to be replaced at some point to allow for Levitate, etc.
 *
 * @param attacker The type of the attacking move.
 * @param defender The types of the defending Pokemon.
 */
fun computeTypeEffectiveness(attacker: PokemonType, defender: List<PokemonType>): TypeDamage {
  if (defender.isEmpty()) {
    throw IllegalArgumentException("Defender is typeless!")
  }
  if (defender.size > 2) {
    throw IllegalArgumentException("Defender has too many types: $defender")
  }
  var exponent = 0
  for (defenderType: PokemonType in defender) {
    when (attacker.effectivenessAgainst(defenderType)) {
      Effectiveness.SUPER_EFFECTIVE -> {
        exponent += 1
      }
      Effectiveness.EFFECTIVE -> {}
      Effectiveness.NOT_VERY_EFFECTIVE -> {
        exponent -= 1
      }
      Effectiveness.NO_EFFECT -> {
        return TypeDamage.NONE
      }
    }
  }

  return when (exponent) {
    2 -> TypeDamage.QUADRUPLED
    1 -> TypeDamage.DOUBLED
    0 -> TypeDamage.NORMAL
    -1 -> TypeDamage.HALF
    -2 -> TypeDamage.QUARTER
    else -> throw AssertionError()
  }
}
