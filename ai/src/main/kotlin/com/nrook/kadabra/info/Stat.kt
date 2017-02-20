package com.nrook.kadabra.info

import com.google.common.collect.ImmutableMap
import com.nrook.kadabra.proto.EvSpread

enum class Stat(val abbreviation: String) {
  HP("HP"),
  ATTACK("Atk"),
  DEFENSE("Def"),
  SPECIAL_ATTACK("SpA"),
  SPECIAL_DEFENSE("SpD"),
  SPEED("Spe")
}

private fun buildAbbreviationMap() : Map<String, Stat> {
  val map = ImmutableMap.builder<String, Stat>()
  for (s in Stat.values()) {
    map.put(s.abbreviation, s)
  }
  return map.build()
}
private val abbreviationMap = buildAbbreviationMap()

fun StatFromAbbreviation(abbreviation: String, caseSensitive: Boolean = true): Stat {
  if (caseSensitive) {
    return abbreviationMap[abbreviation]
        ?: throw IllegalArgumentException("Unrecognized abbreviation $abbreviation")
  } else {
    return uppercaseAbbreviationMap[abbreviation.toUpperCase()]
        ?: throw IllegalArgumentException("Unrecognized abbreviation $abbreviation")

  }
}

private val uppercaseAbbreviationMap: Map<String, Stat> =
    abbreviationMap.mapKeys { it.key.toUpperCase() }

fun SetStatOnEvSpread(evSpreadBuilder: EvSpread.Builder, s : Stat, value: Int) {
  when (s) {
    Stat.HP -> evSpreadBuilder.hp = value
    Stat.ATTACK -> evSpreadBuilder.attack = value
    Stat.DEFENSE -> evSpreadBuilder.defense = value
    Stat.SPECIAL_ATTACK -> evSpreadBuilder.specialAttack = value
    Stat.SPECIAL_DEFENSE -> evSpreadBuilder.specialDefense = value
    Stat.SPEED -> evSpreadBuilder.speed = value
  }
}