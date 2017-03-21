package com.nrook.kadabra.info

/**
 * Represents a possible move learned by Pokemon.
 */
data class Move(
    /**
     * The ID of a move.
     */
    val id: MoveId,

    /**
     * The move's base power.
     *
     * Moves which aren't attacks, like Swords Dance, have basePower of 0.
     */
    val basePower: Int,

    /**
     * The move's type.
     */
    val type: PokemonType,

    /**
     * Whether the attack is physical or special.
     */
    val category: MoveCategory,

    /**
     * Whether or not we are actually representing all the mechanics behind
     * this move.
     */
    val fullyUnderstood: Boolean
)

/**
 * The ID of a move.
 */
data class MoveId(val str: String)

/**
 * Whether a move uses physical or special attack.
 */
enum class MoveCategory {
  /**
   * The attack uses Attack and is defended against by Defense.
   */
  PHYSICAL,

  /**
   * The attack uses Special Attack and is defended against by Special Defense.
   */
  SPECIAL,

  /**
   * The attack doesn't use stats, or isn't an attack.
   */
  NOT_APPLICABLE;

  /**
   * Returns which stat is used for the attacking Pokemon to calculate the damage of this attack.
   */
  fun offensiveStat(): Stat {
    when (this) {
      PHYSICAL -> return Stat.ATTACK
      SPECIAL -> return Stat.SPECIAL_ATTACK
      NOT_APPLICABLE -> throw IllegalArgumentException()
    }
  }

  /**
   * Returns which stat is used for the defending Pokemon to calculate the damage of this attack.
   */
  fun defensiveStat(): Stat {
    when (this) {
      PHYSICAL -> return Stat.DEFENSE
      SPECIAL -> return Stat.SPECIAL_DEFENSE
      NOT_APPLICABLE -> throw IllegalArgumentException()
    }
  }
}
