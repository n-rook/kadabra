package com.nrook.kadabra.ai.client

import com.nrook.kadabra.ai.perfect.Ai
import com.nrook.kadabra.inference.BattleLoader
import com.nrook.kadabra.inference.constructBattle
import com.nrook.kadabra.inference.parseLogLines
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.TeamPokemon
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.arena.Choice
import com.nrook.kadabra.mechanics.arena.MoveChoice
import com.nrook.kadabra.mechanics.arena.SwitchChoice
import com.nrook.kadabra.proto.ActionRequest
import com.nrook.kadabra.proto.ActionResponse
import com.nrook.kadabra.proto.LeadChoice
import com.nrook.kadabra.proto.SideInfo
import com.nrook.kadabra.usage.UsageDataset

private val logger = mu.KLogging().logger()

private fun <T, V> findOrThrow(list: List<T>, target: V, transform: (T) -> V): T {
  return list.find { transform(it) == target } ?:
      throw RuntimeException("Could not find $target in $list")
}

private fun buildActionResponseFromChoice(
    choice: Choice, team: List<PokemonSpec>, sideInfo: SideInfo):
    ActionResponse {
  // The Showdown protocol selects moves and switch targets by index,
  // but we don't hold onto the index in the Battle type,
  // so we need the sideInfo to know who our real target actually is.

  val responseBuilder = ActionResponse.newBuilder()
  when (choice) {
    is MoveChoice -> {
      val activePokemonInfo = sideInfo.teamList.find { it.active } ?:
          throw IllegalStateException("Move choice cannot be made with no active Pokemon\n" +
              sideInfo.toString())
      // I suspect this doesn't work
      val activePokemon = team.find { it.species.name == activePokemonInfo.species } ?:
          throw IllegalStateException(
              "Could not find team member with species " +
                  "${activePokemonInfo.species}; only species are " +
                  team.map { it.species }.joinToString(", "))
      val moveIndex = activePokemon.moves.indexOf(choice.move)
      if (moveIndex == -1) {
        throw IllegalStateException("Could not find move ${choice.move.id}; only moves are " +
            activePokemon.moves.map { it.id }.joinToString(", "))
      }
      responseBuilder.moveBuilder.index = moveIndex
    }
    is SwitchChoice -> {
      val switchTarget = team.find { it.species.id == choice.target } ?:
          throw IllegalStateException("Could not find switch target with species " +
              "${choice.target}; only choices were " +
              team.map { it.species }.joinToString { ", " })
      val switchTargetInfo =
          findOrThrow(sideInfo.teamList, switchTarget.species.name, { it.species })
      val switchTargetIndex = sideInfo.teamList.indexOf(switchTargetInfo)

      if (switchTargetIndex == -1) {
        throw AssertionError("impossible")
      }
      if (switchTargetInfo.active) {
        throw IllegalStateException("Cannot switch to active Pokemon")
      }
      if (switchTargetInfo.fainted) {
        throw IllegalStateException("Cannot switch to fainted Pokemon")
      }

      responseBuilder.switchBuilder.index = switchTargetIndex + 1
    }
    else -> {
      throw IllegalStateException("Unknown choice type $choice")
    }
  }
  return responseBuilder.build()
}

class RealClientAi(
    private val pokedex: Pokedex,
    private val dataset: UsageDataset,
    private val battleLoader: BattleLoader,
    private val ai: Ai): ClientAi {

  override fun pickLead(): LeadChoice {
    throw NotImplementedError()
  }

  override fun pickAction(request: ActionRequest): ActionResponse {
    // Uh, we need our PokemonSpecs!
    val ourTeam = request.teamSpecList
        .map { TeamPokemon.fromSpecProto(pokedex, it).toSpec(null) }

    val events = parseLogLines(request.logList)

    val info = battleLoader.parseBattle(ourTeam, events)

    val battle = constructBattle(
        info,
        events,
        pokedex,
        dataset)

    val decision = ai.decide(battle, info.us)

    return buildActionResponseFromChoice(decision, ourTeam, request.sideInfo)
  }
}
