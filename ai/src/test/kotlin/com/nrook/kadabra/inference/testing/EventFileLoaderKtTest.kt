package com.nrook.kadabra.inference.testing

import com.google.common.io.Resources
import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.inference.PlayerEvent
import com.nrook.kadabra.mechanics.arena.Player
import org.junit.Test
import java.nio.charset.Charset

class EventFileLoaderKtTest {
  @Test
  fun loadTeamBuilderEvents() {
    // This sample file has no sent messages in it.
    val lines = Resources.readLines(
        Resources.getResource("teambuilder1.log"), Charset.forName("UTF-8"))
    val events = loadEventsFromFile(lines)

    assertThat(events[0]).isInstanceOf(PlayerEvent::class.java)
    val playerEvent = events[0] as PlayerEvent
    assertThat(playerEvent.player).isEqualTo(Player.BLACK)
    assertThat(playerEvent.playerName).isEqualTo("nrook")
  }
}