package com.nrook.kadabra.mechanics.arena

import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.PokemonId


/**
 * The choice a player makes to kick off a turn.
 */
interface Choice

/**
 * A choice of move.
 */
data class MoveChoice(val move: Move): Choice

/**
 * A choice to switch to another Pokemon.
 *
 * This choice represents all sorts of switches: during combat, before the end of the turn, or
 * after the end of the turn.
 *
 * @param target The original ID of the Pokemon to be switched in. For instance, if switching into
 *  Mega Garchomp, this is generally going to be "garchomp".
 */
data class SwitchChoice(val target: PokemonId): Choice
