package com.nrook.kadabra.mechanics

import com.nrook.kadabra.info.Species
import com.nrook.kadabra.info.Stat

data class BenchedPokemon(
    val species: Species,
    val originalSpec: PokemonSpec,
    val hp: Int,
    val condition: Condition
) {

  /**
   * Return this Pokemon as an active Pokemon.
   *
   * Converts the data only; this call does not active enter-the-battle effects.
   */
  fun toActive(): ActivePokemon {
    return ActivePokemon(species, originalSpec, hp, condition)
  }
}

fun newBenchedPokemonFromSpec(spec: PokemonSpec): BenchedPokemon {
  return BenchedPokemon(spec.species, spec, spec.getStat(Stat.HP), Condition.OK)
}
