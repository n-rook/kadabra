package com.nrook.kadabra.ai.client

import com.nrook.kadabra.inference.BattleLoader
import com.nrook.kadabra.inference.OngoingBattle
import com.nrook.kadabra.proto.ActionRequest
import com.nrook.kadabra.proto.ActionResponse
import com.nrook.kadabra.proto.LeadChoice

private val random: java.util.Random = java.util.Random()
private val logger = mu.KLogging().logger()

// TODO: Use a wrapper class which takes a random action if we hit an error
class RealClientAi(
    private val battleLoader: BattleLoader): ClientAi {

  override fun pickLead(): LeadChoice {
    return LeadChoice.newBuilder()
        .setLeadIndex(random.nextInt(6) + 1)
        .build()
  }

  override fun pickAction(request: ActionRequest): ActionResponse {
    // Uh, we need our PokemonSpecs!

    val info = battleLoader.parseBattle()
  }
}
