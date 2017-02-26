package com.nrook.kadabra.common

import java.util.*

/**
 * Pick a random value in the given IntRange.
 */
fun resolveRange(random: Random, range: IntRange): Int {
  val exclusiveBound = range.endInclusive - range.start + 1
  val randomValue = random.nextInt(exclusiveBound)
  return range.start + randomValue
}