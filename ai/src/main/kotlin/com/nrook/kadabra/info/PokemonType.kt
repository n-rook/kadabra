package com.nrook.kadabra.info

import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.SetMultimap

private val SUPER_EFFECTIVE_TYPES: SetMultimap<PokemonType, PokemonType> = ImmutableSetMultimap.builder<PokemonType, PokemonType>()
    .putAll(PokemonType.NORMAL)
    .build()

/**
 * Represents a Pokemon type.
 *
 * @param effective The types which attacks of this type are effective against.
 * @param notVeryEffective The types which attacks of this type are not very effective against.
 * @param ineffective The types which attacks of this type deal no damage to.
 */
enum class PokemonType {
  NORMAL,
  FIGHTING,
  FLYING,
  POISON,
  GROUND,
  ROCK,
  BUG,
  GHOST,
  STEEL,
  FIRE,
  WATER,
  GRASS,
  ELECTRIC,
  PSYCHIC,
  ICE,
  DRAGON,
  DARK,
  FAIRY
}
