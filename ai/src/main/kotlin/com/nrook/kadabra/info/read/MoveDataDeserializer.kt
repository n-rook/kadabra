package com.nrook.kadabra.info.read

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.MoveId
import com.nrook.kadabra.info.PokemonType
import java.lang.reflect.Type

/**
 * A convenience type token for a map from move ID to move.
 */
val MOVE_MAP_TYPE: Type = object : TypeToken<Map<MoveId, Move>>() {}.type


/**
 * Registers deserializers defined in this class.
 *
 * @param gsonBuilder A reference to the gsonBuilder instance passed in.
 */
fun registerDeserializers(gsonBuilder: GsonBuilder): GsonBuilder {
  return gsonBuilder
      .registerTypeAdapter(MOVE_MAP_TYPE, MoveMapDeserializer())
      .registerTypeAdapter(Move::class.java, MoveDeserializer())
      .registerTypeAdapter(PokemonType::class.java, TypeDeserializer())
}

private class MoveMapDeserializer: JsonDeserializer<Map<MoveId, Move>> {
  override fun deserialize(
      json: JsonElement, expectedType: Type, context: JsonDeserializationContext):
      Map<MoveId, Move> {
    val root: JsonObject = json.asJsonObject
    val movedex: JsonObject = root["BattleMovedex"].asJsonObject
    return movedex.entrySet()
        .map { context.deserialize<Move>(it.value, Move::class.java) }
        .associateBy(Move::id)
  }
}

private class MoveDeserializer: JsonDeserializer<Move> {
  override fun deserialize(
      json: JsonElement, expectedType: Type, context: JsonDeserializationContext): Move {
    val root = json.asJsonObject
    return Move(
        MoveId(root["id"].asString),
        root["basePower"].asInt,
        context.deserialize(root["type"], PokemonType::class.java),
        false)
  }
}

// TODO: Is making this public actually a good idea?
class TypeDeserializer: JsonDeserializer<PokemonType> {
  override fun deserialize(
      json: JsonElement, expectedType: Type, context: JsonDeserializationContext): PokemonType {
    val type = json.asString
    return PokemonType.valueOf(type.toUpperCase())
  }
}