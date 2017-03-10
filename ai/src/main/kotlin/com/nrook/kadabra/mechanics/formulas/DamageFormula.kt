package com.nrook.kadabra.mechanics.formulas

import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.ImmutableSortedMultiset
import com.google.common.collect.ImmutableSortedSet
import com.nrook.kadabra.mechanics.Level

// Source: http://www.smogon.com/bw/articles/bw_complete_damage_formula

private val MODIFIER_50 = 0x800
private val MODIFIER_75 = 0xC00
private val MODIFIER_120 = 0x1333
private val MODIFIER_150 = 0x1800 // 4096 * 3 / 2
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
 * As computeDamage, but returns the minimum and maximum possible random damage from this attack.
 */
fun computeDamageRange(level: Level, offensiveStat: Int, defensiveStat: Int, movePower: Int,
                  effectiveness: TypeDamage, modifiers: Set<Modifier>): IntRange {
  val computeFromRandomRoll: (Int) -> Int = {
    computeDamage(level, offensiveStat, defensiveStat, movePower, effectiveness, it, modifiers)
  }
  return computeFromRandomRoll(85)..computeFromRandomRoll(100)
}

/**
 * Returns the exact distribution of possible outcomes from an attack, as an
 * ImmutableSortedMultiset with precisely 16 elements. This is expensive, but its accuracy makes it
 * a nice way to test that our damage formula is perfect.
 */
fun computeDamageDistribution(level: Level, offensiveStat: Int, defensiveStat: Int, movePower: Int,
                              effectiveness: TypeDamage, modifiers: Set<Modifier>):
    ImmutableSortedMultiset<Int> {
  val builder: ImmutableSortedMultiset.Builder<Int> = ImmutableSortedMultiset.naturalOrder()
  for (damageRoll in 85..100) {
    builder.add(computeDamage(
        level, offensiveStat, defensiveStat, movePower, effectiveness, damageRoll, modifiers))
  }
  return builder.build()
}

/**
 * Returns the range of possible damage values given these constants.
 *
 * @param level The level of the attacking Pokemon.
 * @param offensiveStat The offensive stat of the attacker, with any modifiers already applied.
 * @param defensiveStat The defensive stat of the defender, with any modifiers already applied.
 * @param movePower The power of the move.
 * @param effectiveness Whether the attack is super-effective, not very effective, etc.
 * @param damageRoll An integer between 85 and 100, which represents the effects of chance on the
 *  damage roll. 85 results in minimal damage, whereas 100 results in the maximum possible damage.
 *
 */
fun computeDamage(level: Level, offensiveStat: Int, defensiveStat: Int, movePower: Int,
                  effectiveness: TypeDamage, damageRoll: Int, modifiers: Set<Modifier>): Int {
  if (damageRoll !in 85..100) {
    throw IllegalArgumentException("Damage roll must be between 85 and 100, but it's $damageRoll")
  }

  val groupedModifiers = groupModifiersByPhase(modifiers)
  var damage = computeUnmodifiedDamage(level, offensiveStat, defensiveStat, movePower)

  // Multi-target modifier
  // Weather modifier
  // Critical hit modifier
  damage = applyModifiers(damage, groupedModifiers[ModifierPhase.CRITICAL_HIT])

  // Random damage
  damage = (damage * damageRoll) / 100

  // STAB
  damage = applyModifiers(damage, groupedModifiers[ModifierPhase.STAB])

  // Effectiveness
  damage = effectiveness.apply(damage)
  if (damage == 0) {
    return damage
  }

  // Burn
  // Minimum of 1 damage
  damage = Math.max(damage, 0)
  // Final modifier
  return applyModifiers(damage, groupedModifiers[ModifierPhase.FINAL_MODIFIER])
}

/**
 * Returns all modifiers as an ImmutableSetMultimap.
 *
 * <p>Modifiers in a phase are ordered by which ones should be applied first.
 */
private fun groupModifiersByPhase(modifiers: Set<Modifier>): ImmutableSetMultimap<ModifierPhase, Modifier> {
  val map = ImmutableSetMultimap.builder<ModifierPhase, Modifier>()
  for (modifier in ImmutableSortedSet.copyOf(modifiers)) {
    map.put(modifier.phase, modifier)
  }
  return map.build()

}

private fun applyModifiers(value: Int, modifiers: Iterable<Modifier>): Int {
  // "Chain" multiple modifiers together.
  var currentModifierValue = 0x1000
  for (modifier in modifiers) {
    // cv = (cv * m + 2048) / 4096
    //    = cv * (m / 4096) + 1/2
    // In other words, we multiply by the "real fractional value" of a modifier, since modifiers
    // are meant to be applied by taking their numerator and dividing by 4096. Then we multiply this
    // by the current value, and add 1/2 for rounding purposes.
    // TODO: Try integer division by 0x1000 aka 4096, and see if it's exactly the same or not
    currentModifierValue = ((currentModifierValue * modifier.numerator) + 0x800).shr(12)
  }

  val result = value * currentModifierValue / MODIFIER_DENOMINATOR.toDouble()
  return roundDown(result)
}

private fun roundDown(value: Double): Int {
  val int = value.toInt()
  val remainder = value - int
  return if (remainder <= 0.5)
    int
    else int + 1
}

/**
 * Pokemon makes extensive use of "Modifiers": specific factors which adjust the outcome of battle.
 * It's important to exactly simulate these, as Kadabra uses actual damage numbers against its own
 * Pokemon to guess at its opponents' exact statistics.
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

/**
 * All modifiers are applied in a certain order.
 */
enum class ModifierPhase {
  // There is a multi-target penalty, but we don't simulate it since this is a singles bot.
  WEATHER,
  CRITICAL_HIT,
  STAB,
  TYPE_EFFECTIVENESS,
  BURN_PENALTY,

  /**
   * Many effects are applied at this time.
   */
  FINAL_MODIFIER
}

/**
 * Defines a modifier.
 *
 * Be aware! The natural order of modifiers in this class has meaning: Modifiers which are listed
 * earlier are intended to be applied earlier than modifiers listed later.
 */
enum class Modifier(

    /**
     * The phase during which the modifier is applied.
     */
    val phase: ModifierPhase,

    /**
     * The strength of the modifier. Roughly, the modifier is equal to this value over 4096.
     */
    val numerator: Int): Comparable<Modifier> {

  /**
   * The attack was a critical hit.
   */
  CRITICAL_HIT(ModifierPhase.CRITICAL_HIT, MODIFIER_150),

  /**
   * The attack was of a type possessed by its user.
   */
  STAB(ModifierPhase.STAB, MODIFIER_150),

  /**
   * The attack was a special move, and Light Screen was up.
   *
   * This modifier's base value is different in doubles.
   */
  LIGHT_SCREEN(ModifierPhase.FINAL_MODIFIER, MODIFIER_50),

  /**
   * The attack was super-effective, but the defender had the ability Solid Rock or Filter.
   */
  SE_VERSUS_SOLID_ROCK(ModifierPhase.FINAL_MODIFIER, MODIFIER_75),

  /**
   * The attack was super-effective, and the attack had an Expert Belt.
   */
  SE_EXPERT_BELT(ModifierPhase.FINAL_MODIFIER, MODIFIER_120)
}
