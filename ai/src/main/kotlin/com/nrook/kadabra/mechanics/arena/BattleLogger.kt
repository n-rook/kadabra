package com.nrook.kadabra.mechanics.arena

import com.nrook.kadabra.info.Move
import com.nrook.kadabra.mechanics.ActivePokemon
import com.nrook.kadabra.mechanics.BenchedPokemon

/**
 * An interface for logging events which occur in battle.
 */
interface BattleLogger {

  /**
   * Log that a move was used.
   */
  fun useMove(player: Player, move: Move, pokemon: ActivePokemon)

  /**
   * Log that a damaging move was made.
   */
  fun attack(attackingPlayer: Player, attackingPokemon: ActivePokemon,
             defendingPokemon: ActivePokemon, damage: Int)

  /**
   * Log that at the end of the turn, after a Pokemon fainted, a new Pokemon was brought out.
   */
  fun switchAfterFaint(
      switchingPlayer: Player, faintedPokemon: ActivePokemon, newPokemon: BenchedPokemon)

}