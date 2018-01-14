package com.nrook.kadabra.inference.testing

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.inference.BattleEvent
import com.nrook.kadabra.inference.SentEvent
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

/**
 * Eliminates all events after an event matching the given definition.
 */
fun snipToEvent(events: List<BattleEvent>, matcher: (BattleEvent) -> Boolean ): ImmutableList<BattleEvent> {
  val eventIndex = events.indexOfFirst(matcher)
  if (eventIndex == -1) {
    throw IllegalArgumentException("Could not locate matching event")
  }
  return ImmutableList.copyOf(events.subList(0, eventIndex + 1))
}

/**
 * Eliminates a sent event and all events after it.
 */
fun snipUntilChoice(events: List<BattleEvent>, rqid: String): ImmutableList<BattleEvent> {
  return snipToEvent(events, { it is SentEvent && it.rqid == rqid })
}