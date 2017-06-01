package com.nrook.kadabra.usage

import com.google.common.collect.ComparisonChain
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.proto.EvSpread
import com.nrook.kadabra.proto.Nature
import java.util.*

/**
 * A batch of Pokemon usage data for a given metagame.
 */
data class UsageDataset(
    val info: UsageDatasetMetadata,

    /**
     * Pokemon usage data. Keys are species.
     */
    val data: Map<String, PokemonUsageData>
) {
  fun banPokemon(bans: Iterable<String>): UsageDataset {
    val newData = HashMap<String, PokemonUsageData>(data)
    for (ban in bans) {
      val removed = newData.remove(ban)
      if (removed == null) {
        throw IllegalArgumentException("Data for Pokemon $ban not found")
      }
    }
    return UsageDataset(info, newData)
  }
}

data class UsageDatasetMetadata(
    // ELO (or is it Glicko or something?) cutoff
    val cutoff: Int,

    /**
     * Unused
     */
    val cutoffDeviation: Int,

    /**
     * The name of the metagame.
     *
     * Does not necessarily correspond to the metagame names used by Showdown.
     */
    val metagame: String,

    val numberOfBattles: Int
)

/**
 * Usage data for a specific Pokemon.
 *
 * Sorted by usage.
 */
data class PokemonUsageData(
    /**
     * The Pokemon's species name.
     */
    val species: String,

    /**
     * Usage information about the Pokemon's abilities.
     */
    val abilities: List<AbilityUsageData>,

    /**
     * The usage for each item. Keys are item names.
     */
    val items: Map<String, ItemUsageData>,

    /**
     * I don't know what this is.
     */
    val rawCount: Int,  // I don't really get what this is

    /**
     * The usage of each EV/Nature spread. Sorted by usage.
     */
    val spreads: List<SpreadUsageData>,

    /**
     * The proportion of teams which contain this Pokemon.
     */
    val usage: Double,  // I don't know what this is

    /**
     * The usage of each individual move.
     */
    val moves: Map<String, MoveUsageData>
): Comparable<PokemonUsageData> {
  override fun compareTo(other: PokemonUsageData): Int {
    return ComparisonChain.start()
        .compare(this.rawCount, other.rawCount)
        .result()
  }
}

/**
 * Contains usage data for a single Pokemon for an ability.
 */
data class AbilityUsageData(
    /**
     * The name of the ability.
     */
    val ability: String,

    /**
     * A weight determining the proportion of Pokemon with this ability.
     *
     * Note that this value across all Pokemon neither sums to 1 nor to the Pokemon's
     * "raw count".
     */
    val usage: Double
)

data class ItemUsageData(
    /**
     * The item's name.
     */
    val item: String,

    /**
     * Weighted usage data for this item. The proportions are right, but it's not clear what the total
     * represents.
     */
    val usage: Double
)

/**
 * A single stat spread, with usage data attached.
 */
data class SpreadUsageData(
    /**
     * The stat spread.
     */
    val spread: StatSpread,

    /**
     * Usage information for the stat spread. Doesn't add up to anything in particular.
     */
    val usage: Double
)

/**
 * Information about a stat spread.
 */
data class StatSpread(
    /**
     * A map from an EV's stat spread to its data.
     *
     * Note that this map has values for precisely all six stats.
     */
    val evs: Map<Stat, Int>,

    /**
     * The nature involved in the stat spread.
     */
    val nature: Nature
) {
  init {
    if (evs.size != 6) {
      throw IllegalArgumentException("EVs ${evs.keys} are missing some stats")
    }
  }

  fun asEvSpreadProto(): EvSpread {
    return EvSpread.newBuilder()
        .setHp(evs[Stat.HP]!!)
        .setAttack(evs[Stat.ATTACK]!!)
        .setDefense(evs[Stat.DEFENSE]!!)
        .setSpecialAttack(evs[Stat.SPECIAL_ATTACK]!!)
        .setSpecialDefense(evs[Stat.SPECIAL_DEFENSE]!!)
        .setSpeed(evs[Stat.SPEED]!!)
        .build()
  }
}

/**
 * Information about the usage of a specific move.
 */
data class MoveUsageData(
    /**
     * The move.
     */
    val move: String,

    /**
     * The move's usage, as a double. Doesn't add up to anything in particular when summed over all
     * moves, but the proportions are right, at least.
     */
    val usage: Double
)
