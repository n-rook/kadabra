package com.nrook.kadabra.mechanics.arena

import com.google.common.collect.ImmutableList
import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.PokemonId


/**
 * The choice a player makes to kick off a turn.
 */
interface Choice

/**
 * A choice of move.
 */
data class MoveChoice(val move: Move): Choice {
  override fun toString(): String {
    return "[${move.id.str}]"
  }
}

/**
 * A choice to switch to another Pokemon.
 *
 * This choice represents all sorts of switches: during combat, before the end of the turn, or
 * after the end of the turn.
 *
 * @param target The original ID of the Pokemon to be switched in. For instance, if switching into
 *  Mega Garchomp, this is generally going to be "garchomp".
 */
data class SwitchChoice(val target: PokemonId): Choice {
  companion object {

    /**
     * Returns SwitchChoices for each Pokemon on this side's bench.
     */
    fun forSide(s: Side): ImmutableList<SwitchChoice> {
      return ImmutableList.copyOf(s.bench.keys.map { SwitchChoice(it) })
    }
  }

  override fun toString(): String {
    return "[-> ${target.str}]"
  }
}
