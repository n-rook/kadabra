package com.nrook.kadabra.info

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.read.getGen7Pokedex
import com.nrook.kadabra.mechanics.EvSpread
import com.nrook.kadabra.mechanics.IvSpread
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.Nature
import com.nrook.kadabra.mechanics.makeEvs
import org.junit.Before
import org.junit.Test

class TeamPokemonTest {
  lateinit var pokedex: Pokedex

  @Before
  fun setUp() {
    pokedex = getGen7Pokedex()
  }

  @Test
  fun toAndFromSpecProto() {
    val evs = ImmutableMap.of(Stat.SPECIAL_ATTACK, 252, Stat.SPEED, 252)
    val ivBuilder = ImmutableMap.builder<Stat, Int>()
    ivBuilder.put(Stat.ATTACK, 0)
    for (stat in Stat.values().filter { it != Stat.ATTACK }) {
      ivBuilder.put(stat, 31)
    }
    val ivs = ivBuilder.build()

    val teamPokemon = TeamPokemon(
        species = pokedex.getSpeciesByName("Alakazam"),
        item = "Leftovers",
        ability = AbilityId("Synchronize"),
        gender = null,
        nature = Nature.MODEST,
        evSpread = makeEvs(evs),
        ivSpread = IvSpread(ivs),
        level = Level(95),
        moves = ImmutableList.of(
            pokedex.getMoveByName("Psychic")
        )
    )

    val teamPokemonProto = teamPokemon.toSpecProto()

    val result = TeamPokemon.fromSpecProto(pokedex, teamPokemonProto)
    assertThat(result.species.name).isEqualTo("Alakazam")
    assertThat(result.item).isEqualTo("Leftovers")
    assertThat(result.ability).isEqualTo(AbilityId("Synchronize"))
    assertThat(result.gender).isNull()
    assertThat(result.nature).isEqualTo(Nature.MODEST)
    assertThat(result.evSpread.values).isEqualTo(makeEvs(evs).values)
    assertThat(result.ivSpread.values).isEqualTo(ivs)
    // TODO: We don't serialize or use level on the wire...
    assertThat(result.level).isEqualTo(Level.MAX)
    assertThat(result.moves).hasSize(1)
    assertThat(result.moves[0].name).isEqualTo("Psychic")
  }
}