package com.nrook.kadabra.info.read

import com.google.common.io.Resources
import com.google.gson.GsonBuilder
import com.nrook.kadabra.info.*
import java.io.InputStreamReader

/**
 * Returns the Gen 7 pokedex.
 */
fun getGen7Species(): Map<PokemonId, Species> {
  val gson = registerPokedexDeserializers(GsonBuilder()).create()
  val resource = Resources.getResource("gen7pokedex.json")

  return gson.fromJson(InputStreamReader(resource.openStream()), POKEDEX_MAP_TYPE)
}

/**
 * Returns all moves in Gen 7.
 */
fun getGen7Movedex(): Map<MoveId, Move> {
  val gson = registerDeserializers(GsonBuilder()).create()
  val resource = Resources.getResource("gen7moves.json")

  return gson.fromJson(InputStreamReader(resource.openStream()), MOVE_MAP_TYPE)
}

fun getGen7Pokedex(): Pokedex {
  return Pokedex.create(getGen7Species().values.toList(), getGen7Movedex().values.toList())
}