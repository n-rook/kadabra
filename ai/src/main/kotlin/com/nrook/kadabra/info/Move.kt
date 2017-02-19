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
     * Whether or not we are actually representing all the mechanics behind
     * this move.
     */
    val fullyUnderstood: Boolean
)

/**
 * The ID of a move.
 */
data class MoveId(val str: String)