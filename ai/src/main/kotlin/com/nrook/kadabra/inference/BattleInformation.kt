package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableMap
import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.PokemonId
import com.nrook.kadabra.proto.Player

/**
 * Contains what information we know about an entire battle.
 */
data class BattleInformation(
    val turn: Int,
    val sides: ImmutableMap<Player, SideInformation>
)

/**
 * Contains what information we know about one side of a battle.
 */
data class SideInformation(
    val active: ActivePokemonInformation,
    val bench: ImmutableMap<PokemonId, BenchedPokemonInformation>
)

/**
 * Contains what information we know about an active Pokemon.
 */
data class ActivePokemonInformation(
    val hp: HpInformation,
    val knownMoves: List<Move>
)

data class BenchedPokemonInformation(
    val hp: HpInformation
)

/**
 * Contains what information we know about a Pokemon's HP.
 */
data class HpInformation(
    val realHp: Int?,
    val realMaxHp: Int?,
    val percentHp: Int?)
