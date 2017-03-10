package com.nrook.kadabra.info

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PokemonTypeTest {

  @Test
  fun normallyEffective() {
    assertThat(PokemonType.FIRE.effectivenessAgainst(PokemonType.FIGHTING))
        .isEqualTo(Effectiveness.EFFECTIVE)
  }

  @Test
  fun superEffective() {
    assertThat(PokemonType.DARK.effectivenessAgainst(PokemonType.PSYCHIC))
        .isEqualTo(Effectiveness.SUPER_EFFECTIVE)
  }

  @Test
  fun notVeryEffective() {
    assertThat(PokemonType.STEEL.effectivenessAgainst(PokemonType.FIRE))
        .isEqualTo(Effectiveness.NOT_VERY_EFFECTIVE)
  }

  @Test
  fun noEffect() {
    assertThat(PokemonType.GROUND.effectivenessAgainst(PokemonType.FLYING))
        .isEqualTo(Effectiveness.NO_EFFECT)
  }
}