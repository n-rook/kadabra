package com.nrook.kadabra.info.read

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.nrook.kadabra.info.*
import java.lang.reflect.Type
import java.math.BigDecimal

val POKEDEX_MAP_TYPE: Type = object : TypeToken<Map<PokemonId, Species>>() {}.type
private val BASE_STATS_MAP_TYPE: Type = object : TypeToken<Map<Stat, Int>>() {}.type

/**
 * Registers deserializers defined in this class.
 *
 * @param gsonBuilder A reference to the gsonBuilder instance passed in.
 */
fun registerPokedexDeserializers(gsonBuilder: GsonBuilder): GsonBuilder {
  return gsonBuilder
      .registerTypeAdapter(POKEDEX_MAP_TYPE, PokedexDeserializer())
      .registerTypeAdapter(PokemonType::class.java, TypeDeserializer())
      .registerTypeAdapter(AbilitySet::class.java, AbilitySetDeserializer())
      .registerTypeAdapter(BASE_STATS_MAP_TYPE, BaseStatsDeserializer())
}

private class PokedexDeserializer: JsonDeserializer<Map<PokemonId, Species>> {
  override fun deserialize(
      json: JsonElement, expectedType: Type, context: JsonDeserializationContext):
      Map<PokemonId, Species> {
    val root = json.asJsonObject
    val battlePokedex = root["BattlePokedex"].asJsonObject
    return battlePokedex.entrySet()
        .map { deserializeSpecies(PokemonId(it.key), it.value.asJsonObject, context) }
        .filterNotNull()
        .associateBy { it.id }
  }
}

private fun deserializeSpecies(
    id: PokemonId, species: JsonObject, context: JsonDeserializationContext):
    Species? {
  val pokedexNumber = species["num"].asInt
  if (pokedexNumber <= 0) {
    // There are many nonstandard, invalid Pokemon with numbers <= 0.
    return null
  }

  val name = species["species"].asString
  val types = species["types"].asJsonArray
      .map { context.deserialize<PokemonType>(it, PokemonType::class.java) }
  val genderPossibilities = getGenderPossibilitiesFromSpeciesJson(species)
  val abilities = context.deserialize<AbilitySet>(species["abilities"], AbilitySet::class.java)
  val baseStats = context.deserialize<Map<Stat, Int>>(species["baseStats"], BASE_STATS_MAP_TYPE)
  val heightMm = species["heightm"].asBigDecimal.multiply(BigDecimal(1000)).intValueExact()
  val weightG = species["weightkg"].asBigDecimal.multiply(BigDecimal(1000)).intValueExact()

  val otherFormes = convertFormListToSet(species, "otherFormes")
  val form = species["forme"]?.asString

  return Species(
      id = id,
      name = name,
      number = pokedexNumber,
      types = types,
      gender = genderPossibilities,
      baseStats = baseStats,
      ability = abilities,
      heightmm = heightMm,
      weightg = weightG,
      otherForms = otherFormes,
      form = form
  )
}

private fun convertFormListToSet(species: JsonObject, key: String): Set<PokemonId> {
  if (!species.has(key)) {
    return setOf()
  }

  return species[key].asJsonArray
      .map { PokemonId(it.asString) }
      .toSet()
}

private class AbilitySetDeserializer: JsonDeserializer<AbilitySet> {
  override fun deserialize(
      json: JsonElement, expectedType: Type, context: JsonDeserializationContext):
      AbilitySet {
    val root = json.asJsonObject
    val first = AbilityId(root["0"].asString)
    val second = if (root.has("1")) AbilityId(root["1"].asString) else null
    val hidden = if (root.has("H")) AbilityId(root["H"].asString) else null
    return AbilitySet(first, second, hidden)
  }
}

private class BaseStatsDeserializer: JsonDeserializer<Map<Stat, Int>> {
  override fun deserialize(
      json: JsonElement, expectedType: Type, context: JsonDeserializationContext):
      Map<Stat, Int> {
    val root = json.asJsonObject
    return root.entrySet()
        .associate { Pair(StatFromAbbreviation(it.key, caseSensitive = false), it.value.asInt) }
  }
}

private val ALWAYS_GENDER_TO_POSSIBILITY_TABLE: Map<Gender, GenderPossibilities> = mapOf(
    Pair(Gender.MALE, GenderPossibilities.ALWAYS_MALE),
    Pair(Gender.FEMALE, GenderPossibilities.ALWAYS_FEMALE),
    Pair(Gender.GENDERLESS, GenderPossibilities.GENDERLESS)
)

fun getGenderPossibilitiesFromSpeciesJson(species: JsonObject): GenderPossibilities {
  if (species.has("gender")) {
    return ALWAYS_GENDER_TO_POSSIBILITY_TABLE[letterCodeToGender(species["gender"].asString)]!!
  }
  // The "gender ratio" field is omitted for 50-50 Pokemon.
  return GenderPossibilities.MALE_OR_FEMALE
}

fun letterCodeToGender(letterCode: String): Gender {
  when (letterCode) {
    "M" -> return Gender.MALE;
    "F" -> return Gender.FEMALE;
    "N" -> return Gender.GENDERLESS;
    else -> {
      throw IllegalArgumentException("Could not convert letter code '${letterCode}' to gender")
    }
  }
}