package com.nrook.kadabra.mechanics

import com.google.common.truth.Truth
import com.nrook.kadabra.info.testdata.ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD
import com.nrook.kadabra.info.testdata.BLASTOISE
import org.junit.Test

class BenchedPokemonTest {

  @Test
  fun toActive() {
    val benched = BenchedPokemon(
        BLASTOISE,
        ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD, // I guess it used Transform!
        123,
        Condition.BURN)

    val activeVersion = benched.toActive()
    Truth.assertThat(activeVersion.species).isEqualTo(BLASTOISE)
    Truth.assertThat(activeVersion.originalSpec).isEqualTo(ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD)
    Truth.assertThat(activeVersion.hp).isEqualTo(123)
    Truth.assertThat(activeVersion.condition).isEqualTo(Condition.BURN)

    Truth.assertThat(activeVersion.toBenched()).isEqualTo(benched)
  }
}
