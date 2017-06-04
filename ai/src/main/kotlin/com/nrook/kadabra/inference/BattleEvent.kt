package com.nrook.kadabra.inference

import com.nrook.kadabra.mechanics.arena.Player

/**
 * An event which occurred in a battle.
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
 * An event representing a choice made by us.
 *
 * When a player makes a choice, Showdown returns a "choice event" describing the choice that was
 * made.
 */
data class ChoiceEvent(
    val choice: String
): BattleEvent
