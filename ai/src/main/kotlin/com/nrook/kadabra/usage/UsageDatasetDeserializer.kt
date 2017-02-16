package com.nrook.kadabra.usage

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.proto.Nature
import java.lang.reflect.Type

private val POKEMON_USAGE_DATA_MAP_TYPE: Type = object : TypeToken<Map<String, PokemonUsageData>>() {}.type
private val ABILITY_LIST_TYPE = object : TypeToken<List<AbilityUsageData>>() {}.type
private val ITEM_USAGE_MAP_TYPE = object : TypeToken<Map<String, ItemUsageData>>() {}.type
private val SPREAD_LIST_TYPE = object : TypeToken<List<SpreadUsageData>>() {}.type
private val MOVES_MAP_TYPE = object : TypeToken<Map<String, MoveUsageData>>() {}.type

/**
 * Registers deserializers defined in this class.
 *
 * @param gsonBuilder A reference to the gsonBuilder instance passed in.
 */
fun registerDeserializers(gsonBuilder: GsonBuilder): GsonBuilder {
  return gsonBuilder
      .registerTypeAdapter(UsageDataset::class.java, UsageDatasetDeserializer())
      .registerTypeAdapter(UsageDatasetMetadata::class.java, UsageMetadataDeserializer())
      .registerTypeAdapter(POKEMON_USAGE_DATA_MAP_TYPE, UsageDataMapDeserializer())
      .registerTypeAdapter(ABILITY_LIST_TYPE, AbilityUsageDataListDeserializer())
      .registerTypeAdapter(ITEM_USAGE_MAP_TYPE, ItemUsageDataMapDeserializer())
      .registerTypeAdapter(SPREAD_LIST_TYPE, SpreadUsageDataListDeserializer())
      .registerTypeAdapter(MOVES_MAP_TYPE, MovesUsageDataMapDeserializer())
}

private class UsageDatasetDeserializer : JsonDeserializer<UsageDataset> {
  override fun deserialize(jsonElement: JsonElement, expectedType: Type, context: JsonDeserializationContext):
      UsageDataset {
    val root = jsonElement.asJsonObject
    val infoJson = root.getAsJsonObject("info")
    val info: UsageDatasetMetadata = context.deserialize<UsageDatasetMetadata>(infoJson, UsageDatasetMetadata::class.java)

    val dataJson = root.getAsJsonObject("data")
    val data: Map<String, PokemonUsageData> =
        context.deserialize<Map<String, PokemonUsageData>>(dataJson, POKEMON_USAGE_DATA_MAP_TYPE)
    return UsageDataset(info, data)
  }
}

private class UsageMetadataDeserializer: JsonDeserializer<UsageDatasetMetadata> {
  override fun deserialize(json: JsonElement, expectedType: Type, context: JsonDeserializationContext):
      UsageDatasetMetadata {
    val root = json.asJsonObject

    val usageDatasetMetadata: UsageDatasetMetadata = UsageDatasetMetadata(
        root.getAsJsonPrimitive("cutoff").asDouble.toInt(),
        root.getAsJsonPrimitive("cutoff deviation").asInt,
        root.getAsJsonPrimitive("metagame").asString,
        root.getAsJsonPrimitive("number of battles").asInt
    )
    return usageDatasetMetadata
  }
}

private class UsageDataMapDeserializer: JsonDeserializer<Map<String, PokemonUsageData>> {
  override fun deserialize(json: JsonElement, expectedType: Type, context: JsonDeserializationContext):
      Map<String, PokemonUsageData> {
    val root = json.asJsonObject
    return root.entrySet().map { entry -> toPokemonUsageData( entry.key, entry.value.asJsonObject, context) }
        .associateBy { it -> it.species }
  }
}

private class AbilityUsageDataListDeserializer: JsonDeserializer<List<AbilityUsageData>> {
  override fun deserialize(json: JsonElement, expectedType: Type?, context: JsonDeserializationContext?):
      List<AbilityUsageData> {
    val root = json.asJsonObject
    return root.entrySet()
        .map { entry -> AbilityUsageData(entry.key, entry.value.asDouble) }
  }
}

private class ItemUsageDataMapDeserializer: JsonDeserializer<Map<String, ItemUsageData>> {
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext):
      Map<String, ItemUsageData> {
    val root = json.asJsonObject
    return root.entrySet()
        .map { entry -> ItemUsageData(entry.key, entry.value.asDouble)}
        .associateBy { it -> it.item }
  }
}

private class SpreadUsageDataListDeserializer: JsonDeserializer<List<SpreadUsageData>> {
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext):
      List<SpreadUsageData> {
    val root = json.asJsonObject
    return root.entrySet()
        .map { entry -> SpreadUsageData(toSpread(entry.key), entry.value.asDouble) }
        .filter { spread -> spread.usage > 0 }
  }
}

private class MovesUsageDataMapDeserializer: JsonDeserializer<Map<String, MoveUsageData>> {
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext):
      Map<String, MoveUsageData> {
    val root = json.asJsonObject
    return root.entrySet()
        .map { entry -> MoveUsageData(entry.key, entry.value.asDouble)}
        .associateBy { it -> it.move }
  }
}

private fun toSpread(spread: String): StatSpread {
  // Adamant:88/193/84/0/98/44
  val parts = spread.split(":")

  val nature: Nature = Nature.valueOf(parts[0].toUpperCase())
  val evs = parts[1].split("/").map(String::toInt)

  if (evs.size != 6) {
    throw IllegalArgumentException("Could not convert string ${parts[1]} to an EV spread")
  }

  return StatSpread(
      mapOf(
          Stat.HP to evs[0],
          Stat.ATTACK to evs[1],
          Stat.DEFENSE to evs[2],
          Stat.SPECIAL_ATTACK to evs[3],
          Stat.SPECIAL_DEFENSE to evs[4],
          Stat.SPEED to evs[5]
      ),
      nature)
}

private fun toPokemonUsageData(species: String, info: JsonObject, context: JsonDeserializationContext): PokemonUsageData {
  return PokemonUsageData(
      species,
      context.deserialize<List<AbilityUsageData>>(
          info.getAsJsonObject("Abilities"),
          ABILITY_LIST_TYPE),
      context.deserialize<Map<String, ItemUsageData>>(
          info.getAsJsonObject("Items"),
          ITEM_USAGE_MAP_TYPE),
      info.getAsJsonPrimitive("Raw count").asInt,
      context.deserialize(info.getAsJsonObject("Spreads"),
          SPREAD_LIST_TYPE),
      info.getAsJsonPrimitive("usage").asDouble,
      context.deserialize(
          info.getAsJsonObject("Moves"),
          MOVES_MAP_TYPE)
  )
}