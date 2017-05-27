package com.nrook.kadabra.ai.perfect

import com.nrook.kadabra.mechanics.arena.Battle
import com.nrook.kadabra.mechanics.arena.Choice
import com.nrook.kadabra.mechanics.arena.Player
import java.util.*

/**
 * An AI which makes decisions purely at random.
 */
class RandomAi(private val random: Random) {

  /**
   * Decide which choice to make.
   *
   * @param battle The battle in which to make a choice.
   * @param player The player as whom to make the choice.
   */
  fun decide(battle: Battle, player: Player): Choice {
    val choices = battle.choices(player)
    return choices[random.nextInt(choices.size)]
  }
}