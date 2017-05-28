package com.nrook.kadabra.ai.perfect

import com.google.common.collect.*
import com.nrook.kadabra.ai.framework.runToCompletion
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.arena.*
import com.nrook.kadabra.mechanics.rng.REALISTIC_RANDOM_POLICY
import com.nrook.kadabra.mechanics.rng.RandomNumberGenerator
import java.util.*

val SIMULATION_CONTEXT = BattleContext(
    RandomNumberGenerator(REALISTIC_RANDOM_POLICY, Random()),
    NoOpLogger())

/**
 * A naive Monte Carlo AI.
 *
 * @param playouts The maximum number of playouts to run per decision.
 */
class MonteCarloAi(private val playouts: Int): MixedStrategyAi {
  override fun decide(battle: Battle, player: Player): MixedStrategy<Choice> {
    // TODO: There's a severe problem here: this can't easily handle the case where only one player
    // has an actual choice to make.
    val expectedOutcomes = computeExpectedOutcomesMatrix(battle, playouts)
    return findStrategyFromExpectedOutcomes(expectedOutcomes, player)
  }

  override fun chooseLead(black: List<PokemonSpec>, white: List<PokemonSpec>, player: Player):
      MixedStrategy<Int> {
    // TODO: an actual strategy here would be nice
    val us = when (player) {
      Player.BLACK -> black
      Player.WHITE -> white
    }
    return MixedStrategy(Maps.toMap(0 until us.size, {1.0 / us.size}))
  }
}

/**
 * Compute the expected outcomes matrix for a given position.
 *
 * @return a dense Table whose rows are Black's possible choices and whose columns are White's
 *    possible choices. The values of each cell are a double from 0 to 1, representing the chance
 *    of Black winning given these two choices.
 */
fun computeExpectedOutcomesMatrix(battle: Battle, playouts: Int): Table<Choice, Choice, Double> {
  val randomAi = RandomAi(Random())

  val rows = battle.choices(Player.BLACK)
  val columns = battle.choices(Player.WHITE)

  val perCellPlayouts = playouts / (rows.size * columns.size)
  val table: ArrayTable<Choice, Choice, Double> = ArrayTable.create(rows, columns)
  for (row in rows) {
    for (column in columns) {
      table.put(row, column, runNSimulations(battle, row, column, perCellPlayouts, randomAi))
    }
  }

  return table
}

private fun runNSimulations(
    battle: Battle, blackChoice: Choice, whiteChoice: Choice, n: Int, ai: Ai): Double {
  val blackWins = (0 until n)
      .map { runOneSimulationWithGivenChoices(battle, blackChoice, whiteChoice, ai) }
      .count { it === Player.BLACK }
  return (blackWins.toDouble()) / n
}

private fun runOneSimulationWithGivenChoices(
    battle: Battle, blackChoice: Choice, whiteChoice: Choice, ai: Ai): Player {
  val nextBattle = simulateBattle(battle, SIMULATION_CONTEXT, blackChoice, whiteChoice)
  if (nextBattle.winner() != null) {
    return nextBattle.winner()!!
  }
  return runToCompletion(nextBattle, ai, ai, SIMULATION_CONTEXT)
}

/**
 * Given a table of expected outcomes for each choice, determine the strategy to play.
 */
fun findStrategyFromExpectedOutcomes(outcomes: Table<Choice, Choice, Double>, player: Player):
    MixedStrategy<Choice> {

  // Construct a table of outcomes where our choices are rows, and theirs are columns.
  val ourOutcomes = when (player) {
    Player.BLACK -> outcomes
    Player.WHITE -> Tables.transpose(outcomes)
  }

  // TODO: This is basically placeholder logic.
  // Unfortunately, it appears to be nontrivial to go from a table of expected outcomes to an
  // optimal mixed strategy. This logic, which just finds the best strategy assuming the opponent
  // is a moron who plays a mixed strategy where they play every choice evenly, is highly suspect,
  // so we must revisit it soon.

  var bestOutcome = 0.0
  val bestChoices = mutableListOf<Choice>()
  for (choice in ourOutcomes.rowKeySet()) {
    val averageOutcome = ourOutcomes.row(choice)
        .values
        .average()
    if (averageOutcome > bestOutcome) {
      bestOutcome = averageOutcome
      bestChoices.clear()
      bestChoices.add(choice)
    } else if (averageOutcome == bestOutcome) {
      bestChoices.add(choice)
    }
  }

  return MixedStrategy.createEvenStrategy(bestChoices)
}