package com.nrook.kadabra.inference

import com.google.common.base.Converter
import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.Maps
import com.nrook.kadabra.mechanics.arena.Player

/**
 * A converter for going from "p1" to [Player.BLACK] and from "p2" to [Player.WHITE].
 */
val ID_TO_PLAYER_CONVERTER: Converter<String, Player> = Maps.asConverter(ImmutableBiMap.of(
    "p1", Player.BLACK,
    "p2", Player.WHITE))

