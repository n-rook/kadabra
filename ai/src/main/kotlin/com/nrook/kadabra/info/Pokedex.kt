package com.nrook.kadabra.info

/**
 * Represents a single type of Pokemon.
 *
 * Note that different forms of a Pokemon are treated as different species.
 * For instance, Mega Pokemon and alternate formes are different species.
 */
data class Species(
    /**
     * The Pokemon's ID.
     */
    val id: PokemonId,

    /**
     * The Pokemon's actual name. "Venasaur-Mega", not "venasaurmega".
     */
    val name: String,

    /**
     * The Pokemon's number in the traditional national Dex. Not important.
     *
     * This is the same for all forms of a Pokemon.
     */
    val number: Int,

    /**
     * The types this Pokemon has. There are either one or two of these.
     *
     * A Pokemon's types have a canonical ordering, which is reflected here, but that ordering
     * doesn't make any impact on game mechanics.
     */
    val types: List<PokemonType>,

    /**
     * The sexes of this Pokemon population.
     */
    val gender: GenderPossibilities,

    /**
     * This Pokemon's base stats. The map must contain all six stats.
     */
    val baseStats: Map<Stat, Int>,

    /**
     * The abilities available to this Pokemon.
     */
    val ability: AbilitySet,

    /**
     * The Pokemon's height, in millimeters.
     */
    val heightmm: Int,

    /**
     * The Pokemon's weight, in grams.
     */
    val weightg: Int,

    /**
     * If this is a base species Pokemon, this lists other forms of this Pokemon. If this is
     * not a base species Pokemon, otherForms is empty.
     *
     * For instance, for Charizard, this contains Charizard-Mega-X and Charizard-Mega-Y, but for
     * Charizard-Mega-X, this is empty.
     */
    val otherForms: Set<PokemonId>,

    /**
     * If this is an alternate form of some Pokemon, this is a short string which describes that
     * form. For instance, for Charizard-Mega-X, this is "Mega-X".
     */
    val form: String?
) {
  init {
    if (baseStats.size != 6) {
      throw IllegalArgumentException("Base stats ${baseStats.keys} are missing some stats")
    }
  }
}

/**
 * The ID which identifies a Pokemon.
 *
 * This is lowercase and contains no special characters. "venasaurmega", not "Venasaur-Mega".
 */
data class PokemonId(val str: String)

/**
 * A Pokemon ability, such as "Intimidate".
 */
data class AbilityId(val str: String)

data class AbilitySet(
    val first: AbilityId,
    val second: AbilityId?,
    val hidden: AbilityId?
) {
  fun asSet(): Set<AbilityId> {
    return setOf(first, second, hidden).filterNotNull().toSet()
  }
}

/**
 * The sexes which a Pokemon might be.
 */
enum class GenderPossibilities(val possibilities: Set<Gender>) {
  MALE_OR_FEMALE(setOf(Gender.MALE, Gender.FEMALE)),
  ALWAYS_MALE(setOf(Gender.MALE)),
  ALWAYS_FEMALE(setOf(Gender.FEMALE)),
  GENDERLESS(setOf(Gender.GENDERLESS))
}

/**
 * The biological sex of a Pokemon.
 *
 * This is consistently called Gender by the Pokemon community, but it's really sex, not gender.
 */
enum class Gender {
  FEMALE,
  MALE,
  GENDERLESS
}