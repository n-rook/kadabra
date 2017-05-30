package com.nrook.kadabra.ai.perfect

import com.google.common.collect.ArrayTable
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Table
import mu.KLogging
import java.util.*

private val logger = KLogging().logger()

// All credit for this section goes to Thomas Ferguson's book, Game Theory.
// https://www.math.ucla.edu/~tom/Game_Theory/mat.pdf

/**
 * Find the best strategy for the player of a zero-sum game in normal form.
 *
 * @param outcomes A table whose rows represent possible choices by us, and whose columns represent
 *     possible choices by our opponent. The cells in the table represent the outcomes for us of
 *     each decision. Each cell's value must be between 0 and 1.
 */
fun <R, C> findBestStrategy(outcomes: Table<R, C, Double>): MixedStrategy<R> {
  return findBestStrategies(ArrayTable.create(outcomes)).player1Strategy
}

fun <R, C> findBestStrategies(outcomes: ArrayTable<R, C, Double>): StrategyPair<R, C> {
  try {
    val firstTableau = Tableau.create(makeTablePositive(outcomes))
    return solveTableau(firstTableau)
  } catch (e: Exception) {
    logger.error("Failed to compute optimal strategy.\n$outcomes\n", e)
    throw e
  }
}

/**
 * Represents a pair of strategies for a game, one for each player.
 */
data class StrategyPair<R, C>(
    val player1Strategy: MixedStrategy<R>, val player2Strategy: MixedStrategy<C>)

interface RowOrColumnLabel<R, C>
/**
 * A label that was originally a row label.
 */
data class RowLabel<R, C>(val label: R): RowOrColumnLabel<R, C>

/**
 * A label that was originally a column label.
 */
data class ColumnLabel<R, C>(val label: C): RowOrColumnLabel<R, C>

internal fun <R, C> makeTablePositive(game: ArrayTable<R, C, Double>): ArrayTable<R, C, Double> {
  // The algorithm only works if the game's value is positive: that is, if on average,
  // something good happens for both players. So, we ensure this by adding a number to each cell
  // to ensure each value is positive.
  val tableWithConstantAdded = ArrayTable.create(game)

  val minimumOutcome = game.values().min()!!
  val addend = -minimumOutcome + 1.0

  for (i in 0 until tableWithConstantAdded.rowKeyList().size) {
    for (j in 0 until tableWithConstantAdded.columnKeyList().size) {
      tableWithConstantAdded.set(i, j, game.at(i, j) + addend)
    }
  }
  return tableWithConstantAdded
}

internal class Tableau<R, C> private constructor(
    val arrayTable: ArrayTable<Int, Int, Double>,
    val rowLabels: ImmutableList<RowOrColumnLabel<R, C>>,
    val columnLabels: ImmutableList<RowOrColumnLabel<R, C>>) {
  companion object Factory {
    fun <R, C> create(originalOutcomesTable: ArrayTable<R, C, Double>):
        Tableau<R, C> {
      val m = originalOutcomesTable.rowKeyList().size
      val n = originalOutcomesTable.columnKeyList().size

      val newTable: ArrayTable<Int, Int, Double> = ArrayTable.create(0..m, 0..n)

      for (r in 0 until m) {
        for (c in 0 until n) {
          newTable.set(r, c, originalOutcomesTable.at(r, c))
        }
      }

      // Set augmented values in the last column.
      for (r in 0 until m) {
        newTable.set(r, n, 1.0)
      }

      // Set augmented values in the last row.
      for (c in 0 until n) {
        newTable.set(m, c, -1.0)
      }

      // Finally, set the bottom-right corner.
      newTable.set(m, n, 0.0)

      val rowLabels: ImmutableList<RowOrColumnLabel<R, C>> = ImmutableList.copyOf(
          originalOutcomesTable.rowKeyList().map { RowLabel<R, C>(it) })
      val columnLabels: ImmutableList<RowOrColumnLabel<R, C>> = ImmutableList.copyOf(
          originalOutcomesTable.columnKeyList().map { ColumnLabel<R, C>(it) })

      return Tableau(newTable, rowLabels, columnLabels)
    }
  }

  /**
   * The number of row labels.
   *
   * In other words, one less than the number of rows.
   */
  val m: Int
    get() = rowLabels.size

  /**
   * The number of column labels.
   *
   * In other words, one less than the number of columns.
   */
  val n: Int
    get() = columnLabels.size

  fun clone(): Tableau<R, C> {
    return Tableau(ArrayTable.create(arrayTable), rowLabels, columnLabels)
  }

  /**
   * Exchange the label for the row p with the label for the row q.
   *
   * This method is safe, but it's also inefficient. It should be possible to replace it with a
   * version that mutates this instance.
   */
  fun safeExchangeLabels(p: Int, q: Int): Tableau<R, C> {
    val newRowLabels = ArrayList(rowLabels)
    val newColumnLabels = ArrayList(columnLabels)

    val rowLabelToBeSwapped = rowLabels[p]
    val columnLabelToBeSwapped = columnLabels[q]
    newRowLabels[p] = columnLabelToBeSwapped
    newColumnLabels[q] = rowLabelToBeSwapped

    return Tableau(ArrayTable.create(arrayTable),
        ImmutableList.copyOf(newRowLabels),
        ImmutableList.copyOf(newColumnLabels))
  }
}

internal fun tableauHasNegativeNumbersInTheBottomRow(t: Tableau<*, *>): Boolean {
  for (j in 0..t.n) {
    if (t.arrayTable.at(t.m, j) < 0.0) {
      return true
    }
  }
  return false
}

internal fun <R, C> solveTableau(original: Tableau<R, C>): StrategyPair<R, C> {
  var t = original
  while (tableauHasNegativeNumbersInTheBottomRow(t)) {
    t = pivotOnce(t)
  }

  return readOutOptimalStrategiesForEachPlayer(t)
}

internal fun <R, C> pivotOnce(t: Tableau<R, C>): Tableau<R, C> {
  val pivot = selectPivot(t)
  val updatedCells = adjustCellsByPivot(t, pivot)
  return updatedCells.safeExchangeLabels(pivot.first, pivot.second)
}

internal fun selectPivot(t: Tableau<*, *>): Pair<Int, Int> {
  // The pivot must be subject to these properties:
  // The number at the bottom of the pivot column must be negative.
  // The pivot itself must be positive.
  // The pivot row must be chosen as such: Let p be the value of the pivot.
  // Let b be the number at the right of the pivot row. Then b/p must be the
  // minimum of each possible pivot value.

  // First, pick the pivot column.
  var q: Int? = null
  for (j in 0 until t.n) {
    val borderValue = t.arrayTable.at(t.m, j)
    if (borderValue < 0.0) {
      q = j
      break
    }
  }
  q ?: throw IllegalStateException("Could not find a pivot column. This should be impossible.")

  // Next, pick the pivot row.
  var p: Int? = null
  var smallestRatioSoFar: Double = Double.POSITIVE_INFINITY
  for (i in 0 until t.m) {
    val prospectivePivot = t.arrayTable.at(i, q)
    if (prospectivePivot <= 0.0) {
      continue
    }

    val borderValue = t.arrayTable.at(i, t.n)
    val ratio = borderValue / prospectivePivot

    if (ratio < smallestRatioSoFar) {
      p = i
      smallestRatioSoFar = ratio
    }
  }
  p ?: throw IllegalStateException("Could not find a pivot row. This should be impossible.")

  return Pair(p, q)
}

internal fun <R, C> adjustCellsByPivot(t: Tableau<R, C>, pivot: Pair<Int, Int>): Tableau<R, C> {
  // The pivot row.
  val p = pivot.first
  // The pivot column.
  val q = pivot.second

  val tn = t.clone()
  // We adjust the values of the tableau as follows:
  // Let (p, q) be the pivot, and let v be its value.
  // 1. The pivot value, v, is replaced by its reciprocal.
  // 2. Each entry in the pivot row, except the pivot, is divided by v.
  // 3. Each entry in the pivot column, except the pivot, is divided by v, then negated.
  // 4. For all other entries (i, j), we replace t[i, j] with
  //    t[i, j] - t[p, j] * t[i, q] / v.

  val v = t.arrayTable.at(p, q)

  // Replace the pivot with its reciprocal.
  tn.arrayTable.set(p, q, 1 / v)

  // Divide each entry in the pivot row, except the pivot, by v.
  for (j in 0..t.n) {
    if (j == q) {
      continue
    }
    tn.arrayTable.set(p, j, t.arrayTable.at(p, j) / v)
  }

  // Replace each entry in the pivot column, except the pivot, with the negative of its value
  // divided by the pivot value.
  for (i in 0..t.m) {
    if (i == p) {
      continue
    }
    tn.arrayTable.set(i, q, -t.arrayTable.at(i, q) / v)
  }

  // For all other entries (i, j), replace their value x with
  // x - t[p, j] * t[i, q] / v.
  for (i in (0..t.m).filter { it != p }) {
    for (j in (0..t.n).filter { it != q }) {
      val newValue = t.arrayTable.at(i, j) -
          t.arrayTable.at(p, j) * t.arrayTable.at(i, q) / v
      tn.arrayTable.set(i, j, newValue)
    }
  }

  return tn
}

internal fun <R, C> readOutOptimalStrategiesForEachPlayer(t: Tableau<R, C>): StrategyPair<R, C> {
  // Player 1's optimal strategy is as follows:
  // Player 1's labels which end up on the left side get probability 0.
  // Player 1's labels which end up on the top receive their value on the bottom edge,
  // divided by the lower right hand corner.

  val divisor = t.arrayTable.at(t.m, t.n)
  val player1StrategyBuilder: ImmutableMap.Builder<R, Double> = ImmutableMap.builder()
  for ((j, columnLabel) in t.columnLabels.withIndex()) {
    if (columnLabel is RowLabel<R, C>) {
      val weight = t.arrayTable.at(t.m, j) / divisor
      player1StrategyBuilder.put(columnLabel.label, weight)
    }
  }
  val player1Strategy = MixedStrategy<R>(player1StrategyBuilder.build())

  // Player 2's optimal strategy is as follows:
  // Player 2's labels which wind up on the top get probability 0.
  // Player 2's labels which wind up on the left receive their value on the right edge,
  // divided by the lower right hand corner.

  val player2StrategyBuilder: ImmutableMap.Builder<C, Double> = ImmutableMap.builder()
  for ((i, rowLabel) in t.rowLabels.withIndex()) {
    if (rowLabel is ColumnLabel<R, C>) {
      val weight = t.arrayTable.at(i, t.n) / divisor
      player2StrategyBuilder.put(rowLabel.label, weight)
    }
  }
  val player2Strategy = MixedStrategy<C>(player2StrategyBuilder.build())

  return StrategyPair(player1Strategy, player2Strategy)
}