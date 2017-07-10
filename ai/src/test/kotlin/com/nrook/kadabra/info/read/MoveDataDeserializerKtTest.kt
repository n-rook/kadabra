package com.nrook.kadabra.info.read

import com.google.common.io.Resources
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.MoveCategory
import com.nrook.kadabra.info.MoveId
import com.nrook.kadabra.info.PokemonType
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

class MoveDataDeserializerKtTest {

  lateinit var gson: Gson
  lateinit var dataset: Map<MoveId, Move>

  @Before
  fun setUp() {
    this.gson = registerDeserializers(GsonBuilder())
        .create()
    val resource = Resources.getResource("gen7moves.json")

    dataset = gson.fromJson(InputStreamReader(resource.openStream()), MOVE_MAP_TYPE)
  }

  @Test
  fun works() {
    assertThat(dataset).containsKey(MoveId("thunderbolt"))
    val tbolt = dataset[MoveId("thunderbolt")]!!
    assertThat(tbolt.id).isEqualTo(MoveId("thunderbolt"))
    assertThat(tbolt.name).isEqualTo("Thunderbolt")
    assertThat(tbolt.basePower).isEqualTo(90)
    assertThat(tbolt.type).isEqualTo(PokemonType.ELECTRIC)
    assertThat(tbolt.category).isEqualTo(MoveCategory.SPECIAL)
    assertThat(tbolt.fullyUnderstood).isFalse()
  }
}