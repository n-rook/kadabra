package com.nrook.kadabra.inference.testing

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.inference.BattleEvent
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.TeamPokemon
import com.nrook.kadabra.teambuilder.TeamLoader

data class EventFileWithoutTeams(
    override val events: ImmutableList<BattleEvent>
): EventFile

data class EventFileWithBlackTeam(
    override val events: ImmutableList<BattleEvent>,
    val black: ImmutableList<TeamPokemon>
): EventFile

data class EventFileWithWhiteTeam(
    override val events: ImmutableList<BattleEvent>,
    val white: ImmutableList<TeamPokemon>
): EventFile

data class FullEventFile(
    override val events: ImmutableList<BattleEvent>,
    val black: ImmutableList<TeamPokemon>,
    val white: ImmutableList<TeamPokemon>
): EventFile

interface EventFile {
  val events: ImmutableList<BattleEvent>
}

@Suppress("PropertyName")
class EventFileBank(val pokedex: Pokedex) {
  private val teamLoader: TeamLoader = TeamLoader(pokedex)

  /**
   * The first sample battle recorded; consists of a few actions. Only the BLACK team is available.
   */
  val SAMPLE by lazy {
    EventFileWithBlackTeam(
        ImmutableList.copyOf(loadEventsFromResource("battle1.log")),
        teamLoader.loadTeamFromResource("battle1_p2_team.txt")
    )
  }

  /**
   * A battle which only has gotten up to the team preview stage. Only BLACK team available.
   */
  val TEAM_PREVIEW by lazy {
    EventFileWithBlackTeam(
        ImmutableList.copyOf(loadEventsFromResource("teambuilder1.log")),
        teamLoader.loadTeamFromResource("teambuilder1_p2_team.txt")
    )
  }
}
