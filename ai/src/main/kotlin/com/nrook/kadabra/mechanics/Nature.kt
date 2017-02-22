package com.nrook.kadabra.mechanics

import com.nrook.kadabra.info.Stat

/**
 * Defines each available Pokemon nature.
 *
 *
 */
enum class Nature(
    /**
     * The stat boosted by this nature.
     */
    val strong: Stat?,

    /**
     * The stat weakened by this nature.
     */
    val weak: Stat?) {
  HARDY(null, null),
  LONELY(Stat.ATTACK, Stat.DEFENSE),
  BRAVE(Stat.ATTACK, Stat.SPEED),
  ADAMANT(Stat.ATTACK, Stat.SPECIAL_ATTACK),
  NAUGHTY(Stat.ATTACK, Stat.SPECIAL_DEFENSE),
  BOLD(Stat.DEFENSE, Stat.ATTACK),
  DOCILE(null, null),
  RELAXED(Stat.DEFENSE, Stat.SPEED),
  IMPISH(Stat.DEFENSE, Stat.SPECIAL_ATTACK),
  LAX(Stat.DEFENSE, Stat.SPECIAL_DEFENSE),
  TIMID(Stat.SPEED, Stat.ATTACK),
  HASTY(Stat.SPEED, Stat.DEFENSE),
  SERIOUS(null, null),
  JOLLY(Stat.SPEED, Stat.SPECIAL_ATTACK),
  NAIVE(Stat.SPEED, Stat.SPECIAL_DEFENSE),
  MODEST(Stat.SPECIAL_ATTACK, Stat.ATTACK),
  MILD(Stat.SPECIAL_ATTACK, Stat.DEFENSE),
  QUIET(Stat.SPECIAL_ATTACK, Stat.SPEED),
  BASHFUL(null, null),
  RASH(Stat.SPECIAL_ATTACK, Stat.SPECIAL_DEFENSE),
  CALM(Stat.SPECIAL_DEFENSE, Stat.ATTACK),
  GENTLE(Stat.SPECIAL_DEFENSE, Stat.ATTACK),
  SASSY(Stat.SPECIAL_DEFENSE, Stat.SPEED),
  CAREFUL(Stat.SPECIAL_DEFENSE, Stat.SPECIAL_ATTACK),
  QUIRKY(null, null);

  fun effectOn(stat: Stat): NatureEffect {
    if (stat == this.strong) {
      return NatureEffect.STRENGTHENS
    } else if (stat == this.weak) {
      return NatureEffect.WEAKENS
    } else {
      return NatureEffect.NO_EFFECT
    }
  }
}

/**
 * Holds the effect of a nature on a stat.
 *
 * @property modifier The multiplier of the nature on the given stat.
 */
enum class NatureEffect(val modifier: Double) {
  NO_EFFECT(1.0),
  STRENGTHENS(1.1),
  WEAKENS(0.9),
}