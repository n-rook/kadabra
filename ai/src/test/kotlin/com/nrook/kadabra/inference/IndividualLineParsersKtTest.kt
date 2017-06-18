package com.nrook.kadabra.inference

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.arena.Player
import com.nrook.kadabra.proto.ReceivedMessage
import org.junit.Test

class IndividualLineParsersKtTest {

  fun buildReceivedMessage(className: String, vararg content: String): ReceivedMessage {
    return ReceivedMessage.newBuilder()
        .setClass_(className)
        .addAllContent(content.asIterable())
        .build()
  }

  fun <T: BattleEvent> parseEvent(
      expectedClass: Class<T>, className: String, vararg content: String): T {
    val message = buildReceivedMessage(className, *content)
    val event = parseLine(message)
    assertThat(event).isInstanceOf(expectedClass)
    return event as T
  }

  @Test
  fun parsePlayerEvent() {
    val playerEvent = parseEvent(PlayerEvent::class.java, "player", "p1", "myname", "1")
    assertThat(playerEvent.player).isEqualTo(Player.BLACK)
    assertThat(playerEvent.playerName).isEqualTo("myname")
  }

  @Test
  fun parseGameTypeEvent() {
    val gameTypeEvent = parseEvent(GameTypeEvent::class.java, "gametype", "singles")
    assertThat(gameTypeEvent.gameType).isEqualTo("singles")
  }

  @Test
  fun parseGenerationEvent() {
    val generationEvent = parseEvent(GenerationEvent::class.java, "gen", "7")
    assertThat(generationEvent.generation).isEqualTo(7)
  }

  @Test
  fun parseTierEvent() {
    val tierEvent = parseEvent(TierEvent::class.java, "tier", "[Gen 7] OU")
    assertThat(tierEvent.tier).isEqualTo("[Gen 7] OU")
  }

  @Test
  fun parseSeedEvent() {
    parseEvent(SeedEvent::class.java, "seed", "")
  }

  @Test
  fun parseClearPokeEvent() {
    parseEvent(ClearPokeEvent::class.java, "clearpoke")
  }

  @Test
  fun parsePokeEvent() {
    val pokeEvent = parseEvent(PokeEvent::class.java, "poke", "p2", "Scizor, F", "item")
    assertThat(pokeEvent.player).isEqualTo(Player.WHITE)
    assertThat(pokeEvent.details).isEqualTo("Scizor, F")
    assertThat(pokeEvent.item).isEqualTo("item")
  }

  @Test
  fun parseRuleEvent() {
    val sleepClause = parseEvent(RuleEvent::class.java, "rule",
        "Sleep Clause Mod: Limit one foe put to sleep")
    assertThat(sleepClause.rule).startsWith("Sleep Clause Mod")
  }

  @Test
  fun teamPreviewEvent() {
    parseEvent(TeamPreviewEvent::class.java, "teampreview")
  }

  @Test
  fun parseTeamPreviewChoiceEvent() {
    val teamPreviewChoice = parseEvent(
        ChoiceEvent::class.java, "choice", "team 4, team 1, team 2, team 3, team 5, team 6", "")
    assertThat(teamPreviewChoice.choice).isEqualTo("team 4, team 1, team 2, team 3, team 5, team 6")
  }

  @Test
  fun parseStartEvent() {
    parseEvent(StartEvent::class.java, "start")
  }

  @Test
  fun parseSwitchEventWithComplexDetails() {
    val event = parseEvent(
        SwitchEvent::class.java, "switch", "p2a: SkarioMario",
        "Skarmory, L95, M, shiny", "100/100")
    assertThat(event.player).isEqualTo(Player.WHITE)
    assertThat(event.identifier).isEqualTo(Nickname("SkarioMario"))
    assertThat(event.details.species).isEqualTo("Skarmory")
    assertThat(event.details.gender).isEqualTo(Gender.MALE)
    assertThat(event.details.level).isEqualTo(Level(95))
  }

  @Test
  fun parseSwitchEventWithFullHp() {
    val event = parseEvent(
        SwitchEvent::class.java, "switch",
        "p1a: Togekiss", "Togekiss, M", "374/374")
    assertThat(event.condition.maxHp).isEqualTo(374)
    assertThat(event.condition.hp).isEqualTo(374)
    assertThat(event.condition.status).isEqualTo(Status.OK)
  }

  @Test
  fun parseSwitchEventWithStatus() {
    val event = parseEvent(SwitchEvent::class.java, "switch",
        "p2a: SkarioMario", "Skarmory, L95, M, shiny", "95/100 par")
    assertThat(event.condition.hp).isEqualTo(95)
    assertThat(event.condition.maxHp).isEqualTo(100)
    assertThat(event.condition.status).isEqualTo(Status.PARALYZED)
  }
}