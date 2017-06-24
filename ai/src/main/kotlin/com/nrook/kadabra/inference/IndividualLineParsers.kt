package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.MoveId
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.proto.ReceivedMessage
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.full.findAnnotation

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
    throw IllegalArgumentException(
        debugMessage(line, "Line had $comparisonWord than $expectedLength elements"))
  }
}

private fun expectLength(line: ReceivedMessage, vararg expectedLengths: Int) {
  val count = line.contentCount
  if (!expectedLengths.contains(count)) {
    throw IllegalArgumentException(debugMessage(line, "Unexpected length $count"))
  }
}

private fun debugMessage(line: ReceivedMessage, reason: String): String {
  return "$reason\n${line.class_}: ${line.contentList.joinToString("|")}"
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

@EventParser("switch")
private fun parseSwitchEvent(line: ReceivedMessage): BattleEvent {
  val pokemonString = parsePokemonString(line.contentList[0])
  val details = parseDetails(line.contentList[1])
  val condition = parseConditionString(line.contentList[2])
  return SwitchEvent(pokemonString.player, pokemonString.name, details, condition)
}

@EventParser("drag")
private fun parseDragEvent(line: ReceivedMessage): BattleEvent {
  val pokemonString = parsePokemonString(line.contentList[0])
  val details = parseDetails(line.contentList[1])
  val condition = parseConditionString(line.contentList[2])
  return DragEvent(pokemonString.player, pokemonString.name, details, condition)
}

@EventParser("move")
private fun parseMoveEvent(line: ReceivedMessage): BattleEvent {
  val sourceString = parsePokemonString(line.contentList[0])
  val moveId = MoveId(line.contentList[1])
  val targetString = parsePokemonString(line.contentList[2])

  val tags = ArrayList(line.contentList.subList(3, line.contentList.size))
  val missed = tags.remove("[miss]")

  val fromTagResult = popFromTag(tags)
  return MoveEvent(
      sourceString, moveId, targetString, missed, fromTagResult.fromTag, ImmutableList.copyOf(tags))
}

@EventParser("detailschange")
private fun parseDetailsChangeEvent(line: ReceivedMessage): BattleEvent {
  return parseDetailsChangeOrFormeChange(line)
}

@EventParser("-formechange")
private fun parseFormeChangeEvent(line: ReceivedMessage): BattleEvent {
  return parseDetailsChangeOrFormeChange(line)
}

@EventParser("cant")
private fun parseCantEvent(line: ReceivedMessage): BattleEvent {
  expectLength(line, 2, 3)
  val move: MoveId? = if (line.contentList.size > 2) MoveId(line.contentList[2]) else null
  return CantEvent(
      parsePokemonString(line.contentList[0]),
      line.contentList[1],
      move)
}

@EventParser("faint")
private fun parseFaintEvent(line: ReceivedMessage): BattleEvent {
  expectLength(line, 1)
  return FaintEvent(parsePokemonString(line.contentList[0]))
}

@EventParser("-damage")
private fun parseDamageEvent(line: ReceivedMessage): BattleEvent {
  expectLength(line, 2, 3, 4)
  val tags = ArrayList(line.contentList.subList(2, line.contentList.size))
  val from = popFromTag(tags)
  if (!from.remainder.isEmpty()) {
    logger.warn(debugMessage(line, "Extra tags"))
  }

  return DamageEvent(
      parsePokemonString(line.contentList[0]),
      parseConditionString(line.contentList[1]),
      from.fromTag)
}

@EventParser("-heal")
private fun parseHealEvent(line: ReceivedMessage): BattleEvent {
  expectLength(line, 2, 3, 4)
  val tags = ArrayList(line.contentList.subList(2, line.contentList.size))
  val from = popFromTag(tags)
  if (!from.remainder.isEmpty()) {
    logger.warn(debugMessage(line, "Extra tags"))
  }

  return HealEvent(
      parsePokemonString(line.contentList[0]),
      parseConditionString(line.contentList[1]),
      from.fromTag)
}

@EventParser("turn")
private fun parseTurnEvent(line: ReceivedMessage): BattleEvent {
  expectLength(line, 1)
  return TurnEvent(line.contentList[0].toInt())
}

private fun parseDetailsChangeOrFormeChange(line: ReceivedMessage): BattleEvent {
  expectLength(line, 2, 3)
  val permanent = line.class_ == "detailschange"
  val condition = if (line.contentList.size == 3) parseConditionString(line.contentList[2])
    else null

  return DetailsChangeEvent(
      permanent,
      parsePokemonString(line.contentList[0]),
      parseDetails(line.contentList[1]),
      condition)
}

private val POKEMON_STRING_REGEX = Regex("(p[12])([a-z]): (.*)")

/**
 * Parses the identifier string used to identify a Pokemon.
 *
 * These strings typically look like "p1a: PokemonNickname".
 */
private fun parsePokemonString(pokemon: String): PokemonIdentifier {
  val result = POKEMON_STRING_REGEX.matchEntire(pokemon)
      ?: throw IllegalArgumentException("Could not parse Pokemon string: \"$pokemon\"")

  val player = ID_TO_PLAYER_CONVERTER.convert(result.groupValues[1])!!
  if (result.groupValues[2] != "a") {
    throw IllegalArgumentException(
        "We can't handle doubles yet, " +
            "but the location for this Pokemon was ${result.groupValues[2]}")
  }
  return PokemonIdentifier(player, Nickname(result.groupValues[3]))
}

private var LEVEL_REGEX = Regex("L([0-9]+)")
private var GENDER_REGEX = Regex("[MF]")
private fun parseDetails(details: String): PokemonDetails {
  // Example string: "Skarmory, L95, M, shiny"
  // Each part is omitted if not applicable.

  val splitDetails = details.split(", ")
  // We append a placeholder suffix to make some of this logic easier.
  val splitDetailsWithSuffix = splitDetails.plus("")
  val species = splitDetailsWithSuffix[0]
  val remainder1 = splitDetailsWithSuffix.drop(1)

  val level: Level
  val remainder2: List<String>
  val levelRegexMatch = LEVEL_REGEX.find(remainder1[0])
  if (levelRegexMatch != null) {
    level = Level(levelRegexMatch.groupValues[1].toInt())
    remainder2 = remainder1.drop(1)
  } else {
    level = Level(100)
    remainder2 = remainder1
  }

  val genderCode = GENDER_REGEX.find(remainder2[0])?.value
  val gender: Gender
  val remainder3: List<String>
  if (genderCode == null) {
    gender = Gender.GENDERLESS
    remainder3 = remainder2
  } else {
    gender = if(genderCode == "M") Gender.MALE else Gender.FEMALE
    remainder3 = remainder2.drop(1)
  }

  val shiny = remainder3[0] == "shiny"

  return PokemonDetails(species, shiny, gender, level)
}

private val CONDITION_PARSER_REGEX = Regex("""(\d+)/(\d+) ?(\w*)""")

/**
 * Parses a condition string, like "85/100 par".
 */
private fun parseConditionString(condition: String): VisibleCondition {
  if (!condition.contains("/")) {
    // Fainted Pokemon are often reported as having the condition "0 fnt".
    // The spec doesn't require the "fnt", but it's always there.
    return CONDITION_FAINTED
  }

  val matchResult = CONDITION_PARSER_REGEX.matchEntire(condition)
    ?: throw IllegalArgumentException("Could not parse condition string $condition")

  val hp = matchResult.groupValues[1].toInt()
  val maxHp = matchResult.groupValues[2].toInt()
  val status = parseStatusString(matchResult.groupValues[3])
  return VisibleCondition(hp, maxHp, status)
}

/**
 * Parses a status string, like "par", into a Status object.
 */
private fun parseStatusString(status: String): Status {
  return when (status) {
    "" -> Status.OK
    "par" -> Status.PARALYZED
    else -> throw IllegalArgumentException("Unknown status $status")
  }
}

private data class PopFromTagResult(val fromTag: FromTag?, val remainder: List<String>)
private fun popFromTag(tags: List<String>): PopFromTagResult {
  val indexOfFrom = tags.indexOfFirst { it.startsWith("[from]") }
  if (indexOfFrom == -1) {
    return PopFromTagResult(null, tags)
  }

  val returnList = ArrayList(tags)
  val fromString = returnList.removeAt(indexOfFrom)
  val parsedFromString = fromString.removePrefix("[from]").trim()

  val ofSource: PokemonIdentifier?
  if (returnList.size > indexOfFrom && returnList[indexOfFrom].startsWith("[of] ")) {
    val ofString = returnList.removeAt(indexOfFrom).removePrefix("[of] ")
    ofSource = parsePokemonString(ofString)
  } else {
    ofSource = null
  }
  return PopFromTagResult(
      FromTag(parsedFromString, ofSource), returnList)
}

// The following events are intentionally not parsed:
// "swap": Only happens in doubles
//
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
    ::parseSwitchEvent,
    ::parseDragEvent,
    ::parseMoveEvent,
    ::parseDetailsChangeEvent,
    ::parseFormeChangeEvent,
    ::parseCantEvent,
    ::parseFaintEvent,
    ::parseTurnEvent,
    ::parseDamageEvent,
    ::parseHealEvent,
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