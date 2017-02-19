package com.nrook.kadabra.ai

import com.nrook.kadabra.proto.*
import mu.KLogging
import java.util.Random


private val random: Random = Random()
private val logger = KLogging().logger()

/**
 * Makes decisions. Will probably be an interface someday.
 */
class Ai {
  fun pickLead(): LeadChoice {
    return LeadChoice.newBuilder()
        .setLeadIndex(random.nextInt(6) + 1)
        .build()
  }

  fun pickAction(request: ActionRequest): ActionResponse {
    if (request.forceSwitch) {
      val team = request.sideInfo.teamList

      val indices: MutableList<Int> = mutableListOf()
      for (i: Int in team.indices) {
        val index = i + 1
        val pokemon = team[i]

        if (!pokemon.fainted) {
          indices.add(index)
        }
      }

      if (indices.size == 0) {
        throw IllegalArgumentException("All these Pokemon are fainted! What's going on?")
      }

      return ActionResponse.newBuilder()
          .setSwitch(SwitchSelection.newBuilder()
              .setIndex(indices[random.nextInt(indices.size)]))
          .build()
    } else {
      val legalMoves: List<Int> = (0 until request.moveCount)
          .filterNot { request.moveList[it].disabled }
          .toList()

      val moveIndex = legalMoves[random.nextInt(legalMoves.size)] + 1

      return ActionResponse.newBuilder()
          .setMove(
              MoveSelection.newBuilder()
                  .setIndex(moveIndex))
          .build()
    }
  }
}
