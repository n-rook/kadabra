package com.nrook.kadabra.mechanics.arena

import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.ActivePokemon
import com.nrook.kadabra.mechanics.BenchedPokemon
import mu.KLogging
import java.io.Writer

/**
 * A battle logger that logs to some text stream.
 */
class TextLogger(val writer: Writer): BattleLogger {
  override fun useMove(player: Player, move: Move, pokemon: ActivePokemon) {
    writer.write("${format(player)} ${format(pokemon)} used ${move.id.str}.")
  }

  override fun attack(
      attackingPlayer: Player,
      attackingPokemon: ActivePokemon,
      defendingPokemon: ActivePokemon,
      damage: Int) {
    writer.write("${format(attackingPlayer)} ${format(attackingPokemon)} dealt $damage damage to " +
        format(defendingPokemon))
  }

  override fun switchAfterFaint(switchingPlayer: Player, faintedPokemon: ActivePokemon, newPokemon: BenchedPokemon) {
    writer.write("${format(switchingPlayer)} After ${format(faintedPokemon)} fainted, " +
        "${format(newPokemon)} was brought out")
  }
}

fun debugLogger(): TextLogger {
  return TextLogger(KLoggingWriter(KLogging().logger()))
}

private fun format(player: Player): String {
  return when(player) {
    Player.BLACK -> "[B]"
    Player.WHITE -> "[W]"
  }
}

private fun format(activePokemon: ActivePokemon): String {
  return formatPokemon(activePokemon.species.name, activePokemon.hp, activePokemon.getStat(Stat.HP))
}

private fun format(benchedPokemon: BenchedPokemon): String {
  return formatPokemon(benchedPokemon.species.name, benchedPokemon.hp,
      benchedPokemon.originalSpec.getStat(Stat.HP))
}

private fun formatPokemon(name: String, currentHp: Int, maxHp: Int): String {
  return "$name ($currentHp/$maxHp)"
}