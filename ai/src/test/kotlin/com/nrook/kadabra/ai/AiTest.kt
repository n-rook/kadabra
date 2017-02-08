package com.nrook.kadabra.ai

import com.google.common.truth.Truth
import com.nrook.kadabra.proto.*
import org.junit.Before
import org.junit.Test

class AiTest {
  lateinit var ai: Ai;

  @Before
  fun setUp() {
    ai = Ai()
  }

  @Test
  fun pickLead() {
    val result = ai.pickLead()
    Truth.assertThat(result.leadIndex).isAtLeast(1)
    Truth.assertThat(result.leadIndex).isAtMost(6)
  }

  @Test
  fun pickStartOfTurnAction() {
    val result = ai.pickStartOfTurnAction(ActionRequest.newBuilder()
        .addMove(MoveStatus.newBuilder()
            .setId("flareblitz")
            .setPp(5)
            .setMaxpp(8)
            .setDisabled(false))
        .addMove(MoveStatus.newBuilder()
            .setId("hyperbeam")
            .setPp(5)
            .setMaxpp(8)
            .setDisabled(false))
        .build())
    Truth.assertThat(result.move.index).isAtLeast(1)
    Truth.assertThat(result.move.index).isAtMost(2)
  }

  @Test
  fun dontPickDisabledStuff() {
    val result = ai.pickStartOfTurnAction(ActionRequest.newBuilder()
        .addMove(MoveStatus.newBuilder()
            .setId("flareblitz")
            .setPp(5)
            .setMaxpp(8)
            .setDisabled(true))
        .addMove(MoveStatus.newBuilder()
            .setId("hyperbeam")
            .setPp(5)
            .setMaxpp(8)
            .setDisabled(false))
        .build())
    Truth.assertThat(result.move.index).isEqualTo(2)
  }

  @Test
  fun pickSwitchAfterFaintAction() {
    val mon1 = PokemonSideInfo.newBuilder()
        .setSpecies("Blissey")
        .setHp(800)
        .setMaxHp(5000)
        .setFainted(false)
        .setItem("leftovers")
        .build()

    val mon2 = PokemonSideInfo.newBuilder()
        .setSpecies("Electrode")
        .setHp(0)
        .setFainted(true)
        .setItem("choiceband")
        .build()

    val response = ai.pickSwitchAfterFaintAction(SwitchAfterFaintRequest.newBuilder()
        .setSideInfo(SideInfo.newBuilder()
            .addTeam(mon1)
            .addTeam(mon2))
        .build())

    // Pick Blissey, not Electrode
    Truth.assertThat(response.switch.index).isEqualTo(1)
  }
}