package com.nrook.kadabra.ai.perfect

import com.google.common.collect.Table

/**
 * Find the best strategy for the player of a zero-sum game in normal form.
 *
 * @param outcomes A table whose rows represent possible choices by us, and whose columns represent
 *     possible choices by our opponent. The cells in the table represent the outcomes for us of
 *     each decision. Each cell's value must be between 0 and 1.
 */
fun <R, C> findBestStrategy(outcomes: Table<R, C, Double>): MixedStrategy<R> {
  // http://cedar.wwu.edu/cgi/viewcontent.cgi?article=1002&context=computerscience_stupubs
  // https://www.math.ucla.edu/~tom/Game_Theory/mat.pdf

  // TODO: This is basically placeholder logic.
  // Unfortunately, it appears to be nontrivial to go from a table of expected outcomes to an
  // optimal mixed strategy. This logic, which just finds the best strategy assuming the opponent
  // is a moron who plays a mixed strategy where they play every choice evenly, is highly suspect,
  // so we must revisit it soon.

  var bestOutcome = 0.0
  val bestChoices = mutableListOf<R>()
  for (choice in outcomes.rowKeySet()) {
    val averageOutcome = outcomes.row(choice)
        .values
        .average()
    if (averageOutcome > bestOutcome) {
      bestOutcome = averageOutcome
      bestChoices.clear()
      bestChoices.add(choice)
    } else if (averageOutcome == bestOutcome) {
      bestChoices.add(choice)
    }
  }

  return MixedStrategy.createEvenStrategy(bestChoices)
}
