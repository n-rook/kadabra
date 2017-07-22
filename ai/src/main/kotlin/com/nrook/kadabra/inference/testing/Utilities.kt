package com.nrook.kadabra.inference.testing

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.inference.BattleEvent
import com.nrook.kadabra.inference.TurnEvent

/**
 * Eliminates all events after the beginning of the given turn.
 */
fun snipToTurn(events: List<BattleEvent>, turn: Int): ImmutableList<BattleEvent> {
  val turnIndex = events.indexOfFirst { it is TurnEvent && it.turn == turn }
  if (turnIndex == -1) {
    throw IllegalArgumentException("No turn event for turn $turn in this list")
  }
  return ImmutableList.copyOf(events.subList(0, turnIndex + 1))
}