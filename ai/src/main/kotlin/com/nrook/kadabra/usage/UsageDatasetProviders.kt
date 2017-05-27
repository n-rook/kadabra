package com.nrook.kadabra.usage

import com.google.common.io.Resources
import com.google.gson.GsonBuilder
import java.io.InputStreamReader

/**
 * Returns OU data from 2017.
 */
fun getOuUsageDataset(): UsageDataset {
  val gson = registerDeserializers(GsonBuilder()).create()
  val resource = Resources.getResource("gen7pokebankou-1695.json")
  return gson.fromJson(InputStreamReader(resource.openStream()), UsageDataset::class.java)
}
