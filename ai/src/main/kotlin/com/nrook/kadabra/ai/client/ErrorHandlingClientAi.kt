package com.nrook.kadabra.ai.client

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.proto.ActionRequest
import com.nrook.kadabra.proto.ActionResponse
import com.nrook.kadabra.proto.LeadChoice

private val logger = mu.KLogging().logger()

/**
 * A composite client AI which resorts to a second option if the first option throws an error.
 */
class ErrorHandlingClientAi(private val ais: ImmutableList<ClientAi>): ClientAi {

  override fun pickLead(): LeadChoice {
    var lastError: Throwable? = null

    for (ai in ais) {
      try {
        return ai.pickLead()
      } catch (e: NotImplementedError) {
        logger.info("Using alternate AI to compute lead choice")
        lastError = e
      } catch (e: RuntimeException) {
        logger.error("Failed to compute lead choice", e)
      }
    }

    throw lastError ?: IllegalStateException("No AI supplied")
  }

  override fun pickAction(request: ActionRequest): ActionResponse {
    var lastError: Throwable? = null

    for (ai in ais) {
      try {
        return ai.pickAction(request)
      } catch (e: NotImplementedError) {
        logger.info("Using alternate AI to compute action")
        lastError = e
      } catch (e: RuntimeException) {
        logger.error("Failed to compute action", e)
      }
    }

    throw lastError ?: IllegalStateException("No AI supplied")
  }
}