package com.nrook.kadabra.inference

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

  return TeamPreviewRequest(
      ourSide["name"].asString,
      ID_TO_PLAYER_CONVERTER.convert(ourSide["id"].asString)!!)
}

interface RequestMessage

/**
 * A request sent during the "Team Preview" phase of the game.
 *
 * The Team Preview message also contains lots of statistical information about our team, but since
 * we should already know all this information, it isn't terribly relevant.
 *
 * @param name The name of this player: the name of the player making the request.
 * @param id Which player this is: White or Black.
 */
data class TeamPreviewRequest(
    val name: String,
    val id: Player
): RequestMessage