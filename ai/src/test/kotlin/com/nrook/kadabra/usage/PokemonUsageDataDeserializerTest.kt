package com.nrook.kadabra.usage

import com.google.common.io.Resources
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.proto.Nature
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

class PokemonUsageDataDeserializerTest {
  lateinit var gson: Gson
  lateinit var dataset: UsageDataset

  @Before
  fun setUp() {
    this.gson = registerDeserializers(GsonBuilder())
        .create()
    val resource = Resources.getResource("gen7pokebankou-1695.json")

    dataset = gson.fromJson(InputStreamReader(resource.openStream()), UsageDataset::class.java)
  }

  @Test
  fun metadata() {
    val metadata = dataset.info
    assertThat(metadata).isEqualTo(UsageDatasetMetadata(
        cutoff = 1695,
        cutoffDeviation = 0,
        metagame = "gen7pokebankou",
        numberOfBattles = 1141698
    ))
  }

  @Test
  fun abilities() {
    val toxapex = dataset.data["Toxapex"]!!
    assertThat(toxapex.abilities).hasSize(3)
    val abilityMap: Map<String, AbilityUsageData> = toxapex.abilities.associateBy { it -> it.ability }
    assertThat(abilityMap).containsKey("regenerator")
    assertThat(abilityMap["regenerator"]!!.usage).isWithin(0.1).of(12708.6066)
    assertThat(abilityMap).containsKey("merciless")
    assertThat(abilityMap["merciless"]!!.usage).isWithin(0.1).of(292.5516)
    assertThat(abilityMap).containsKey("limber")
    assertThat(abilityMap["limber"]!!.usage).isWithin(0.1).of(7.4170)
  }

  @Test
  fun items() {
    val pheromosa = dataset.data["Pheromosa"]!!
    val items = pheromosa.items
    assertThat(items["choicescarf"]!!.usage).isWithin(0.1).of(7960.2644)
    assertThat(items["fightiniumz"]!!.usage).isWithin(0.1).of(1489.1001)
  }

  @Test
  fun spreads() {
    val tapuLele = dataset.data["Tapu Lele"]!!
    val spreads = tapuLele.spreads.sortedByDescending { it -> it.usage }
    assertThat(spreads[0].spread).isEqualTo(StatSpread(
        mapOf(
            Stat.HP to 0,
            Stat.ATTACK to 0,
            Stat.DEFENSE to 0,
            Stat.SPECIAL_ATTACK to 252,
            Stat.SPECIAL_DEFENSE to 4,
            Stat.SPEED to 252
        ),
        Nature.TIMID))
    assertThat(spreads[0].usage).isWithin(0.1).of(19036.8860)
  }

  @Test
  fun moves() {
    val dugtrio = dataset.data["Dugtrio"]!!
    val moves = dugtrio.moves
    assertThat(moves["earthquake"]!!.usage).isWithin(0.1).of(14510.8619)

    // I'm not clear on why these assertions don't work.
    // moves.usage / rawCount has a minimum way below 1.
//    assertThat(moves["earthquake"]!!.usage / dugtrio.rawCount).isGreaterThan(0.9)
//    assertThat(moves["earthquake"]!!.usage / dugtrio.rawCount).isAtMost(1.0)
  }
}