package com.nrook.kadabra.ai.client


private val random: java.util.Random = java.util.Random()
private val logger = mu.KLogging().logger()

/**
 * Makes decisions. Will probably be an interface someday.
 */
class ClientAi {
  fun pickLead(): com.nrook.kadabra.proto.LeadChoice {
    return com.nrook.kadabra.proto.LeadChoice.newBuilder()
        .setLeadIndex(com.nrook.kadabra.ai.client.random.nextInt(6) + 1)
        .build()
  }

  fun pickAction(request: com.nrook.kadabra.proto.ActionRequest): com.nrook.kadabra.proto.ActionResponse {
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

      return com.nrook.kadabra.proto.ActionResponse.newBuilder()
          .setSwitch(com.nrook.kadabra.proto.SwitchSelection.newBuilder()
              .setIndex(indices[com.nrook.kadabra.ai.client.random.nextInt(indices.size)]))
          .build()
    } else {
      val legalMoves: List<Int> = (0 until request.moveCount)
          .filterNot { request.moveList[it].disabled }
          .toList()

      val moveIndex = legalMoves[com.nrook.kadabra.ai.client.random.nextInt(legalMoves.size)] + 1

      return com.nrook.kadabra.proto.ActionResponse.newBuilder()
          .setMove(
              com.nrook.kadabra.proto.MoveSelection.newBuilder()
                  .setIndex(moveIndex))
          .build()
    }
  }
}
