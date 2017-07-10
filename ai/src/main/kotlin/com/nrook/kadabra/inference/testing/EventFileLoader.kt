package com.nrook.kadabra.inference.testing

import com.google.common.collect.ImmutableList
import com.google.common.io.Resources
import com.nrook.kadabra.inference.BattleEvent
import com.nrook.kadabra.inference.parseLogLines
import com.nrook.kadabra.proto.LogLine
import com.nrook.kadabra.proto.ReceivedMessage
import java.nio.charset.Charset

fun loadEventsFromResource(resourceName: String): List<BattleEvent> {
  return loadEventsFromFile(Resources.readLines(Resources.getResource(resourceName),
      Charset.forName("UTF-8")))
}

fun loadEventsFromFile(lines: Iterable<String>): List<BattleEvent> {
  return parseLogLines(load(lines))
}

/**
 * Load a list of events from a file.
 *
 * TODO: Handle sent messages. They look like this:
 * client.js?f8eb228d:761 >> battle-gen7ou-12|/team 523416|2
 */
private fun load(lines: Iterable<String>): ImmutableList<LogLine> {
  val builder: ImmutableList.Builder<LogLine> = ImmutableList.builder()
  for (line in lines) {
    if (line.isEmpty() || line[0] != '|') {
      continue
    }

    var splitLine = line.split('|')
    splitLine = splitLine.subList(1, splitLine.size)
    if (splitLine.isNotEmpty() && splitLine[splitLine.size - 1].isEmpty()) {
      // Some events just have an empty part at the end for some reason.
      splitLine = splitLine.subList(0, splitLine.size - 1)
    }

    if (splitLine.isEmpty()) {
      throw IllegalArgumentException("Could not read line $line")
    }
    builder.add(
        LogLine.newBuilder()
            .setReceived(
                ReceivedMessage.newBuilder()
                    .setClass_(splitLine[0])
                    .addAllContent(splitLine.drop(1)))
            .build())
  }

  return builder.build()
}