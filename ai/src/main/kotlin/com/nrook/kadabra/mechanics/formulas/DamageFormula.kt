package com.nrook.kadabra.mechanics.formulas

import com.nrook.kadabra.mechanics.Level

// Source: http://www.smogon.com/bw/articles/bw_complete_damage_formula

private val MODIFIER_150 = 4096 * 3 / 2
private val MODIFIER_DENOMINATOR = 4096

/**
 * Returns the range of possible damage values given these constants.
 *
 * @param level The level of the attacking Pokemon.
 * @param offensiveStat The offensive stat of the attacker, with any modifiers already applied.
 * @param defensiveStat The defensive stat of the defender, with any modifiers already applied.
 * @param movePower The power of the move.
 * @param crit Whether or not the move is a critical hit.
 */
fun computeDamage(level: Level, offensiveStat: Int, defensiveStat: Int, movePower: Int,
                  crit: Boolean): IntRange {
  val unmodifiedDamage = computeUnmodifiedDamage(level, offensiveStat, defensiveStat, movePower)

  // apply modifiers
  val critDamage = if (crit) applyModifier(unmodifiedDamage, MODIFIER_150)
      else unmodifiedDamage

  val minDamage = critDamage * 85 / 100
  return IntRange(minDamage, critDamage)
}

/**
 * Pokemon makes extensive use of "Modifiers"; in other words,
 */
private fun applyModifier(value: Int, modifier: Int): Int {
  // These are B/W mechanics.
  // Hopefully they're accurate enough.
  // Also, I don't *think* using doubles here affects accuracy, but it might.
  val doubleResult = value * modifier / MODIFIER_DENOMINATOR.toDouble()
  return Math.round(doubleResult).toInt()
}

/**
 * Computes "unmodified" or "base" damage.
 */
private fun computeUnmodifiedDamage(
    level: Level, offensiveStat: Int, defensiveStat: Int, movePower: Int): Int {
  return (((2 * level.value) / 5 + 2) * movePower * offensiveStat / defensiveStat) / 50 + 2
}
