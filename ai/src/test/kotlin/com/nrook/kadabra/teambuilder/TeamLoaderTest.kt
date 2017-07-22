package com.nrook.kadabra.teambuilder

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.mechanics.Nature
import org.junit.Before
import org.junit.Test

class TeamLoaderTest {
  lateinit private var teamLoader: TeamLoader

  @Before
  fun setUp() {
    teamLoader = TeamLoader(getGen7Pokedex())
  }

  @Test
  fun loadsTeam() {
    teamLoader.loadTeamFromResource("testTeam.txt")
  }

  @Test fun loadsSixTeamMembers() {
    val team = teamLoader.loadTeamFromResource("testTeam.txt")
    assertThat(team).hasSize(6)
  }

  @Test fun loadsFirstMemberProperly() {
    val team = teamLoader.loadTeamFromResource("testTeam.txt")
    val lead = team[0]
    assertThat(lead.species.name).isEqualTo("Alakazam")
    assertThat(lead.item).isEqualTo("Alakazite")
    assertThat(lead.ability.str).isEqualTo("Synchronize")
    assertThat(lead.evSpread.values).containsExactly(
        Stat.HP, 0,
        Stat.ATTACK, 0,
        Stat.DEFENSE, 0,
        Stat.SPECIAL_ATTACK, 252,
        Stat.SPECIAL_DEFENSE, 4,
        Stat.SPEED, 252
    )
    assertThat(lead.nature).isEqualTo(Nature.TIMID)
    assertThat(lead.moves.map { it.name }).containsExactly(
        "Psychic", "Focus Blast", "Shadow Ball", "Substitute")
  }

  @Test
  fun loadsTwoWordItems() {
    val team = teamLoader.loadTeamFromResource("testTeam.txt")
    assertThat(team[2].item).isEqualTo("Choice Band")
  }
}