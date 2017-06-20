package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.MoveId
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.arena.Player

/**
 * An event which occurred in a battle.
 *
 * @link https://github.com/Zarel/Pokemon-Showdown/blob/master/PROTOCOL.md
 */
interface BattleEvent

/**
 * An event which indicates the name of a player.
 *
 * @param player Which player this is.
 * @param playerName The player's name.
 */
data class PlayerEvent(
    val player: Player,
    val playerName: String): BattleEvent

/**
 * An event which describes whether this game is singles, doubles or something else.
 *
 * We don't play doubles, so this generally isn't used for anything.
 */
data class GameTypeEvent(val gameType: String): BattleEvent

/**
 * An event which describes which generation we're playing.
 *
 * We only play gen 7 (Sun and Moon), so this is generally irrelevant.
 */
data class GenerationEvent(val generation: Int): BattleEvent

/**
 * Which tier we're playing.
 *
 * @param tier The name of the tier. Generally "[Gen 7] OU".
 */
data class TierEvent(val tier: String): BattleEvent

/**
 * A marker event which proceeds the "poke" events which describe both teams.
 */
class ClearPokeEvent private constructor(): BattleEvent {
  companion object instance {
    val INSTANCE = ClearPokeEvent()
  }
}

/**
 * An event which describes a player's initial team from Team Preview.
 *
 * @param player Whose team this Pokemon is on.
 * @param details A "Pokemon details string". I haven't completely puzzled out this string's format,
 *    but it contains a Pokemon's species (to a certain extent: for certain Pokemon, Forme isn't
 *    included) and its gender.
 * @param item If the Pokemon has an item, this is "item".
 *    TODO: What does the message look like if this isn't an item?
 */
data class PokeEvent(
    val player: Player,
    val details: String,
    val item: String
): BattleEvent

/**
 * A special rule, like the sleep clause, which applies to this battle.
 *
 * @param rule A description of the rule. Typically this takes the form of a short name, like
 *    "Sleep Clause", a colon, then a long description of what the clause means.
 */
data class RuleEvent(
    val rule: String
): BattleEvent

/**
 * An event which marks that the player should start working on Team Preview.
 */
class TeamPreviewEvent private constructor(): BattleEvent {
  companion object instance {
    val INSTANCE = TeamPreviewEvent()
  }
}

/**
 * An unknown event type. What it means is unclear.
 */
class SeedEvent private constructor(): BattleEvent {
  companion object instance {
    val INSTANCE = SeedEvent()
  }
}

/**
 * An event representing the start of battle.
 *
 * Team preview and rules occur before this; normal battle decisions occur after.
 */
class StartEvent private constructor(): BattleEvent {
  companion object instance {
    val INSTANCE = StartEvent()
  }
}

/**
 * This event triggers when a Pokemon switches out. It includes both [SwitchEvent] and [DragEvent].
 */
interface SwitchOrDragEvent: BattleEvent {
  val player: Player
  val identifier: Nickname
  val details: PokemonDetails
  val condition: VisibleCondition
}

/**
 * This event triggers when a Pokemon switches by choice. This includes both regular switches and
 * less conventional switches, triggered by moves like U-Turn, but does not include forced
 * switches like Roar.
 */
data class SwitchEvent(
    override val player: Player,
    override val identifier: Nickname,
    override val details: PokemonDetails,
    override val condition: VisibleCondition
): SwitchOrDragEvent

/**
 * This event triggers when a Pokemon is forced to switch, such as by the move Whirlwind.
 */
data class DragEvent(
    override val player: Player,
    override val identifier: Nickname,
    override val details: PokemonDetails,
    override val condition: VisibleCondition
): SwitchOrDragEvent

/**
 * This event triggers when a Pokemon users a move.
 *
 * Each move has a source (the Pokemon moving) and a target (the Pokemon being affected).
 * This is true even when a move doesn't really have a meaningful target: For instance, Sunny Day
 * "targets" the Pokemon using the move.
 *
 * Known from tags:
 * [from]lockedmove if the Pokemon's move is locked.
 * [from]Nature Power if the Pokemon's move was used because Nature Power was used. Here,
 * the Pokemon is shown using Nature Power first, and then immediately using their second move
 * with this as the reason.
 *
 * @property source The Pokemon making the move.
 * @property move The ID of the move being made.
 * @property target The Pokemon being affected by the move.
 * @property miss Whether the move missed.
 * @property from An optional string that explains why a move would made, if the move was forced.
 *  For instance, this is "lockedmove" if the Pokemon is locked into a move like Outrage.
 * @property unparsedTags Other tags we don't specifically parse yet.
 */
data class MoveEvent(
    val source: PokemonIdentifier,
    val move: MoveId,
    val target: PokemonIdentifier,
    val miss: Boolean,
    val from: FromTag?,
    val unparsedTags: ImmutableList<String>
): BattleEvent

/**
 * The source of an effect.
 *
 * @property from A string describing the effect.
 * @property of The Pokemon to which the effect belongs.
 */
data class FromTag(val from: String, val source: PokemonIdentifier?)

/**
 * An object which identifies a Pokemon. Does not change throughout a battle.
 *
 * This is the object representation of strings like "p2a: Charizard".
 * TODO: Use consistently
 */
data class PokemonIdentifier(
    val player: Player,
    val name: Nickname
)

/**
 * Visible details about a Pokemon.
 *
 * These are most prominently displayed in a [SwitchEvent].
 *
 * @param species The Pokemon's species name, like "Venasaur-Mega".
 * @param shiny Whether the Pokemon is shiny.
 * @param gender The Pokemon's gender.
 * @param level The Pokemon's level.
 */
data class PokemonDetails(
    val species: String,
    val shiny: Boolean,
    val gender: Gender,
    val level: Level
)

/**
 * A Pokemon's visible condition.
 */
data class VisibleCondition(
    val hp: Int,
    val maxHp: Int,
    val status: Status
)

/**
 * A constant for representing a fainted Pokemon.
 *
 * In the spec, the max HP of fainted Pokemon is generally unknown, so it is just set to 1 here.
 */
val CONDITION_FAINTED = VisibleCondition(0, 1, Status.FAINTED)

/**
 * A Pokemon's visible status effect.
 */
enum class Status {
  OK,
  POISON,
  TOXIC,
  BURN,
  FROZEN,
  PARALYZED,
  FAINTED
}

/**
 * An event representing a choice made by us.
 *
 * When a player makes a choice, Showdown returns a "choice event" describing the choice that was
 * made.
 */
data class ChoiceEvent(
    val choice: String
): BattleEvent

/**
 * A Pokemon's nickname.
 *
 * Note that these identifiers do not change in battle, under any circumstances! For instance, if an
 * Alakazam is named Alakazam, then mega evolves into Alakazam-Mega, it'll still be named
 * Alakazam, not Alakazam-Mega.
 *
 * I'm not sure how Zoroark works here.
 */
data class Nickname(
    val nickname: String
)