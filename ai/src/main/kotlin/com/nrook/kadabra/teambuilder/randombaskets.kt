package com.nrook.kadabra.teambuilder

import com.google.common.collect.ImmutableSortedMap
import java.util.*

/**
 * A basket of elements from which one can be picked at random. Each element has a weight, which governs its chance
 * of being picked.
 *
 * @param weightMap A map from doubles to elements of the basket. The probability of each element being picked is equal
 * to the difference between its key and the previous key in the list. For instance, if the first element's key is
 * 40, and the second's is 60, the first element will be picked twice as often as the second.
 */
class RandomBasket<out T>
    private constructor(private val random: Random, private val weightMap: NavigableMap<Double, T>) {

  companion object Factory {

    /**
     * Create a RandomBasket.
     *
     * @param elements The elements which will populate the random basket.
     * @param weightFunction A function from elements to weights. Cannot return negative values: those do not make sense
     * as weights.
     * @param chanceFloor The minimum chance of an element being picked. Those elements whose weights are so low that,
     * were no chance floor provided, they would have less than this chance of being picked, will be omitted from the
     * distribution.
     */
    fun <T> create(random: Random, elements: Collection<T>, weightFunction: (T) -> Double, chanceFloor: Double):
        RandomBasket<T> {
      for (e in elements) {
        val weight = weightFunction(e)
        if (weight < 0) {
          throw IllegalArgumentException("Element $e had unacceptably low weight $weight")
        }
      }

      if (chanceFloor < 0 || chanceFloor > 1) {
        throw IllegalArgumentException("Illegal chance floor $chanceFloor")
      }

      val elementsWhichPassBar = cropUnlikelyElements(elements, weightFunction, chanceFloor)

      val weightMap: MutableMap<Double, T> = mutableMapOf()
      var totalSoFar: Double = 0.0
      for (e in elementsWhichPassBar) {
        val weight = weightFunction(e)
        totalSoFar += weight
        weightMap.put(totalSoFar, e)
      }

      return RandomBasket(random, ImmutableSortedMap.copyOf(weightMap))
    }
  }

  fun pick(): T {
    val index = random.nextDouble() * weightMap.lastKey()
    // Pick the least element whose weight exceeds index.
    return weightMap.ceilingEntry(index).value
  }
}

private fun <T> cropUnlikelyElements(elements: Collection<T>, weightFunction: (T) -> Double, chanceFloor: Double):
    Set<T> {
  val total = elements.sumByDouble(weightFunction)

  return elements.filter {
    val weight = weightFunction(it)
    val chanceOfBeingPicked = weight / total
    chanceOfBeingPicked >= chanceFloor
  }.toSet()
}