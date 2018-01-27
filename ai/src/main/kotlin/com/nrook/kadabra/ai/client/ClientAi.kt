package com.nrook.kadabra.ai.client

import com.nrook.kadabra.inference.parseLogLines
import com.nrook.kadabra.proto.ActionRequest
import com.nrook.kadabra.proto.ActionResponse
import com.nrook.kadabra.proto.LeadChoice
import com.nrook.kadabra.proto.MoveSelection
import com.nrook.kadabra.proto.SwitchSelection
import io.grpc.Status
import io.grpc.StatusRuntimeException


private val random: java.util.Random = java.util.Random()
private val logger = mu.KLogging().logger()

/**
 * Makes decisions. Will probably be an interface someday.
 */
class ClientAi {
  fun pickLead(): LeadChoice {
    return LeadChoice.newBuilder()
        .setLeadIndex(random.nextInt(6) + 1)
        .build()
  }

  fun pickAction(request: ActionRequest): ActionResponse {
    if (request.teamSpecList.isEmpty()) {
      throw StatusRuntimeException(
          Status.INVALID_ARGUMENT.withDescription("No team spec!"))
    }

    try {
      val lines = parseLogLines(request.logList)
      logger.info("Parsed ${lines.size} lines")
    } catch (e: Exception) {
      logger.error("Failed to parse logs", e)
    }

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
          .setMove(MoveSelection.newBuilder().setIndex(moveIndex))
          .build()
    }
  }
}
