package com.nrook.kadabra.teambuilder

import com.google.common.io.Resources
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.proto.Nature
import org.junit.Before
import org.junit.Test
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

class FileBasedTeamLoaderTest {
  lateinit var fs : FileSystem
  lateinit var dataDirectory: Path
  lateinit var teamLoader: FileBasedTeamLoader

  @Before
  fun setUp() {
    fs = Jimfs.newFileSystem()
    val resource = Resources.getResource("testTeam.txt")
    dataDirectory = fs.getPath("teams")
    Files.createDirectory(dataDirectory)
    Resources.copy(resource, Files.newOutputStream(dataDirectory.resolve("team.txt")))
    teamLoader = FileBasedTeamLoader(dataDirectory)
  }

  @Test
  fun loadsTeam() {
    teamLoader.loadTeam("team.txt")
  }

  @Test fun loadsSixTeamMembers() {
    val team = teamLoader.loadTeam("team.txt")
    assertThat(team).hasSize(6)
  }

  @Test fun loadsFirstMemberProperly() {
    val team = teamLoader.loadTeam("team.txt")
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