package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.nrook.kadabra.proto.ReceivedMessage
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.reflect

/**
 * An annotation which associates keywords
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventParser(val className: String)

private fun expectEmpty(line: ReceivedMessage) {
  expectLength(line, 0)
}

private fun expectLength(line: ReceivedMessage, expectedLength: Int) {
  val count = line.contentCount
  if (count != expectedLength) {
    val comparisonWord = if (count < expectedLength) "fewer" else "more"
    throw IllegalArgumentException("" +
        "Line of type ${line.class_} had $comparisonWord than $expectedLength elements:\n" +
        line.contentList.joinToString("|"))
  }
}

@EventParser("player")
private fun parsePlayerEvent(line: ReceivedMessage): BattleEvent {
  // Player event: [playerId] [playerName] [avatarCode]
  // We ignore avatarCode, since as a bot we don't care about avatars.
  return PlayerEvent(ID_TO_PLAYER_CONVERTER.convert(line.contentList[0])!!, line.contentList[1])
}

@EventParser("gametype")
private fun parseGameTypeEvent(line: ReceivedMessage): BattleEvent {
  return GameTypeEvent(line.contentList[0])
}

@EventParser("gen")
private fun parseGenerationEvent(line: ReceivedMessage): BattleEvent {
  return GenerationEvent(line.contentList[0].toInt())
}

@EventParser("tier")
private fun parseTierEvent(line: ReceivedMessage): BattleEvent {
  return TierEvent(line.contentList[0])
}

@EventParser("seed")
private fun parseSeedEvent(line: ReceivedMessage): BattleEvent {
  return SeedEvent.INSTANCE
}

@EventParser("clearpoke")
private fun parseClearPokeEvent(line: ReceivedMessage): BattleEvent {
  expectEmpty(line)
  return ClearPokeEvent.INSTANCE
}

@EventParser("poke")
private fun parsePokeEvent(line: ReceivedMessage): BattleEvent {
  expectLength(line, 3)
  return PokeEvent(
      ID_TO_PLAYER_CONVERTER.convert(line.contentList[0])!!,
      line.contentList[1],
      line.contentList[2]
  )
}

@EventParser("rule")
private fun parseRuleEvent(line: ReceivedMessage): BattleEvent {
  expectLength(line, 1)
  return RuleEvent(line.contentList[0])
}

@EventParser("teampreview")
private fun parseTeamPreviewEvent(line: ReceivedMessage): BattleEvent {
  expectEmpty(line)
  return TeamPreviewEvent.INSTANCE
}

@EventParser("choice")
private fun parseChoiceEvent(line: ReceivedMessage): BattleEvent {
  // FOr some reason, choice events always have a tailing "".
  expectLength(line, 2)
  return ChoiceEvent(line.contentList[0])
}

@EventParser("start")
private fun parseStartEvent(line: ReceivedMessage): BattleEvent {
  return StartEvent.INSTANCE
}

private val PARSERS: ImmutableList<(ReceivedMessage) -> BattleEvent> = ImmutableList.of(
    ::parsePlayerEvent,
    ::parseGameTypeEvent,
    ::parseGenerationEvent,
    ::parseTierEvent,
    ::parseSeedEvent,
    ::parseClearPokeEvent,
    ::parsePokeEvent,
    ::parseRuleEvent,
    ::parseTeamPreviewEvent,
    ::parseStartEvent,
    ::parseChoiceEvent
)

private fun getLineParsers(): ImmutableMap<String, (ReceivedMessage) -> BattleEvent> {
  val builder: ImmutableMap.Builder<String, (ReceivedMessage) -> BattleEvent> =
      ImmutableMap.builder()
  for (parser in PARSERS) {
    val className = (parser as CallableReference).findAnnotation<EventParser>()?.className!!
    builder.put(className, parser)
  }
  return builder.build()
}

private val LINE_PARSERS by lazy { getLineParsers() }

/**
 * Parses a received message and returns the resulting [BattleEvent]
 *
 * @param line The received message.
 * @return The parsed event.
 */
fun parseLine(line: ReceivedMessage): BattleEvent {
  val parser = LINE_PARSERS[line.class_]
  if (parser == null) {
    throw IllegalArgumentException("Failed to parse line of class ${line.class_}")
  } else {
    return parser(line)
  }
}