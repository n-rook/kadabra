package com.nrook.kadabra.teambuilder

import com.google.common.io.Resources
import com.google.common.truth.Truth
import com.google.gson.GsonBuilder
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.usage.UsageDataset
import com.nrook.kadabra.usage.registerDeserializers
import org.junit.BeforeClass
import org.junit.Test
import java.io.InputStreamReader
import java.util.*

class UsageDatasetTeamPickerTest {

  companion object {
    lateinit var usageDataset: UsageDataset

    @BeforeClass
    @JvmStatic
    fun setUpClass() {
      // This is gross! Create an artificial, shorter .json file.
      val gson = registerDeserializers(GsonBuilder()).create()
      val resource = Resources.getResource("gen7pokebankou-1695.json")

      usageDataset = gson.fromJson(InputStreamReader(resource.openStream()), UsageDataset::class.java)
    }
  }

  @Test
  fun works() {
    val picker = UsageDatasetTeamPicker.create(getGen7Pokedex(), Random(), usageDataset, 0.01)
    val team = picker.pick()
    Truth.assertThat(team).hasSize(6)
  }
}