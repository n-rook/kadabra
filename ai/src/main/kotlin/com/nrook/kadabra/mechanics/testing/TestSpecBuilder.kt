package com.nrook.kadabra.mechanics.testing

import com.google.common.collect.ImmutableMap
import com.nrook.kadabra.info.*
import com.nrook.kadabra.mechanics.*

/**
 * A test utility for creating PokemonSpecs.
 */
class TestSpecBuilder private constructor(
    private val pokedex: Pokedex,
    private val species: Species,
    private var ability: AbilityId,
    private var gender: Gender,
    private var nature: Nature,
    private var ev: EvSpread,
    private var iv: IvSpread,
    private var level: Level,
    private var moves: List<Move>

) {
  companion object Factory {
    fun create(pokedex: Pokedex, species: String): TestSpecBuilder {
      val speciesData = pokedex.getSpeciesByName(species)
      val gender = when(speciesData.gender) {
        GenderPossibilities.MALE_OR_FEMALE -> Gender.FEMALE
        GenderPossibilities.ALWAYS_MALE -> Gender.MALE
        GenderPossibilities.ALWAYS_FEMALE -> Gender.FEMALE
        GenderPossibilities.GENDERLESS -> Gender.GENDERLESS
      }

      return TestSpecBuilder(
          pokedex,
          speciesData,
          speciesData.ability.first,
          gender,
          Nature.HARDY,
          NO_EVS,
          MAX_IVS,
          Level(100),
          listOf())
    }
  }

  fun withNature(nature: Nature): TestSpecBuilder {
    this.nature = nature
    return this
  }

  fun withMoves(vararg moves: String): TestSpecBuilder {
    this.moves = moves.map { pokedex.getMoveById(MoveId(it)) }
    return this
  }

  /**
   * Sets a 252/252/4 EV spread for this Pokemon.
   */
  fun withEvSpread(firstMaxedStat: Stat, secondMaxedStat: Stat, leftover: Stat): TestSpecBuilder {
    this.ev = makeEvs(ImmutableMap.of(firstMaxedStat, 252, secondMaxedStat, 252, leftover, 4))
    return this
  }

  fun build(): PokemonSpec {
    if (moves.isEmpty()) {
      throw IllegalStateException("You didn't select any moves!")
    }

    return PokemonSpec(
        species,
        ability,
        gender,
        nature,
        ev,
        iv,
        level,
        moves
    )
  }
}