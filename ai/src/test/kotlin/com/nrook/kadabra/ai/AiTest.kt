package com.nrook.kadabra.ai

import com.google.common.truth.Truth
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
    val result = ai.pickStartOfTurnAction()
    Truth.assertThat(result.move.index).isEqualTo(1)
  }
}