package com.nrook.kadabra.inference

import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.PokemonId
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
 * This event triggers when a Pokemon switches by choice. This includes both regular switches and
 * less conventional switches, triggered by moves like U-Turn, but does not include forced
 * switches like Roar.
 */
data class SwitchEvent(
    val player: Player,
    val identifier: Nickname,
    val details: PokemonDetails,
    val condition: VisibleCondition
): BattleEvent

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