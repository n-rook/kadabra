package com.nrook.kadabra.mechanics.rng

import com.google.common.truth.Truth
import org.junit.Test
import java.util.*

class RandomNumberGeneratorTest {
  @Test
  fun moveDamage() {
    val generator = RandomNumberGenerator(RandomPolicy(MoveDamagePolicy.ONE), Random())
    Truth.assertThat(generator.moveDamage()).isEqualTo(92)
  }
}