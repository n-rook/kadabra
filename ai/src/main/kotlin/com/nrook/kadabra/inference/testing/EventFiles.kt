package com.nrook.kadabra.inference.testing

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.inference.BattleEvent
import com.nrook.kadabra.inference.ChooseSwitchEvent
import com.nrook.kadabra.inference.DamageEvent
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
  // TODO: Wait, this makes no sense. Looking at the log, aren't we WHITE?
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

  /**
   * On White's first turn, their lead Scizor uses U-Turn. The log ends at the
   * the decision to switch.
   */
  val U_TURN_FIRST by lazy {
    EventFileWithWhiteTeam(
        ImmutableList.copyOf(loadEventsFromResource("BattleWithUturnGoingFirst.log")),
        teamLoader.loadTeamFromResource("UTurnTeam.txt")
    )
  }

  /**
   * On White's first turn, their lead Scizors uses U-Turn after their opponent attacks.
   *
   * Ends immediately after the U-Turn connects.
   *
   */
  val U_TURN_SECOND_IMMEDIATELY_AFTER_U_TURN_HITS by lazy {
    EventFileWithWhiteTeam(
        snipUntilChoice(
            loadEventsFromResource("BattleWithUturnGoingSecond.log"),
            "7"),
        teamLoader.loadTeamFromResource("UTurnTeam.txt")
    )
  }

  /**
   * On White's first turn, their lead Scizors uses U-Turn after their opponent attacks.
   *
   * This log ends on turn 2.
   */
  val U_TURN_SECOND by lazy {
    EventFileWithWhiteTeam(
        ImmutableList.copyOf(loadEventsFromResource("BattleWithUturnGoingSecond.log")),
        teamLoader.loadTeamFromResource("UTurnTeam.txt")
    )
  }
}
