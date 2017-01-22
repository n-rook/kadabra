package com.nrook.kadabra.info

import com.nrook.kadabra.proto.Nature
import com.nrook.kadabra.proto.PokemonSpec

/**
 * Represents a single Pokemon on a team.
 */
class PokemonDefinition(val spec : PokemonSpec) {
  val species: String
    get() = this.spec.species

  val item : String
    get() = this.spec.item

  val ability : String
    get() = this.spec.ability

  val evs : Map<Stat, Int>
    get() = mapOf(
        Stat.HP to spec.evs.hp,
        Stat.ATTACK to spec.evs.attack,
        Stat.DEFENSE to spec.evs.defense,
        Stat.SPECIAL_ATTACK to spec.evs.specialAttack,
        Stat.SPECIAL_DEFENSE to spec.evs.specialDefense,
        Stat.SPEED to spec.evs.speed)

  val ivs: Map<Stat, Int>
    get() = mapOf(
        Stat.HP to spec.ivs.hp,
        Stat.ATTACK to spec.ivs.attack,
        Stat.DEFENSE to spec.ivs.defense,
        Stat.SPECIAL_ATTACK to spec.ivs.specialAttack,
        Stat.SPECIAL_DEFENSE to spec.ivs.specialDefense,
        Stat.SPEED to spec.ivs.speed)

  val nature: Nature
    get() = this.spec.nature

  val moves: List<String>
    get() = this.spec.moveList
}
