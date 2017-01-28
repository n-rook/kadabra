package com.nrook.kadabra.teambuilder

import com.google.common.io.Resources
import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.proto.Nature
import org.junit.Test
import java.net.URL

class LoadTeamTest {
  val RESOURCE: URL = Resources.getResource("testTeam.txt")

  @Test
  fun loadsTeam() {
    loadTeamFromResource(RESOURCE)
  }

  @Test fun loadsSixTeamMembers() {
    val team = loadTeamFromResource(RESOURCE)
    assertThat(team).hasSize(6)
  }

  @Test fun loadsFirstMemberProperly() {
    val team = loadTeamFromResource(RESOURCE)
    val lead = team[0]
    assertThat(lead.species).isEqualTo("Alakazam")
    assertThat(lead.item).isEqualTo("Alakazite")
    assertThat(lead.ability).isEqualTo("Synchronize")
    assertThat(lead.evs).containsExactly(
        Stat.HP, 0,
        Stat.ATTACK, 0,
        Stat.DEFENSE, 0,
        Stat.SPECIAL_ATTACK, 252,
        Stat.SPECIAL_DEFENSE, 4,
        Stat.SPEED, 252
    )
    assertThat(lead.nature).isEqualTo(Nature.TIMID)
    assertThat(lead.moves).containsExactly(
        "Psychic", "Focus Blast", "Shadow Ball", "Substitute")
  }
}