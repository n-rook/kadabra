package com.nrook.kadabra.mechanics.rng

/**
 * The policy used to determine the rules by which random numbers are generated.
 *
 * The idea is that for AI purposes, it may be best not to consider every possible outcome in many
 * cases. Instead of checking all 16 damage outcomes, for instance, we might want to check only 1
 * or 2. Similarly, we might want to ignore certain unlikely outcomes, like a 95% hit chance move
 * missing.
 */
data class RandomPolicy(val moveDamagePolicy: MoveDamagePolicy) {

}

/**
 * The policy used to determine the randomness inherent in move damage.
 */
enum class MoveDamagePolicy(val damageNumbers: List<Int>) {
  /**
   * All moves deal their average damage.
   *
   * Note: There are sixteen possible rolls, so the actual number provided is one below average.
   */
  ONE(listOf(92)),

  /**
   * All moves deal either 1/4 or 3/4 of the way through their damage range.
   */
  TWO(listOf(89, 96)),

  /**
   * The full damage range is used.
   */
  FULL((85..100).toList())
}

val REALISTIC_RANDOM_POLICY = RandomPolicy(MoveDamagePolicy.FULL)
