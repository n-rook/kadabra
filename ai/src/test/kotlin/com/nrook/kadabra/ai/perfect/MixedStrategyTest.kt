package com.nrook.kadabra.ai.perfect

import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

class MixedStrategyTest {

  lateinit var random: Random

  @Before
  fun setUp() {
    random = Random()
  }

  @Test
  fun createEvenStrategy() {
    val evenStrategy = MixedStrategy.createEvenStrategy(listOf(1, 2, 3))
    assertThat(evenStrategy.choices).hasSize(3)
    assertThat(evenStrategy.choices[1]).isWithin(0.01).of(0.33)
    assertThat(evenStrategy.choices[2]).isWithin(0.01).of(0.33)
    assertThat(evenStrategy.choices[3]).isWithin(0.01).of(0.33)
  }

  @Test
  fun pickOneNeverPicksZeroProbabilityOption() {
    val strategy = MixedStrategy(ImmutableMap.of("a", 0.8, "b", 0.2, "c", 0.0))
    for (i in 0 until 10) {
      val outcome = strategy.pickOne(random)
      assertThat(outcome).isAnyOf("a", "b")
    }
  }

  @Test
  fun pickOneRarelyPicksLowProbabilityOption() {
    val strategy = MixedStrategy(ImmutableMap.of("likely", 0.9999, "unlikely", 0.0001))
    val unlikelyHits = (0 until 20)
        .map { strategy.pickOne(random) }
        .count {it == "unlikely"}
    assertThat(unlikelyHits).isLessThan(3)
  }
}