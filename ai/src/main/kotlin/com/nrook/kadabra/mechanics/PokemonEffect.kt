package com.nrook.kadabra.mechanics

/**
 * An effect which applies to a single Pokemon.
 *
 * These effects are all cleared when a Pokemon faints or switches out.
 */
interface PokemonEffect

/**
 * This class contains effects which are either on or off.
 */
enum class EffectFlag: PokemonEffect {
  /**
   * At the next opportunity, this Pokemon will switch out.
   *
   * This is applied after successfully using a move like U-Turn or Parting Shot.
   */
  SELF_SWITCH
}
