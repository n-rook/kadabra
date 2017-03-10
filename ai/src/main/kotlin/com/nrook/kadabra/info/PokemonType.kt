package com.nrook.kadabra.info

import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.SetMultimap

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
  FAIRY;

  /**
   * Returns how effective attacks of this type are against another type.
   */
  fun effectivenessAgainst(target: PokemonType): Effectiveness {
    if (SUPER_EFFECTIVE_TYPES.containsEntry(this, target)) {
      return Effectiveness.SUPER_EFFECTIVE
    } else if (NOT_VERY_EFFECTIVE_TYPES.containsEntry(this, target)) {
      return Effectiveness.NOT_VERY_EFFECTIVE
    } else if (INEFFECTIVE_TYPES.containsEntry(this, target)) {
      return Effectiveness.NO_EFFECT
    } else {
      return Effectiveness.EFFECTIVE
    }
  }
}

/**
 * The degree to which one type's attacks are effective against another.
 */
enum class Effectiveness {
  /**
   * The type is super-effective, dealing double damage.
   */
  SUPER_EFFECTIVE,

  /**
   * The type has an average level of effectiveness.
   */
  EFFECTIVE,

  /**
   * The type is not very effective, dealing half damage.
   */
  NOT_VERY_EFFECTIVE,

  /**
   * The type has no effect whatsoever.
   */
  NO_EFFECT
}

/**
 * A multimap from each type to the types against which it is super-effective.
 */
private val SUPER_EFFECTIVE_TYPES: SetMultimap<PokemonType, PokemonType> = ImmutableSetMultimap.builder<PokemonType, PokemonType>()
    .putAll(PokemonType.NORMAL)
    .putAll(PokemonType.FIGHTING, PokemonType.NORMAL, PokemonType.ICE, PokemonType.ROCK, PokemonType.DARK, PokemonType.STEEL)
    .putAll(PokemonType.FLYING, PokemonType.GRASS, PokemonType.BUG)
    .putAll(PokemonType.POISON, PokemonType.GRASS, PokemonType.FAIRY)
    .putAll(PokemonType.GROUND, PokemonType.FIRE, PokemonType.ELECTRIC, PokemonType.POISON, PokemonType.ROCK, PokemonType.STEEL)
    .putAll(PokemonType.ROCK, PokemonType.FIRE, PokemonType.ICE, PokemonType.FLYING, PokemonType.BUG)
    .putAll(PokemonType.BUG, PokemonType.GRASS, PokemonType.PSYCHIC, PokemonType.DARK)
    .putAll(PokemonType.GHOST, PokemonType.PSYCHIC, PokemonType.GHOST)
    .putAll(PokemonType.STEEL, PokemonType.ICE, PokemonType.ROCK, PokemonType.FAIRY)
    .putAll(PokemonType.FIRE, PokemonType.GRASS, PokemonType.ICE, PokemonType.BUG, PokemonType.STEEL)
    .putAll(PokemonType.WATER, PokemonType.GROUND, PokemonType.ROCK)
    .putAll(PokemonType.GRASS, PokemonType.WATER, PokemonType.GROUND, PokemonType.ROCK)
    .putAll(PokemonType.ELECTRIC, PokemonType.WATER, PokemonType.FLYING)
    .putAll(PokemonType.PSYCHIC, PokemonType.FIGHTING, PokemonType.POISON)
    .putAll(PokemonType.ICE, PokemonType.GRASS, PokemonType.GROUND, PokemonType.FLYING, PokemonType.DRAGON)
    .putAll(PokemonType.DRAGON, PokemonType.DRAGON)
    .putAll(PokemonType.DARK, PokemonType.PSYCHIC, PokemonType.GHOST)
    .putAll(PokemonType.FAIRY, PokemonType.FIGHTING, PokemonType.DRAGON, PokemonType.DARK)
    .build()

private val NOT_VERY_EFFECTIVE_TYPES: SetMultimap<PokemonType, PokemonType> = ImmutableSetMultimap.builder<PokemonType, PokemonType>()
    .putAll(PokemonType.NORMAL, PokemonType.ROCK, PokemonType.STEEL)
    .putAll(PokemonType.FIGHTING, PokemonType.POISON, PokemonType.FLYING, PokemonType.PSYCHIC, PokemonType.BUG, PokemonType.FAIRY)
    .putAll(PokemonType.FLYING, PokemonType.ELECTRIC, PokemonType.ROCK, PokemonType.STEEL)
    .putAll(PokemonType.POISON, PokemonType.POISON, PokemonType.GROUND, PokemonType.ROCK, PokemonType.GHOST)
    .putAll(PokemonType.GROUND, PokemonType.GRASS, PokemonType.BUG)
    .putAll(PokemonType.ROCK, PokemonType.FIGHTING, PokemonType.GROUND, PokemonType.STEEL)
    .putAll(PokemonType.BUG, PokemonType.FIRE, PokemonType.FIGHTING, PokemonType.POISON)
    .putAll(PokemonType.GHOST, PokemonType.DARK)
    .putAll(PokemonType.STEEL, PokemonType.FIRE, PokemonType.WATER, PokemonType.ELECTRIC, PokemonType.STEEL)
    .putAll(PokemonType.FIRE, PokemonType.FIRE, PokemonType.WATER, PokemonType.ROCK, PokemonType.DRAGON)
    .putAll(PokemonType.WATER, PokemonType.WATER, PokemonType.GRASS, PokemonType.DRAGON)
    .putAll(PokemonType.GRASS, PokemonType.FIRE, PokemonType.GRASS, PokemonType.POISON, PokemonType.FLYING, PokemonType.BUG, PokemonType.DRAGON, PokemonType.STEEL)
    .putAll(PokemonType.ELECTRIC, PokemonType.ELECTRIC, PokemonType.GRASS, PokemonType.DRAGON)
    .putAll(PokemonType.PSYCHIC, PokemonType.PSYCHIC, PokemonType.STEEL)
    .putAll(PokemonType.ICE, PokemonType.FIRE, PokemonType.WATER, PokemonType.ICE, PokemonType.STEEL)
    .putAll(PokemonType.DRAGON, PokemonType.STEEL)
    .putAll(PokemonType.DARK, PokemonType.FIGHTING, PokemonType.DARK, PokemonType.FAIRY)
    .putAll(PokemonType.FAIRY, PokemonType.FIRE, PokemonType.POISON, PokemonType.STEEL)
    .build()

private val INEFFECTIVE_TYPES: SetMultimap<PokemonType, PokemonType> = ImmutableSetMultimap.builder<PokemonType, PokemonType>()
    .putAll(PokemonType.NORMAL, PokemonType.GHOST)
    .putAll(PokemonType.FIGHTING, PokemonType.GHOST)
    .putAll(PokemonType.POISON, PokemonType.STEEL)
    .putAll(PokemonType.GROUND, PokemonType.FLYING)
    .putAll(PokemonType.GHOST, PokemonType.NORMAL)
    .putAll(PokemonType.ELECTRIC, PokemonType.GROUND)
    .putAll(PokemonType.PSYCHIC, PokemonType.DARK)
    .putAll(PokemonType.DRAGON, PokemonType.FAIRY)
    .build()

