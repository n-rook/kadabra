package com.nrook.kadabra.mechanics.formulas

import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.Nature
import com.nrook.kadabra.mechanics.NatureEffect

// Credit goes to:
// https://www.dragonflycave.com/mechanics/stats

/**
 * Compute the value of a given stat for a Pokemon.
 */
fun computeStat(
    stat: Stat,
    baseStat: Int,
    iv: Int,
    ev: Int,
    nature: Nature,
    level: Level
): Int {
  if (stat == Stat.HP) {
    return computeStat(baseStat, iv, ev, NatureEffect.NO_EFFECT, level, true)
  } else {
    return computeStat(baseStat, iv, ev, nature.effectOn(stat), level, false)
  }
}

/**
 * Compute the value of a given stat for a Pokemon.
 *
 * This is suitable for computing an unmodified stat value.
 *
 * @param baseStat The Pokemon species's base stat.
 * @param iv The pokemon's IV.
 * @param ev The pokemon's EV for this stat.
 * @param natureEffect Whether or not the Pokemon has a beneficial or harmful nature for this stat.
 * @param level The Pokemon's level.
 * @param isHp Whether the stat being computed is HP or not.
 */
fun computeStat(
    baseStat: Int,
    iv: Int,
    ev: Int,
    natureEffect: NatureEffect,
    level: Level,
    isHp: Boolean
): Int {
  return if (isHp) {
    if (natureEffect != NatureEffect.NO_EFFECT) {
      throw IllegalArgumentException("There is no nature which affects HP!")
    }
    computeHp(baseStat, iv, ev, level)
  } else {
    computeNonHpStat(baseStat, iv, ev, natureEffect, level)
  }
}

private fun computeNonHpStat(
    baseStat: Int,
    iv: Int,
    ev: Int,
    natureEffect: NatureEffect,
    level: Level
): Int {
  val derivedEv = computeDerivedEv(ev)
  val basePlusStatModifiers: Int = 2 * baseStat + iv + derivedEv
  val levelModifier: Double = level.value / 100.0
  val preNatureActualValue: Int = (basePlusStatModifiers * levelModifier + 5).toInt()
  return (preNatureActualValue * natureEffect.modifier).toInt()
}

private fun computeHp(
    baseStat: Int,
    iv: Int,
    ev: Int,
    level: Level
): Int {
  val derivedEv = computeDerivedEv(ev)
  val basePlusStatModifiers: Int = 2 * baseStat + iv + derivedEv
  val levelModifier: Double = level.value / 100.0
  return (basePlusStatModifiers * levelModifier + level.value + 10).toInt()
}

private fun computeDerivedEv(ev: Int): Int {
  return ev / 4
}