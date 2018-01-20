package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableTable
import com.google.common.collect.Ordering
import com.nrook.kadabra.info.AbilityId
import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.ActivePokemon
import com.nrook.kadabra.mechanics.BenchedPokemon
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.MAX_IVS
import com.nrook.kadabra.mechanics.NO_EVS
import com.nrook.kadabra.mechanics.Nature
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.arena.Battle
import com.nrook.kadabra.mechanics.arena.Choice
import com.nrook.kadabra.mechanics.arena.MoveChoice
import com.nrook.kadabra.mechanics.arena.Phase
import com.nrook.kadabra.mechanics.arena.Player
import com.nrook.kadabra.mechanics.arena.Side
import com.nrook.kadabra.usage.MoveUsageData
import com.nrook.kadabra.usage.PokemonUsageData
import com.nrook.kadabra.usage.UsageDataset

fun constructBattle(info: OngoingBattle, events: ImmutableList<BattleEvent>, pokedex: Pokedex, dataset: UsageDataset): Battle {
  val ourSide = constructOurSide(info)
  val theirSide = constructTheirSide(info, pokedex, dataset)

  val blackSide = if (info.us == Player.BLACK) ourSide else theirSide
  val whiteSide = if (info.us == Player.WHITE) ourSide else theirSide

  return when (info.phase!!) {
    DecisionPhase.BEGIN -> Battle(
        turn = info.turn,
        blackSide = blackSide,
        whiteSide = whiteSide,
        choices = ImmutableTable.of(),
        faster = null,
        phase = Phase.BEGIN
    )
    DecisionPhase.END -> Battle(
        turn = info.turn,
        blackSide = blackSide,
        whiteSide = whiteSide,
        choices = ImmutableTable.of(),
        faster = null,  // TODO: INACCURATE! Fix by moving PRIORITY_BEFORE_END after END
        phase = Phase.END
    )
    DecisionPhase.FIRST_MOVE_SWITCH -> {
      info.faster ?: throw IllegalArgumentException(
          "OngoingBattle faster info was null, but we should know who is faster, since someone " +
              "has already used a switch move")
      if (info.faster != info.us) {
        throw IllegalArgumentException("Combination of state and question makes no sense; " +
            "we shouldn't get a decision point if our opponent is switching out")
      }

      // Our model fits this case exceptionally poorly.
      // The opponent has already made a decision, but now we need to make a decision without
      // knowing what they've decided!
      // In theory, we should construct our decision matrix by figuring out what move they would
      // have liked back at the start of the turn.
      // For now, let's just arbitrarily pick a move.
      val arbitraryMove = theirSide.active.moves[0]
      val choiceMatrix: ImmutableTable<Player, Phase, Choice> =
          ImmutableTable.of(info.us.other(), Phase.BEGIN, MoveChoice(arbitraryMove))

      return Battle(
          turn = info.turn,
          blackSide = blackSide,
          whiteSide = whiteSide,
          choices = choiceMatrix,
          faster = info.faster,
          phase = Phase.FIRST_MOVE_SWITCH
      )
    }
    DecisionPhase.SECOND_MOVE_SWITCH -> {
      info.faster ?: throw IllegalArgumentException(
          "OngoingBattle faster info was null, but we should know who is faster, since someone " +
              "has already used a switch move")
      if (info.faster == info.us) {
        throw IllegalArgumentException("Combination of state and question makes no sense; " +
            "we shouldn't get a decision point if our opponent is switching out")
      }

      return Battle(
          turn = info.turn,
          blackSide = blackSide,
          whiteSide = whiteSide,
          // This doesn't really make sense, but that's okay, all the move-based decisions should
          // have already been made.
          choices = ImmutableTable.of(),
          faster = info.faster,
          phase = Phase.SECOND_MOVE_SWITCH
      )
    }
  }
}

fun constructOurSide(info: OngoingBattle): Side {
  // Start with active Pokemon
  // TODO: I don't think active Pokemon should ever actually be null

  val active = info.ourSide.active!!
  val translatedActive = ActivePokemon(
      active.species,
      active.originalSpec,
      active.hp,
      active.condition,
      effects = ImmutableSet.of())

  val bench = info.ourSide.bench.map {
    BenchedPokemon(
        it.species,
        it.originalSpec,
        it.hp,
        it.condition)
  }

  return Side(translatedActive, bench)
}

/**
 * Try to figure out the Pokemon our opponent has on their side.
 */
fun constructTheirSide(info: OngoingBattle, pokedex: Pokedex, dataset: UsageDataset): Side {
  val active = info.theirSide.active!!
  val activeUsageData = dataset.data[active.species.name] ?:
      throw IllegalStateException("Could not find usage data for Pokemon ${active.species.name}")

  val activeSpec = PokemonSpec(
      active.species,
      getMostFrequentAbility(activeUsageData, pokedex),
      active.gender,
      Nature.HARDY,
      NO_EVS,
      MAX_IVS,
      Level(100),
      getMostFrequentMoves(activeUsageData, pokedex))

  val translatedActive = ActivePokemon(
      active.species,
      activeSpec,
      getHpFromFraction(active.hp, activeSpec.getStat(Stat.HP)),
      active.condition,
      ImmutableSet.of())

  val bench = info.theirSide.bench.map {
    val usageData = dataset.data[it.species.name] ?:
        throw IllegalStateException("Could not find usage data for Pokemon ${it.species.name}")

    val benchSpec = PokemonSpec(
        it.species,
        getMostFrequentAbility(usageData, pokedex),
        it.gender,
        Nature.HARDY,
        NO_EVS,
        MAX_IVS,
        Level(100),
        getMostFrequentMoves(usageData, pokedex))

    BenchedPokemon(
        it.species,
        benchSpec,
        getHpFromFraction(it.hp, benchSpec.getStat(Stat.HP)),
        it.condition)
  }

  return Side(translatedActive, bench)
}


private fun getMostFrequentAbility(usage: PokemonUsageData, pokedex: Pokedex): AbilityId {
  return pokedex.getAbilityByUsageCode(usage.abilities.max()!!.ability)
}

private fun getMostFrequentMoves(usage: PokemonUsageData, pokedex: Pokedex): ImmutableList<Move> {
  val moves = usage.moves
  val top4 = Ordering.natural<MoveUsageData>().greatestOf(moves.values, 4)
  return ImmutableList.copyOf(top4.map { pokedex.getMoveByUsageCode(it.move) })
}

private fun getHpFromFraction(fraction: HpFraction, max: Int): Int {
  // TODO: This is probably marginally inaccurate.
  return fraction.numerator * max / fraction.denominator
}
