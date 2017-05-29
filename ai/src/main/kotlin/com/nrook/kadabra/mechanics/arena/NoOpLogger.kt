package com.nrook.kadabra.mechanics.arena

import com.nrook.kadabra.info.Move
import com.nrook.kadabra.mechanics.ActivePokemon
import com.nrook.kadabra.mechanics.BenchedPokemon

/**
 * A BattleLogger which does nothing and logs no messages whatsoever.
 */
class NoOpLogger: BattleLogger {

  override fun useMove(player: Player, move: Move, pokemon: ActivePokemon) {}

  override fun attack(
      attackingPlayer: Player,
      attackingPokemon: ActivePokemon,
      defendingPokemon: ActivePokemon,
      damage: Int) {}

  override fun switch(
      switchingPlayer: Player,
      switchedOut: ActivePokemon,
      switchedIn: BenchedPokemon) {}

  override fun switchAfterFaint(
      switchingPlayer: Player,
      faintedPokemon: ActivePokemon,
      newPokemon: BenchedPokemon) {}
}