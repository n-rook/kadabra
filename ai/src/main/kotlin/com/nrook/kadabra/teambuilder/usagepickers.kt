package com.nrook.kadabra.teambuilder

import com.nrook.kadabra.common.RandomBasket
import com.nrook.kadabra.info.PokemonDefinition
import com.nrook.kadabra.info.PokemonId
import com.nrook.kadabra.proto.PokemonSpec
import com.nrook.kadabra.usage.PokemonUsageData
import com.nrook.kadabra.usage.StatSpread
import com.nrook.kadabra.usage.UsageDataset
import java.util.*

/**
 * A team picker which creates a random team from weighted usage data.
 *
 * @param random A random number generator.
 * @param usageDataset The usage dataset to use as a base.
 */
class UsageDatasetTeamPicker private constructor(
    private val random: Random,
    private val usageDataset: UsageDataset,
    private val speciesPicker: RandomBasket<PokemonUsageData>) : TeamPickingStrategy {

  companion object {
    /**
     * Creates a team picker.
     *
     * @param chanceFloor The chance a Pokemon should be selected, PER POKEMON. Beware: This is much lower than
     *  the chance a Pokemon will be on a given team, so don't make this too strict by accident.
     */
    fun create(
        random: Random,
        usageDataset: UsageDataset,
        chanceFloor: Double): UsageDatasetTeamPicker {
      return UsageDatasetTeamPicker(
          random,
          usageDataset,
          RandomBasket.create(random, usageDataset.data.values, { it.usage }, chanceFloor)
      )
    }
  }

  override fun pick(): List<PokemonDefinition> {
    val team: MutableList<PokemonDefinition> = mutableListOf()
    for (i in 0..5) {
      val choiceData = pickNotAlreadyPicked(speciesPicker, team)
      team.add(rectifySpecies(choiceData))
    }

    return team
  }

  private fun rectifySpecies(usageData: PokemonUsageData): PokemonDefinition {
    // TODO: Make this better

    val ability: String = usageData.abilities.maxBy { it.usage }!!.ability
    val item: String = usageData.items.values.maxBy { it.usage }!!.item
    val statSpread: StatSpread = usageData.spreads.maxBy { it.usage }!!.spread
    val moves: List<String> = usageData.moves.values.sortedByDescending { it.usage }
        .slice(0..3)
        .map { it.move }

    return PokemonDefinition(
        PokemonSpec.newBuilder()
            .setSpecies(usageData.species)
            .setAbility(ability)
            .setEvs(statSpread.asEvSpreadProto())
            .setIvs(MAX_IVS)
            .setItem(item)
            .setNature(statSpread.nature)
            .addAllMove(moves)
            .build())
  }
}

private fun pickNotAlreadyPicked(randomBasket: RandomBasket<PokemonUsageData>, team: List<PokemonDefinition>):
    PokemonUsageData {
  val species = team.map { it.species }.toSet()
  for (i in 0..99) {
    val choice = randomBasket.pick()
    if (!species.contains(choice.species)) {
      return choice
    }
  }

  throw IllegalStateException("Tried 100 times to pick a Pokemon; something is wrong with the state data")
}
