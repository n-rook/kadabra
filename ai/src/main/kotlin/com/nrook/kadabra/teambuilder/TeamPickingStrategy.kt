package com.nrook.kadabra.teambuilder

import com.nrook.kadabra.info.PokemonDefinition

/**
 * Defines a technique for picking a team for battle.
 */
interface TeamPickingStrategy {
  fun pick() : List<PokemonDefinition>
}