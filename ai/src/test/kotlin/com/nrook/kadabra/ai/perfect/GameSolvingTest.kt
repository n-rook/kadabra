package com.nrook.kadabra.ai.perfect

import com.google.common.collect.ArrayTable
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameSolvingTest {

  val ROCK_PAPER_SCISSORS = makeTable(
      listOf("rock", "paper", "scissors"),
      listOf("rock", "paper", "scissors"),
      listOf(0.5, 0.0, 1.0),
      listOf(1.0, 0.5, 0.0),
      listOf(0.0, 1.0, 0.5))

  fun setRows(t: ArrayTable<String, String, Double>, vararg rows: List<Double>) {
    for (c in 0 until t.columnKeyList().size) {
      for (r in 0 until t.rowKeyList().size) {
        t.set(r, c, rows[r][c])
      }
    }
  }

  fun makeTable(rowKeys: List<String>, columnKeys: List<String>, vararg rows: List<Double>)
      : ArrayTable<String, String, Double> {
    val table = ArrayTable.create<String, String, Double>(rowKeys, columnKeys)
    setRows(table, *rows)
    return table
  }

  @Test
  fun constructTableauWorksAsExpected() {
    val originalTableau = Tableau.create(ROCK_PAPER_SCISSORS)

    assertThat(originalTableau.rowLabels)
        .containsExactly(
            RowLabel<String, String>("rock"),
            RowLabel<String, String>("paper"),
            RowLabel<String, String>("scissors"))
        .inOrder()
    assertThat(originalTableau.columnLabels)
        .containsExactly(
            ColumnLabel<String, String>("rock"),
            ColumnLabel<String, String>("paper"),
            ColumnLabel<String, String>("scissors"))
        .inOrder()

    val table = originalTableau.arrayTable
    assertThat(table.rowKeyList()).containsExactly(0, 1, 2, 3)
    assertThat(table.columnKeyList()).containsExactly(0, 1, 2, 3)

    assertThat(table).containsCell(0, 0, 0.5)
    assertThat(table).containsCell(1, 0, 1.0)
    assertThat(table).containsCell(2, 0, 0.0)
    assertThat(table).containsCell(0, 1, 0.0)
    assertThat(table).containsCell(1, 1, 0.5)
    assertThat(table).containsCell(2, 1, 1.0)
    assertThat(table).containsCell(0, 2, 1.0)
    assertThat(table).containsCell(1, 2, 0.0)
    assertThat(table).containsCell(2, 2, 0.5)

    // The values in the border column should all be 1.
    assertThat(table).containsCell(0, 3, 1.0)
    assertThat(table).containsCell(1, 3, 1.0)
    assertThat(table).containsCell(2, 3, 1.0)

    // The values in the border row should all be -1.
    assertThat(table).containsCell(3, 0, -1.0)
    assertThat(table).containsCell(3, 1, -1.0)
    assertThat(table).containsCell(3, 2, -1.0)

    // The value in the lower right corner should be 0.
    assertThat(table).containsCell(3, 3, 0.0)
  }

  @Test
  fun rejectsObviouslyBadStrategy() {
    val outcomes = makeTable(listOf("good", "bad"), listOf("foe"),
        listOf(1.0),
        listOf(0.1))
    val strategy = findBestStrategy(outcomes)
    assertThat(strategy).isEqualTo(MixedStrategy.createPureStrategy("good"))
  }

  @Test
  fun solvesRockPaperScissors() {
    val outcomes = makeTable(
        listOf("rock", "paper", "scissors"),
        listOf("rock", "paper", "scissors"),
        listOf(0.5, 0.0, 1.0),
        listOf(1.0, 0.5, 0.0),
        listOf(0.0, 1.0, 0.5))
    val strategy = findBestStrategy(outcomes)

    assertThat(strategy.choices).containsKey("rock")
    assertThat(strategy.choices).containsKey("paper")
    assertThat(strategy.choices).containsKey("scissors")

    assertThat(strategy.choices["rock"]).isWithin(0.01).of(0.333)
    assertThat(strategy.choices["scissors"]).isWithin(0.01).of(0.333)
    assertThat(strategy.choices["paper"]).isWithin(0.01).of(0.333)
  }

  @Test
  fun solvesTwoTreasuresGame() {
    // In this game, player 1, the thief, is stealing from player 2, the guard.
    // There are two treasures. The first is worth 0.5, the second worth 1.0.
    // The guard has to pick which treasure to guard, and the thief gets 0.0 if
    // they try to steal from the place the guard is hanging out in.

    // Optimal play is for the thief to target the less valuable treasure twice as often as the more
    // valuable treasure. That way, they make out the same no matter which treasure the guard
    // defends.

    val outcomes = makeTable(
        listOf("target silver", "target gold"),
        listOf("defend silver", "defend gold"),
        listOf(0.0, 0.5),
        listOf(1.0, 0.0))
    val strategy = findBestStrategy(outcomes)
    assertThat(strategy.getChance("target silver")).isWithin(0.01).of(0.667)
    assertThat(strategy.getChance("target gold")).isWithin(0.01).of(0.333)
  }

  @Test
  fun solvesExampleFromGameTheoryFergusonBook() {
    val outcomes = makeTable(
        listOf("#1", "#2", "#3"),
        listOf("#1", "#2", "#3"),
        listOf(4.0, 1.0, 8.0),
        listOf(2.0, 3.0, 1.0),
        listOf(0.0, 4.0, 3.0))

    val strategy = findBestStrategy(outcomes)
    assertThat(strategy.getChance("#1")).isWithin(0.01).of(0.25)
    assertThat(strategy.getChance("#2")).isWithin(0.01).of(0.75)
    assertThat(strategy.getChance("#3")).isWithin(0.01).of(0.0)
  }

  @Test
  fun doesNotCrashWhenGivenHopelessGame() {
    val outcomes = makeTable(
        listOf("play on", "resign"),
        listOf("win", "throw"),
        listOf(0.0, 1.0),
        listOf(0.0, 0.0))
    val strategy = findBestStrategy(outcomes)
    assertThat(strategy.choices.values.sum()).isWithin(0.01).of(1.0)
  }
}
