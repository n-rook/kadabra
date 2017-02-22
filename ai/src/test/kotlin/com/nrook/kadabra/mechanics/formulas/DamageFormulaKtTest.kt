package com.nrook.kadabra.mechanics.formulas

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.mechanics.Level
import org.junit.Test

class DamageFormulaKtTest {
  @Test
  fun computeDamageTest() {
    // Charizard using Earthquake against Blastoise, at L100
    val damage = computeDamage(Level(100), 204, 236, 100, false)
    assertThat(damage).isEqualTo(62..74)
  }

  @Test
  fun critTest() {
    val damage = computeDamage(Level(100), 204, 236, 100, true)
    assertThat(damage).isEqualTo(94..111)
  }
}