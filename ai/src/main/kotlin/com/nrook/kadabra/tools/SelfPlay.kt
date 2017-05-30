package com.nrook.kadabra.tools

import com.nrook.kadabra.ai.framework.runToCompletion
import com.nrook.kadabra.ai.framework.runToTurnLimit
import com.nrook.kadabra.ai.perfect.Ai
import com.nrook.kadabra.ai.perfect.MixedStrategyAiWrapper
import com.nrook.kadabra.ai.perfect.MonteCarloAi
import com.nrook.kadabra.ai.perfect.RandomAi
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.arena.*
import com.nrook.kadabra.mechanics.rng.REALISTIC_RANDOM_POLICY
import com.nrook.kadabra.mechanics.rng.RandomNumberGenerator
import com.nrook.kadabra.teambuilder.TeamPickingStrategy
import com.nrook.kadabra.teambuilder.UsageDatasetTeamPicker
import com.nrook.kadabra.usage.getOuUsageDataset
import java.util.*

private val logger = mu.KLogging().logger()


/**
 * Play a full game of Pokemon using two AIs and random OU teams.
 */
fun selfPlay(blackAi: Ai, whiteAi: Ai, context: BattleContext): Player? {
  val dataset = getOuUsageDataset()
  val pokedex = getGen7Pokedex()

  val teamPicker = UsageDatasetTeamPicker.create(Random(), dataset, 0.005)
  val teams = (0..1).map { pickTeam(teamPicker, pokedex) }

  val blackLeadIndex = blackAi.chooseLead(teams[0], teams[1], Player.BLACK)
  val whiteLeadIndex = whiteAi.chooseLead(teams[0], teams[1], Player.WHITE)

  val battle = startBattle(teams[0], blackLeadIndex, teams[1], whiteLeadIndex, context)
  return runToTurnLimit(battle, blackAi, whiteAi, 100, context)
}

/**
 * Play a full game of Pokemon using two AIs and random OU teams.
 */
fun playOneGame(blackAi: Ai, whiteAi: Ai, context: BattleContext) {
  logger.info("And the winner is: ${selfPlay(blackAi, whiteAi, context)}")
}

/**
 * Repeatedly make two AIs fight each other, then return the first one's winrate.
 */
fun versus(ai1: Ai, ai2: Ai, context: BattleContext, reps: Int): Double {
  var wins = 0
  var games = 0
  while (games < reps) {
    try {
      val result = selfPlay(ai1, ai2, context)
      if (result == Player.BLACK) {
        wins++
      }
      if (result != null) {
        games++
      } else {
        logger.info("Canceled sim; ran out of turns to play.")
      }
    } catch (e: Exception) {
      logger.error("Failed to self-play", e)
    }
  }

  logger.info("The first AI won $wins out of $reps games.")
  return wins.toDouble() / reps
}

private fun pickTeam(
    picker: TeamPickingStrategy, pokedex: Pokedex):
    List<PokemonSpec> {
  val definitions = picker.pick()
  return definitions.map { PokemonSpec.createFromPokemonDefinition(it, pokedex) }
}

fun main(args: Array<String>) {
  val random = Random()

  val blackAi = MixedStrategyAiWrapper(MonteCarloAi(1000), random)
  val whiteAi = MixedStrategyAiWrapper(MonteCarloAi(10000), random)

  val context = BattleContext(RandomNumberGenerator(REALISTIC_RANDOM_POLICY, random), NoOpLogger())
  versus(blackAi, whiteAi, context, 100)
}
