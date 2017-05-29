package com.nrook.kadabra.ai.perfect

import com.google.common.collect.ArrayTable
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameSolvingTest {

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
  fun rejectsObviouslyBadStrategy() {
    val outcomes = makeTable(listOf("good", "bad"), listOf("foe"),
        listOf(1.0),
        listOf(0.1))
    val strategy = findBestStrategy(outcomes)
    assertThat(strategy).isEqualTo(MixedStrategy.createPureStrategy("good"))
  }


}