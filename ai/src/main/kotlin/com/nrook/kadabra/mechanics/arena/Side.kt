package com.nrook.kadabra.mechanics.arena

import com.nrook.kadabra.mechanics.ActivePokemon
import com.nrook.kadabra.mechanics.Condition

/**
 * One side of a battle.
 */
data class Side(
    val active: ActivePokemon
) {

  /**
   * Returns a new Side with a new active Pokemon.
   */
  fun updateActivePokemon(active: ActivePokemon): Side {
    return Side(active)
  }

  /**
   * Returns whether all Pokemon, both active and benched, have fainted.
   */
  fun allFainted(): Boolean {
    return active.condition == Condition.FAINT
  }
}

// The trouble with trying to build the Pokemon battle system is that
// I have no clue how to handle fainted Pokemon.
// I guess there would have to be an event queue of some sort.
// Doing basically anything would return more events.
// For instance, FAINTED would be an event.