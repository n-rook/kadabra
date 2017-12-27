package com.nrook.kadabra.inference

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.mechanics.Condition
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
    assertThat(pokeEvent.details.species).isEqualTo("Scizor")
    assertThat(pokeEvent.details.gender).isEqualTo(Gender.FEMALE)
    assertThat(pokeEvent.item).isEqualTo("item")
  }

  @Test
  fun parsePokeNoItemEvent() {
    val pokeEvent = parseEvent(PokeEvent::class.java, "poke", "p2", "Scizor, F")
    assertThat(pokeEvent.player).isEqualTo(Player.WHITE)
    assertThat(pokeEvent.details.species).isEqualTo("Scizor")
    assertThat(pokeEvent.details.gender).isEqualTo(Gender.FEMALE)
    assertThat(pokeEvent.item).isNull()
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
    assertThat(event.condition.status).isEqualTo(Condition.OK)
  }

  @Test
  fun parseSwitchEventWithCondition() {
    val event = parseEvent(SwitchEvent::class.java, "switch",
        "p2a: SkarioMario", "Skarmory, L95, M, shiny", "95/100 par")
    assertThat(event.condition.hp).isEqualTo(95)
    assertThat(event.condition.maxHp).isEqualTo(100)
    assertThat(event.condition.status).isEqualTo(Condition.PARALYSIS)
  }

  @Test
  fun parseDragEvent() {
    val event = parseEvent(
        DragEvent::class.java, "drag",
        "p1a: Togekiss", "Togekiss, M", "374/374")
    assertThat(event.player).isEqualTo(Player.BLACK)
    assertThat(event.identifier).isEqualTo(Nickname("Togekiss"))
    assertThat(event.details.species).isEqualTo("Togekiss")
    assertThat(event.details.gender).isEqualTo(Gender.MALE)
    assertThat(event.details.level).isEqualTo(Level(100))
  }

  @Test
  fun parseMove() {
    val event = parseEvent(
        MoveEvent::class.java, "move", "p2a: Skarmory", "Whirlwind", "p1a: Garchomp")
    assertThat(event.source).isEqualTo(PokemonIdentifier(Player.WHITE, Nickname("Skarmory")))
    assertThat(event.move).isEqualTo("Whirlwind")
    assertThat(event.target).isEqualTo(PokemonIdentifier(Player.BLACK, Nickname("Garchomp")))
    assertThat(event.miss).isFalse()
    assertThat(event.from).isNull()
    assertThat(event.unparsedTags).isEmpty()
  }

  @Test
  fun parseLockedInMove() {
    val event = parseEvent(
        MoveEvent::class.java, "move", "p1a: Garchomp", "Outrage", "p2a: Skarmory",
        "[from]lockedmove")
    assertThat(event.from!!.from).isEqualTo("lockedmove")
    assertThat(event.from!!.source).isNull()
  }

  @Test
  fun parseMissedMove() {
    val event = parseEvent(
        MoveEvent::class.java, "move", "p1a: Gengar", "Focus Blast", "p2a: Skarmory", "[miss]")
    assertThat(event.miss).isTrue()
  }

  @Test
  fun parseOtherTags() {
    // This is a guess at what a move with a bunch of weird alternate tags would look like
    val event = parseEvent(
        MoveEvent::class.java, "move", "p1a: Gengar", "Shadow Ball", "p2a: Skarmory",
        "[anim] Focus Blast", "[silent]")
    assertThat(event.unparsedTags).containsExactly("[anim] Focus Blast", "[silent]").inOrder()
  }

  @Test
  fun megaEvolve() {
    val event = parseEvent(
        DetailsChangeEvent::class.java, "detailschange", "p1a: Alakazam", "Alakazam-Mega, F")
    assertThat(event.pokemon).isEqualTo(PokemonIdentifier(Player.BLACK, Nickname("Alakazam")))
    assertThat(event.condition).isNull()
    assertThat(event.permanent).isTrue()
    assertThat(event.newDetails.species).isEqualTo("Alakazam-Mega")
    assertThat(event.newDetails.gender).isEqualTo(Gender.FEMALE)


    // TODO: When we implement -mega, put this in:
    // parseEvent(MegaEvent::class.java, "-mega", "p1a: Alakazam", "Alakazam", "Alakazite")
  }

  @Test
  fun faint() {
    val event = parseEvent(FaintEvent::class.java, "faint", "p2a: Skarmory")
    assertThat(event.pokemon).isEqualTo(PokemonIdentifier(Player.WHITE, Nickname("Skarmory")))
  }

  @Test
  fun damageEvent() {
    val event = parseEvent(
        DamageEvent::class.java, "-damage", "p1a: Gengar", "79/100")
    assertThat(event.pokemon).isEqualTo(PokemonIdentifier(Player.BLACK, Nickname("Gengar")))
    assertThat(event.newCondition).isEqualTo(VisibleCondition(79, 100, Condition.OK))
    assertThat(event.from).isNull()
  }

  @Test
  fun damageFromRockyHelmet() {
    val event = parseEvent(
        DamageEvent::class.java,
        "-damage",
        "p2a: Scizor",
        "286/343",
        "[from] item: Rocky Helmet",
        "[of] p1a: Gengar")
    assertThat(event.pokemon).isEqualTo(PokemonIdentifier(Player.WHITE, Nickname("Scizor")))
    assertThat(event.newCondition).isEqualTo(VisibleCondition(286, 343, Condition.OK))
    assertThat(event.from?.from).isEqualTo("item: Rocky Helmet")
    assertThat(event.from?.source?.name).isEqualTo(Nickname("Gengar"))
  }

  @Test
  fun healEvent() {
    val event = parseEvent(
        HealEvent::class.java,
        "-heal",
        "p1a: Tapu Fini",
        "286/343",
        "[from] item: Leftovers")
    assertThat(event.pokemon).isEqualTo(PokemonIdentifier(Player.BLACK, Nickname("Tapu Fini")))
    assertThat(event.newCondition).isEqualTo(VisibleCondition(286, 343, Condition.OK))
    assertThat(event.from?.from).isEqualTo("item: Leftovers")
    assertThat(event.from?.source).isNull()
  }

  @Test
  fun turnEvent() {
    val event = parseEvent(TurnEvent::class.java, "turn", "5")
    assertThat(event.turn).isEqualTo(5)
  }

  // TODO: cant and parseFormeChange
}
