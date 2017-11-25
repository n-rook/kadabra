package com.nrook.kadabra.teambuilder

import com.nrook.kadabra.common.RandomBasket
import com.nrook.kadabra.info.AbilityId
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.TeamPokemon
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.Nature
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
    private val pokedex: Pokedex,
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
        pokedex: Pokedex,
        random: Random,
        usageDataset: UsageDataset,
        chanceFloor: Double): UsageDatasetTeamPicker {
      return UsageDatasetTeamPicker(
          pokedex,
          random,
          usageDataset,
          RandomBasket.create(random, usageDataset.data.values, { it.usage }, chanceFloor)
      )
    }
  }

  override fun pick(): List<TeamPokemon> {
    val team: MutableList<TeamPokemon> = mutableListOf()
    for (i in 0..5) {
      val choiceData = pickNotAlreadyPicked(speciesPicker, team)
      team.add(reifySpecies(choiceData))
    }

    return team
  }

  private fun reifySpecies(usageData: PokemonUsageData): TeamPokemon {
    val ability: String = usageData.abilities.maxBy { it.usage }!!.ability
    val item: String = usageData.items.values.maxBy { it.usage }!!.item
    val statSpread: StatSpread = usageData.spreads.maxBy { it.usage }!!.spread
    val moves: List<String> = usageData.moves.values.sortedByDescending { it.usage }
        .slice(0..3)
        .map { it.move }

    return TeamPokemon(
        pokedex.getSpeciesByName(usageData.species),
        item,
        pokedex.getAbilityByUsageCode(ability),
        null,
        Nature.valueOf(statSpread.nature.name),
        statSpread.asEvSpread(),
        com.nrook.kadabra.mechanics.MAX_IVS,
        Level(100),
        moves.map { pokedex.getMoveByUsageCode(it) }
    )
  }

  private fun pickNotAlreadyPicked(
      randomBasket: RandomBasket<PokemonUsageData>,
      team: List<TeamPokemon>):
      PokemonUsageData {
    val species = team.map { it.species.id }.toSet()
    for (i in 0..99) {
      val choice = randomBasket.pick()
      if (!species.contains(pokedex.getSpeciesByName(choice.species).id)) {
        return choice
      }
    }

    throw IllegalStateException("Tried 100 times to pick a Pokemon; something is wrong with the state data")
  }
}
