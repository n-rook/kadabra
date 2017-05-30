package com.nrook.kadabra.ai.framework

import com.nrook.kadabra.ai.perfect.Ai
import com.nrook.kadabra.mechanics.arena.*

/**
 * Run a battle to completion, and return the winner.
 *
 * This is intended to appear in performance-critical code.
 */
fun runToCompletion(
    battle: Battle, blackAi: Ai, whiteAi: Ai, context: BattleContext): Player {
  return runToTurnLimit(battle, blackAi, whiteAi, Int.MAX_VALUE, context)!!
}

/**
 * Run a battle, either to completion or until the turn limit runs out.
 */
fun runToTurnLimit(
    battle: Battle, blackAi: Ai, whiteAi: Ai, limit: Int, context: BattleContext): Player? {
  var battleStatus = battle
  while (battleStatus.winner() == null && battleStatus.turn < limit) {
    battleStatus = simulateBattle(
        battleStatus,
        context,
        makeChoice(battleStatus, Player.BLACK, blackAi),
        makeChoice(battleStatus, Player.WHITE, whiteAi))
  }
  return battleStatus.winner()
}

private fun makeChoice(battle: Battle, player: Player, ai: Ai): Choice? {
  val validChoices = battle.choices(player)
  if (validChoices.isEmpty()) {
    return null
  }
  return ai.decide(battle, player)
}
