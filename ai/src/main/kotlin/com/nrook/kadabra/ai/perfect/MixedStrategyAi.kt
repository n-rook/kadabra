package com.nrook.kadabra.ai.perfect

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import com.nrook.kadabra.common.RandomBasket
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.arena.Battle
import com.nrook.kadabra.mechanics.arena.Choice
import com.nrook.kadabra.mechanics.arena.Player
import mu.KLogging
import java.util.*

private val logger = KLogging().logger()

/**
 * A wrapper which converts a [MixedStrategyAi] into an [Ai] by picking a strategy at random.
 */
class MixedStrategyAiWrapper(
    private val mixedStrategyAi: MixedStrategyAi,
    private val random: Random): Ai {

  override fun decide(battle: Battle, player: Player): Choice {
    return mixedStrategyAi.decide(battle, player).pickOne(random)
  }

  override fun chooseLead(black: List<PokemonSpec>, white: List<PokemonSpec>, player: Player): Int {
    return mixedStrategyAi.chooseLead(black, white, player).pickOne(random)
  }
}

/**
 * Describes an AI which implements a mixed strategy.
 *
 * This interface is identical to [Ai], except that its methods return a set of possible choices.
 */
interface MixedStrategyAi {

  /**
   * @see [Ai.decide]
   */
  fun decide(battle: Battle, player: Player): MixedStrategy<Choice>

  /**
   * @see [Ai.chooseLead]
   */
  fun chooseLead(black: List<PokemonSpec>, white: List<PokemonSpec>, player: Player):
      MixedStrategy<Int>
}

/**
 * Represents a mixed strategy for a game position.
 */
data class MixedStrategy<T>(val choices: ImmutableMap<T, Double>) {
  init {
    val sum = choices.values.sum()
    if (sum < 0.9 || sum > 1.1) {
      logger.warn("Mixed strategy sums to total outside expected bounds: %f", sum)
    }
  }

  companion object Factory {

    /**
     * Returns a MixedStrategy which chooses evenly between the given choices.
     *
     * @param choices The choices to be included in the strategy.
     */
    fun <T> createEvenStrategy(choices: List<T>): MixedStrategy<T> {
      val weight = 1.0 / choices.size
      return MixedStrategy(Maps.toMap(choices, {weight}))
    }
  }

  /**
   * Pick a random element from this basket according to the rules.
   */
  fun pickOne(random: Random): T {
    return RandomBasket.create(random, choices.keys, {choices[it]!!}, 0.0).pick()
  }
}
