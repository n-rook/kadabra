package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.Species
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.Condition
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.arena.Player

/**
 * A class which reads in a battle state from a list of logs.
 */
class BattleLoader(private val pokedex: Pokedex) {

  /**
   * Parse the state of a battle.
   */
  fun parseBattle(ourTeam: List<PokemonSpec>, events: List<BattleEvent>) {
    // First, figure out who we are.
    val us: Player = findUs(events)

    // Next, identify all friends and enemies.


    // Finally, iterate through all battle events, adjusting states as we go.
  }

  /**
   * Parse the state of a battle up to team preview/lead select.
   */
  fun parseTeamPreviewBattle(ourTeam: List<PokemonSpec>, events: List<BattleEvent>)
      : TeamPreviewBattleState {
    val us = findUs(events)
    val ourBench = findOurSide(events, ourTeam)
    val theirBench = findTheirSide(us, events)
    return TeamPreviewBattleState(us, ourBench, theirBench)
  }

  /**
   * Return which player we are.
   */
  private fun findUs(events: List<BattleEvent>): Player {
    val firstTeamPreviewRequest = events.firstOrNull {
      val request = (it as? RequestEvent)?.request
      request != null && request is TeamPreviewRequest
    } ?: throw IllegalArgumentException("Could not find TeamPreviewRequest")
    return ((firstTeamPreviewRequest as RequestEvent).request as TeamPreviewRequest).id
  }

  /**
   * Look up and return full information about the Pokemon on our side.
   */
  private fun findOurSide(
      events: List<BattleEvent>,
      ourTeam: List<PokemonSpec>): ImmutableList<OurBenchedPokemon> {
    val teamPreviewEvent = events.filterIsInstance(RequestEvent::class.java)
        .first { it.request is TeamPreviewRequest }
    val teamPreviewRequest = teamPreviewEvent.request as TeamPreviewRequest

    val ourTeamAsBenchedPokemon: ImmutableList.Builder<OurBenchedPokemon> = ImmutableList.builder()
    for (pokemon in teamPreviewRequest.pokemon) {
      val spec = ourTeam
          // TODO: This may not work, or it may only work for some species (i.e. not Arceus)
          .find { it.species.name == pokemon.details.species }
      if (spec == null) {
        throw IllegalArgumentException(
            "Could not find corresponding Pokemon for ${pokemon.details.species}. Choices were: " +
                ourTeam.map { it.species.name }.joinToString(", "))
      }

      ourTeamAsBenchedPokemon.add(
          OurBenchedPokemon(
              spec.species,
              pokemon.pokemon.name,
              spec,
              spec.getStat(Stat.HP),
              Condition.OK))
    }

    return ourTeamAsBenchedPokemon.build()
  }

  private fun findTheirSide(
      us: Player,
      events: List<BattleEvent>): ImmutableList<TheirBenchedPokemon> {
    val theirPokeEvents = events.filterIsInstance(PokeEvent::class.java)
        .filter { it.player != us }
    val theirBench = theirPokeEvents.map {
      TheirBenchedPokemon(
          pokedex.getSpeciesByName(it.details.species),
          it.details.gender,
          it.details.shiny,
          it.details.level,
          null,
          HpFraction(100, 100),
          Condition.OK
      )
    }
    return ImmutableList.copyOf(theirBench)
  }

//  private fun buildBenchedPokemonFromRequestInfo(
//      request: TeamPreviewRequest, ourTeam: List<PokemonDefinition>) {
//
//  }
}

/**
 * A battle state parsed up to team preview.
 *
 * @property us Which player we are.
 * @property ourBench The Pokemon on our bench. In order: if we want to lead with the first Pokemon
 *    we have, we'll be sending a message like
 *
 */
data class TeamPreviewBattleState(
    val us: Player,
    val ourBench: ImmutableList<OurBenchedPokemon>,
    val theirBench: ImmutableList<TheirBenchedPokemon>
)

/**
 * A benched Pokemon on our side.
 */
data class OurBenchedPokemon(
    val species: Species,
    val nickname: Nickname,
    val originalSpec: PokemonSpec,
    val hp: Int,
    val condition: Condition
)

/**
 * A benched Pokemon.
 *
 * We don't necessarily know the nicknames of benched Pokemon, if they're opponents.
 * For active Pokemon, we do know, since the nickname is said when they are switched in.
 */
data class TheirBenchedPokemon(
    val species: Species,
    val gender: Gender,
    val shiny: Boolean,
    val level: Level,
    val nickname: Nickname?,
    val hp: HpFraction,
    val condition: Condition
)

/**
 * The information we have about an opponent's HP, represented as a fraction.
 *
 * We don't get perfect information about our opponents' HP. We get this fraction instead. The
 * denominator is 100 if HP Percentage Mod is on, as it is in competitive ladders, and 48 otherwise.
 */
data class HpFraction(val numerator: Int, val denominator: Int)