package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.nrook.kadabra.mechanics.arena.Player
import java.lang.reflect.Type

private val gson: Gson = registerRequestDeserializers(GsonBuilder()).create()

/**
 * Actually deserialize a request using a singleton [Gson] instance.
 */
fun deserializeRequest(request: String): RequestMessage {
  return gson.fromJson(request, RequestMessage::class.java)
}

/**
 * Registers deserializers defined in this class.
 *
 * @param gsonBuilder A reference to the gsonBuilder instance passed in.
 */
fun registerRequestDeserializers(gsonBuilder: GsonBuilder): GsonBuilder {
  return gsonBuilder.registerTypeAdapter(RequestMessage::class.java, RequestDeserializer())
}

private class RequestDeserializer: JsonDeserializer<RequestMessage> {
  override fun deserialize(json: JsonElement, expectedType: Type, context: JsonDeserializationContext): RequestMessage {
    val root = json.asJsonObject

    val teamPreview = root.has("teamPreview") && root["teamPreview"].asBoolean
    if (teamPreview) {
      return deserializeTeamBuilderRequest(root)
    }

    throw NotImplementedError("We have not yet handled non-team builder requests.")
  }
}

private fun deserializeTeamBuilderRequest(root: JsonObject)
    : TeamPreviewRequest {
  val ourSide = root["side"].asJsonObject
  val ourPokemon = ourSide["pokemon"].asJsonArray
  val parsedPokemon = ourPokemon
      .map { it.asJsonObject }
      .map(::deserializeRequestInfoPokemon)

  return TeamPreviewRequest(
      ourSide["name"].asString,
      ID_TO_PLAYER_CONVERTER.convert(ourSide["id"].asString)!!,
      ImmutableList.copyOf(parsedPokemon))
}

private fun deserializeRequestInfoPokemon(info: JsonObject): RequestInfoPokemon {
  return RequestInfoPokemon(
      parsePokemonString(info["ident"].asString),
      parseDetails(info["details"].asString)
  )
}

interface RequestMessage

/**
 * A request sent during the "Team Preview" phase of the game.
 *
 * I've been lazy here; we don't actually parse unnecessary info.
 *
 * @param name The name of this player: the name of the player making the request.
 * @param id Which player this is: White or Black.
 */
data class TeamPreviewRequest(
    val name: String,
    val id: Player,
    val pokemon: List<RequestInfoPokemon>
): RequestMessage

/**
 * The information available about a Pokemon when the Team Preview request is sent.
 *
 * This is information about *our* Pokemon, not theirs.
 */
data class RequestInfoPokemon(
    val pokemon: PokemonIdentifier,
    val details: PokemonDetails
)
