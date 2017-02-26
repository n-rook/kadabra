package com.nrook.kadabra.mechanics.arena

import com.nrook.kadabra.info.Move


/**
 * The choice a player makes to kick off a turn.
 */
interface Choice

/**
 * A choice of move.
 */
data class MoveChoice(val move: Move): Choice
