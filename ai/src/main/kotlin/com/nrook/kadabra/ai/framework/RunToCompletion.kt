package com.nrook.kadabra.ai.framework

import com.nrook.kadabra.ai.perfect.RandomAi
import com.nrook.kadabra.mechanics.arena.Battle
import com.nrook.kadabra.mechanics.arena.BattleContext
import com.nrook.kadabra.mechanics.arena.Player
import com.nrook.kadabra.mechanics.arena.simulateBattle

/**
 * Run a battle to completion, and return the winner.
 *
 * This is intended to appear in performance-critical code.
 */
fun runToCompletion(
    battle: Battle, blackAi: RandomAi, whiteAi: RandomAi, context: BattleContext): Player {
  var battleStatus = battle
  while (battleStatus.winner() == null) {
    battleStatus = simulateBattle(
        battleStatus,
        context,
        blackAi.decide(battleStatus, Player.BLACK),
        whiteAi.decide(battleStatus, Player.WHITE))
  }
  return battleStatus.winner()!!
}
