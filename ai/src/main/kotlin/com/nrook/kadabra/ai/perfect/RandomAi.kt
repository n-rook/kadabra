package com.nrook.kadabra.ai.perfect

import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.arena.Battle
import com.nrook.kadabra.mechanics.arena.Choice
import com.nrook.kadabra.mechanics.arena.Player
import java.util.*

/**
 * An AI which makes decisions purely at random.
 */
class RandomAi(private val random: Random): Ai {

  /**
   * Decide which choice to make.
   *
   * @param battle The battle in which to make a choice.
   * @param player The player as whom to make the choice.
   */
  override fun decide(battle: Battle, player: Player): Choice {
    val choices = battle.choices(player)
    if (choices.isEmpty()) {
      throw IllegalArgumentException("Choice list was empty for $player")
    }
    return choices[random.nextInt(choices.size)]
  }

  /**
   * Choose which Pokemon to lead with at the start of a battle.
   *
   * There's no Battle object until a lead is chosen, so this has to be implemented as a separate
   * variable, unfortunately.
   *
   * @param black Black's team.
   * @param white White's team.
   * @param player Which player we are.
   * @return The index of the team member with which we will lead off.
   */
  override fun chooseLead(black: List<PokemonSpec>, white: List<PokemonSpec>, player: Player):
      Int {
    val us = when(player) {
      Player.BLACK -> black
      Player.WHITE -> white
    }
    return random.nextInt(us.size)
  }
}