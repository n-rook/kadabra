package com.nrook.kadabra.mechanics.arena

import com.nrook.kadabra.mechanics.rng.RandomNumberGenerator

/**
 * Contains utilities and tools useful for battle simulation.
 *
 * The distinction between this and a Simulator/AI type class is that BattleContext should be
 * passed around within battle simulation tasks: that is, Battle calls BattleContext. For instance,
 * the RNG is in BattleContext, because during simulation it's often useful to get an RNG result.
 */
class BattleContext(val random: RandomNumberGenerator)
