package com.nrook.kadabra.info

import com.nrook.kadabra.mechanics.EvSpread
import com.nrook.kadabra.mechanics.IvSpread
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.Nature
import com.nrook.kadabra.mechanics.PokemonSpec

/**
 * Represents a single Pokemon on a saved team.
 *
 * Why isn't this just [PokemonSpec]? This contains all of the information in a team builder text
 * file, but some stuff can be calculated on-the-fly by Showdown itself. For instance, in general
 * if you don't supply a fixed gender for your Pokemon, it is set at random.
 *
 * @property gender The Pokemon's gender. If null, no gender was supplied, and the real gender
 * will be selected at runtime. (For instance, if gender is omitted but the Pokemon is genderless
 * anyway, this will be [Gender.GENDERLESS], not null.
 */
data class TeamPokemon(
    val species: Species,
    val item: String,
    val ability: AbilityId,
    val gender: Gender?,
    val nature: Nature,
    val evSpread: EvSpread,
    val ivSpread: IvSpread,
    val level: Level,
    val moves: List<Move>
) {
  /**
   * @param actualGender The gender selected for this Pokemon in this particular game.
   *  If null, selects [Gender.FEMALE] if ambiguous.
   *  If set, the gender field must be either null or agree with the supplied parameter.
   */
  fun toSpec(actualGender: Gender? = null): PokemonSpec {
    val genderInSpec = selectGender(actualGender)

    return PokemonSpec(
        species, ability, genderInSpec, nature, evSpread, ivSpread, level, moves)
  }

  private fun selectGender(actualGender: Gender?): Gender {
    if (gender == null) {
      if (actualGender == null) {
        return species.gender.possibilities.sorted()[0]
      } else {
        if (species.gender.possibilities.contains(actualGender)) {
          return actualGender
        } else {
          throw IllegalArgumentException("Gender $actualGender invalid for ${species.name}")
        }
      }
    } else {
      if (actualGender != null && actualGender != gender) {
        throw IllegalArgumentException(
            "Supplied gender is $actualGender, but we already know this one is $gender")
      }
      return gender
    }
  }
}