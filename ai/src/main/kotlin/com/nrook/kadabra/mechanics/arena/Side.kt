package com.nrook.kadabra.mechanics.arena

import com.nrook.kadabra.info.PokemonId
import com.nrook.kadabra.mechanics.ActivePokemon
import com.nrook.kadabra.mechanics.BenchedPokemon
import com.nrook.kadabra.mechanics.Condition
import java.util.*

/**
 * One side of a battle.
 */
data class Side(
    val active: ActivePokemon,
    /**
     * A map from each benched Pokemon's original ID to their current state.
     *
     * Why not just a set? It's more convenient to update a map than it is to update a set.
     *
     * As of right now, fainted Pokemon are actually removed from the Side, not just benched.
     */
    val bench: Map<PokemonId, BenchedPokemon>
) {

  /**
   * Returns a new Side with a new active Pokemon.
   */
  fun updateActivePokemon(newActive: ActivePokemon): Side {
    return Side(newActive, bench)
  }

  fun removeFromBench(id: PokemonId): Side {
    val newBench = HashMap(bench)
    newBench.remove(id)!!  // Throw an exception if the ID isn't in the map
    return Side(active, newBench)
  }

  /**
   * Replace the active Pokemon with a benched Pokemon, and vice versa.
   *
   * @param switchedInId The ID of the Pokemon switched in (on the bench).
   * @param switchedIn The switched-in Pokemon.
   * @param switchedOut The benched Pokemon.
   */
  fun switch(switchedInId: PokemonId, switchedIn: ActivePokemon,
             switchedOut: BenchedPokemon): Side {
    val newBench = HashMap(bench)
    newBench.remove(switchedInId)!!
    newBench.put(switchedOut.species.id, switchedOut)
    return Side(switchedIn, newBench)
  }

  /**
   * Returns whether all Pokemon, both active and benched, have fainted.
   */
  fun allFainted(): Boolean {
    return active.condition == Condition.FAINT && bench.isEmpty()
  }
}

// The trouble with trying to build the Pokemon battle system is that
// I have no clue how to handle fainted Pokemon.
// I guess there would have to be an event queue of some sort.
// Doing basically anything would return more events.
// For instance, FAINTED would be an event.
