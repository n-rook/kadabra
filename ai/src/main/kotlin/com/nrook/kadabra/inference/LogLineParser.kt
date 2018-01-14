package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.mechanics.arena.Player
import com.nrook.kadabra.proto.LogLine
import mu.KLogging

// Pokemon Showdown's battle event format is defined in PROTOCOL.md in the Showdown source code.

private val logger = KLogging().logger()

fun parseLogLines(logLines: List<LogLine>): List<BattleEvent> {
//  val us = identifyWhichPlayerIsUs(logLines)

  val events: ImmutableList.Builder<BattleEvent> = ImmutableList.builder()
  lines@ for (line in logLines) {
    when (line.lineCase) {
      LogLine.LineCase.RECEIVED -> {
        val received = line.received

        if (!isLineKnown(received)) {
          // When we parse most messages, we'll add a warning here.
          continue@lines
        }

        var parsedLine: BattleEvent?
        try {
          parsedLine = parseLine(received)
        } catch (e: Exception) {
          logger.warn("Could not parse received message\n${line}", e)
          parsedLine = null
        }

        if (parsedLine != null) {
          events.add(parsedLine)
        }
      }
      LogLine.LineCase.SENT -> {
        var parsedLine: BattleEvent?
        try {
          parsedLine = parseSentLine(line.sent)
        } catch (e: Exception) {
          logger.warn("Could not parse sent message\n${line}", e)
          parsedLine = null
        }

        if (parsedLine != null) {
          events.add(parsedLine)
        }
      }
      LogLine.LineCase.LINE_NOT_SET -> {
        logger.warn("Line not set")
      }
    }
  }

  return events.build()
}

/**
 * Identifies which player is us.
 *
 * Note that the player Showdown refers to as "p1" is the player we refer to as Black. Similarly,
 * "p2" is White.
 */
fun identifyWhichPlayerIsUs(logLines: List<LogLine>): Player {
  for (line in logLines) {
    if (line.lineCase == LogLine.LineCase.RECEIVED) {
      val received = line.received
      if (received.class_ == "request") {
        val request = deserializeRequest(received.contentList[0])
        if (request is TeamPreviewRequest) {
          return request.id
        }
      }
    }
  }

  throw IllegalArgumentException("Could not find team preview request in log lines.")
}