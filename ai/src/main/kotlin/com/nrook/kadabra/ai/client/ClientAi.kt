package com.nrook.kadabra.ai.client

import com.nrook.kadabra.proto.ActionRequest
import com.nrook.kadabra.proto.ActionResponse
import com.nrook.kadabra.proto.LeadChoice

/**
 * A decision-making client AI.
 */
interface ClientAi {

  /**
   * Pick the lead to use before the battle proper starts.
   */
  fun pickLead(): LeadChoice

  /**
   * Pick what to do.
   */
  fun pickAction(request: ActionRequest): ActionResponse
}