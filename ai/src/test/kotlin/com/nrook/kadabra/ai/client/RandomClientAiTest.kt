package com.nrook.kadabra.ai.client

import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth
import com.nrook.kadabra.proto.*

class RandomClientAiTest {
  lateinit var ai: RandomClientAi

  // For some reason, putting this constant outside the test class
  // causes catastrophic classloader issues.
  private val SOME_TEAM = ImmutableList.of<PokemonSpec>(
    PokemonSpec.newBuilder()
        .setSpecies("Alakazam")
        .build())

  @org.junit.Before
  fun setUp() {
    ai = RandomClientAi()
  }

  @org.junit.Test
  fun pickLead() {
    val result = ai.pickLead()
    Truth.assertThat(result.leadIndex).isAtLeast(1)
    Truth.assertThat(result.leadIndex).isAtMost(6)
  }

  @org.junit.Test
  fun pickStartOfTurnAction() {
    val result = ai.pickAction(ActionRequest.newBuilder()
        .addAllTeamSpec(SOME_TEAM)
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

  @org.junit.Test
  fun dontPickDisabledStuff() {
    val result = ai.pickAction(ActionRequest.newBuilder()
        .addAllTeamSpec(SOME_TEAM)
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

  @org.junit.Test
  fun ifForceSwitchThenSwitchRatherThanMoving() {
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

    val response = ai.pickAction(ActionRequest.newBuilder()
        .addAllTeamSpec(SOME_TEAM)
        .setSideInfo(SideInfo.newBuilder()
            .addTeam(mon1)
            .addTeam(mon2))
        .setForceSwitch(true)
        .build())

    Truth.assertThat(response.actionCase).isEqualTo(ActionResponse.ActionCase.SWITCH)
    // Pick Blissey, not Electrode
    Truth.assertThat(response.switch.index).isEqualTo(1)
  }
}