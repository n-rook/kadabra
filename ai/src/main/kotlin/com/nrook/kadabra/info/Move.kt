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
     * The name of a move.
     *
     * For instance, Power Gem has an [id] of "powergem", but a name of "Power Gem".
     */
    val name: String,

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
     * If true, the moving Pokemon will switch out if their move executes successfully.
     *
     * If this is an attack, like U-Turn, it only triggers if the move actually connects.
     * For instance, if the opponent is immune to the move, this effect will not trigger.
     *
     * Effects which take place upon a Pokemon fainting happen before the switch. For instance, if
     * a U-Turn causes a Pokemon with Aftermath to faint, the U-Turn Pokemon will take the hit.
     *
     * TODO(nrook): Implement
     */
    val selfSwitch: Boolean,

    /**
     * Whether or not we read in all the mechanics behind this move.
     */
    val fullyRepresented: Boolean
) {
  fun fullyUnderstood(): Boolean {
    return fullyRepresented
      && !selfSwitch
  }
}

/**
 * The ID of a move.
 *
 * This is an all-lowercase string, like "powergem".
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
