package com.nrook.kadabra.info

import com.google.common.collect.ImmutableMap
import com.nrook.kadabra.mechanics.EvSpread
import com.nrook.kadabra.mechanics.IvSpread
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.Nature
import com.nrook.kadabra.mechanics.PokemonSpec

private fun protoEvSpreadConverter(evSpread: com.nrook.kadabra.proto.EvSpread): EvSpread {
  return EvSpread(ImmutableMap.builder<Stat, Int>()
      .put(Stat.HP, evSpread.hp)
      .put(Stat.ATTACK, evSpread.attack)
      .put(Stat.DEFENSE, evSpread.defense)
      .put(Stat.SPECIAL_ATTACK, evSpread.specialAttack)
      .put(Stat.SPECIAL_DEFENSE, evSpread.specialDefense)
      .put(Stat.SPEED, evSpread.speed)
      .build())
}

private fun protoIvSpreadConverter(ivSpread: com.nrook.kadabra.proto.IvSpread): IvSpread {
  return IvSpread(ImmutableMap.builder<Stat, Int>()
      .put(Stat.HP, ivSpread.hp)
      .put(Stat.ATTACK, ivSpread.attack)
      .put(Stat.DEFENSE, ivSpread.defense)
      .put(Stat.SPECIAL_ATTACK, ivSpread.specialAttack)
      .put(Stat.SPECIAL_DEFENSE, ivSpread.specialDefense)
      .put(Stat.SPEED, ivSpread.speed)
      .build())
}

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
  companion object {
    fun fromSpecProto(pokedex: Pokedex, spec: com.nrook.kadabra.proto.PokemonSpec): TeamPokemon {
      return TeamPokemon(
          species = pokedex.getSpeciesByName(spec.species),
          item = spec.item,
          ability = AbilityId(spec.ability),
          gender = null,  // PokemonSpec currently does not have gender
          nature = Nature.valueOf(spec.nature.name),
          evSpread = protoEvSpreadConverter(spec.evs),
          ivSpread = protoIvSpreadConverter(spec.ivs),
          level = Level.MAX,
          moves = spec.moveList.map { pokedex.getMoveByName(it) }
      )
    }
  }

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

  fun toSpecProto(): com.nrook.kadabra.proto.PokemonSpec {
    val evs: com.nrook.kadabra.proto.EvSpread = com.nrook.kadabra.proto.EvSpread.newBuilder()
        .setHp(this.evSpread[Stat.HP])
        .setAttack(this.evSpread[Stat.ATTACK])
        .setDefense(this.evSpread[Stat.DEFENSE])
        .setSpecialAttack(this.evSpread[Stat.SPECIAL_ATTACK])
        .setSpecialDefense(this.evSpread[Stat.SPECIAL_DEFENSE])
        .setSpeed(this.evSpread[Stat.SPEED])
        .build()

    val ivs: com.nrook.kadabra.proto.IvSpread = com.nrook.kadabra.proto.IvSpread.newBuilder()
        .setHp(this.ivSpread[Stat.HP])
        .setAttack(this.ivSpread[Stat.ATTACK])
        .setDefense(this.ivSpread[Stat.DEFENSE])
        .setSpecialAttack(this.ivSpread[Stat.SPECIAL_ATTACK])
        .setSpecialDefense(this.ivSpread[Stat.SPECIAL_DEFENSE])
        .setSpeed(this.ivSpread[Stat.SPEED])
        .build()

    return com.nrook.kadabra.proto.PokemonSpec.newBuilder()
        .setSpecies(this.species.name)
        .setItem(this.item)
        .setAbility(this.ability.str)
        .setEvs(evs)
        .setIvs(ivs)
        .setNature(com.nrook.kadabra.proto.Nature.valueOf(this.nature.name))
        .addAllMove(this.moves.map { it.name })
        .build()
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