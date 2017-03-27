package com.nrook.kadabra.mechanics.rng

import java.util.*

/**
 * A Pokemon-specific, policy-based random number generator.
 */
class RandomNumberGenerator(val policy: RandomPolicy, val random: Random) {
  /**
   * Returns an integer between 85 and 100, inclusive, for use in the Pokemon damage roll.
   */
  fun moveDamage(): Int {
    val damageNumbers = policy.moveDamagePolicy.damageNumbers
    return damageNumbers[random.nextInt(damageNumbers.size)]
  }

  /**
   * Returns who should win a speed tie.
   *
   * TODO: Actually return BLACK or WHITE.
   */
  fun speedTieWinner(): Boolean {
    return random.nextBoolean()
  }
}
