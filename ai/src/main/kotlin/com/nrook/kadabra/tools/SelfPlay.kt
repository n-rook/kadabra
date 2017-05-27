package com.nrook.kadabra.tools

import com.nrook.kadabra.ai.framework.runToCompletion
import com.nrook.kadabra.ai.perfect.RandomAi
import com.nrook.kadabra.info.*
import com.nrook.kadabra.info.read.getGen7Movedex
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.info.read.getGen7Species
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.arena.BattleContext
import com.nrook.kadabra.mechanics.arena.Player
import com.nrook.kadabra.mechanics.arena.debugLogger
import com.nrook.kadabra.mechanics.arena.startBattle
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
fun selfPlay(blackAi: RandomAi, whiteAi: RandomAi, context: BattleContext) {
  val dataset = getOuUsageDataset()
  val pokedex = getGen7Pokedex()

  val teamPicker = UsageDatasetTeamPicker.create(Random(), dataset, 0.005)
  val teams = (0..1).map { pickTeam(teamPicker, pokedex) }

  val blackLeadIndex = blackAi.chooseLead(teams[0], teams[1], Player.BLACK)
  val whiteLeadIndex = whiteAi.chooseLead(teams[0], teams[1], Player.WHITE)

  val battle = startBattle(teams[0], blackLeadIndex, teams[1], whiteLeadIndex, context)
  val winner = runToCompletion(battle, blackAi, whiteAi, context)
  logger.info("And the winner is: $winner")
}

private fun pickTeam(
    picker: TeamPickingStrategy, pokedex: Pokedex):
    List<PokemonSpec> {
  val definitions = picker.pick()
  return definitions.map { PokemonSpec.createFromPokemonDefinition(it, pokedex) }
}

fun main(args: Array<String>) {
  val random = Random()

  val blackAi = RandomAi(random)
  val whiteAi = RandomAi(random)

  val context = BattleContext(RandomNumberGenerator(REALISTIC_RANDOM_POLICY, random), debugLogger())

  selfPlay(blackAi, whiteAi, context)
}
