package com.nrook.kadabra.ai

import com.nrook.kadabra.proto.ActionResponse
import com.nrook.kadabra.proto.LeadChoice
import com.nrook.kadabra.proto.MoveSelection
import java.util.Random


private val random: Random = Random()

/**
 * Makes decisions. Will probably be an interface someday.
 */
class Ai {
  fun pickLead(): LeadChoice {
    return LeadChoice.newBuilder()
        .setLeadIndex(random.nextInt(6) + 1)
        .build()
  }

  fun pickStartOfTurnAction(): ActionResponse {
    return ActionResponse.newBuilder()
        .setMove(
            MoveSelection.newBuilder()
                .setIndex(1)
        )
        .build()
  }
}
