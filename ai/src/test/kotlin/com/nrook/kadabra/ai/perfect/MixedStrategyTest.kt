package com.nrook.kadabra.ai.perfect

import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.*

class MixedStrategyTest {

  @Test
  fun createEvenStrategy() {
    val evenStrategy = MixedStrategy.createEvenStrategy(listOf(1, 2, 3))
    assertThat(evenStrategy.choices).hasSize(3)
    assertThat(evenStrategy.choices[1]).isWithin(0.01).of(0.33)
    assertThat(evenStrategy.choices[2]).isWithin(0.01).of(0.33)
    assertThat(evenStrategy.choices[3]).isWithin(0.01).of(0.33)
  }

  @Test
  fun pickOne() {
    val strategy = MixedStrategy(ImmutableMap.of("a", 0.8, "b", 0.2, "c", 0.0))
    val outcome = strategy.pickOne(Random())
    assertThat(outcome).isAnyOf("a", "b")
  }
}