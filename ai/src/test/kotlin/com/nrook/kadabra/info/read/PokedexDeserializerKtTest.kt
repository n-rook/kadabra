package com.nrook.kadabra.info.read

import com.google.common.io.Resources
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nrook.kadabra.info.*
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

class PokedexDeserializerKtTest {

  val CHARIZARD = PokemonId("charizard")
  val AUDINO = PokemonId("audino")
  val PIDGEOT = PokemonId("pidgeot")
  val TAUROS = PokemonId("tauros")
  val BLISSEY = PokemonId("blissey")
  val PORYGON2 = PokemonId("porygon2")
  val GROUDON = PokemonId("groudon")
  val MEGA_SALAMENCE = PokemonId("salamencemega")

  lateinit var gson: Gson
  lateinit var dataset: Map<PokemonId, Species>

  @Before
  fun setUp() {
    this.gson = registerPokedexDeserializers(GsonBuilder())
        .create()
    val resource = Resources.getResource("gen7pokedex.json")

    dataset = gson.fromJson(InputStreamReader(resource.openStream()), POKEDEX_MAP_TYPE)
  }

  @Test
  fun works() {
    assertThat(dataset).containsKey(CHARIZARD)
    val charizard = dataset[CHARIZARD]!!

    assertThat(charizard.id).isEqualTo(CHARIZARD)
    assertThat(charizard.number).isEqualTo(6)
  }

  @Test
  fun types() {
    assertThat(dataset[CHARIZARD]!!.types).containsExactly(PokemonType.FIRE, PokemonType.FLYING)
        .inOrder()
    assertThat(dataset[AUDINO]!!.types).containsExactly(PokemonType.NORMAL)
  }

  @Test
  fun gender() {
    assertThat(dataset[CHARIZARD]!!.gender).isEqualTo(GenderPossibilities.MALE_OR_FEMALE)
    assertThat(dataset[PIDGEOT]!!.gender).isEqualTo(GenderPossibilities.MALE_OR_FEMALE)
    assertThat(dataset[TAUROS]!!.gender).isEqualTo(GenderPossibilities.ALWAYS_MALE)
    assertThat(dataset[BLISSEY]!!.gender).isEqualTo(GenderPossibilities.ALWAYS_FEMALE)
    assertThat(dataset[PORYGON2]!!.gender).isEqualTo(GenderPossibilities.GENDERLESS)
  }

  @Test
  fun baseStats() {
    assertThat(dataset).containsKey(CHARIZARD)
    val charizard = dataset[CHARIZARD]!!

    assertThat(charizard.baseStats).containsEntry(Stat.HP, 78)
    assertThat(charizard.baseStats).containsEntry(Stat.ATTACK, 84)
    assertThat(charizard.baseStats).containsEntry(Stat.DEFENSE, 78)
    assertThat(charizard.baseStats).containsEntry(Stat.SPECIAL_ATTACK, 109)
    assertThat(charizard.baseStats).containsEntry(Stat.SPECIAL_DEFENSE, 85)
    assertThat(charizard.baseStats).containsEntry(Stat.SPEED, 100)
  }

  @Test
  fun abilities0() {
    val expected = AbilitySet(AbilityId("Drought"), null, null)

    assertThat(dataset[GROUDON]!!.ability).isEqualTo(expected)
  }

  @Test
  fun abilities0H() {
    val expected = AbilitySet(AbilityId("Blaze"), null, AbilityId("Solar Power"))

    assertThat(dataset[CHARIZARD]!!.ability).isEqualTo(expected)
  }

  @Test
  fun abilities01H() {
    val expected = AbilitySet(
        AbilityId("Keen Eye"), AbilityId("Tangled Feet"), AbilityId("Big Pecks"))

    assertThat(dataset[PIDGEOT]!!.ability).isEqualTo(expected)
  }

  @Test
  fun otherFormes() {
    assertThat(dataset[CHARIZARD]!!.otherForms)
        .containsExactly(PokemonId("charizardmegax"), PokemonId("charizardmegay"))
  }

  @Test
  fun noOtherFormes() {
    assertThat(dataset[TAUROS]!!.otherForms).isEmpty()
  }

  @Test
  fun form() {
    assertThat(dataset[MEGA_SALAMENCE]!!.form).isEqualTo("Mega")
  }

  @Test
  fun noForm() {
    assertThat(dataset[PIDGEOT]!!.form).isNull()
  }
}