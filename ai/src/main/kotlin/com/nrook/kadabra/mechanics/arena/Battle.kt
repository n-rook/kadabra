package com.nrook.kadabra.mechanics.arena

import com.nrook.kadabra.common.resolveRange
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.Condition
import com.nrook.kadabra.mechanics.formulas.Modifier
import com.nrook.kadabra.mechanics.formulas.computeDamage
import com.nrook.kadabra.mechanics.formulas.computeDamageRange
import com.nrook.kadabra.mechanics.formulas.computeTypeEffectiveness
import com.nrook.kadabra.mechanics.rng.RandomNumberGenerator
import mu.KLogging
import java.util.*

val logger = KLogging().logger()

// https://web.archive.org/web/20140422133645/http://www.upokecenter.com/content/pokemon-black-version-and-pokemon-white-version-timing-notes

/**
 * Maintains the current state of a battle.
 *
 * Class rules:
 * Trivial mutations and features are kept in the class.
 * More complex operations are kept outside the class.
 */
data class Battle(
    val turn: Int,
    val blackSide: Side,
    val whiteSide: Side,
    val blackChoice: Choice?,
    val whiteChoice: Choice?,

    val phase: Phase,
    val faster: Player?
) {

  fun side(player: Player): Side {
    return when (player) {
      Player.BLACK -> blackSide
      Player.WHITE -> whiteSide
    }
  }

  fun choiceOf(player: Player): Choice? {
    return when (player) {
      Player.BLACK -> blackChoice
      Player.WHITE -> whiteChoice
    }
  }

  /**
   * Returns the choices available to both Pokemon.
   *
   * If it isn't an appropriate time for either player to make a choice, returns the empty list
   * for both players.
   */
  fun choices(player: Player): List<Choice> {
    when (this.phase) {
      Phase.BEGIN -> {
        return side(player).active.moves.map(::MoveChoice)
      }
      Phase.COMPUTE_TURN_ORDER -> {
        return listOf()
      }
      Phase.FIRST_ATTACK -> {
        return listOf()
      }
      Phase.SECOND_ATTACK -> {
        return listOf()
      }
      Phase.END -> {
        return listOf()
      }
    }
  }

  /**
   * If someone has won, return the winner.
   */
  fun winner(): Player? {
    val blackFainted = blackSide.allFainted()
    val whiteFainted = whiteSide.allFainted()

    if (blackFainted && whiteFainted) {
      throw IllegalStateException("Both sides have fainted. Unfortunately, this exposes a "
          + "flaw in our simulation; in the real game, it's not possible for both sides to lose.")
    } else if (blackFainted) {
      return Player.WHITE
    } else if (whiteFainted) {
      return Player.BLACK
    } else {
      return null
    }
  }

  internal fun incrementTurn(): Battle {
    return Battle(turn + 1, blackSide, whiteSide, blackChoice, whiteChoice, phase, faster)
  }

  internal fun withSide(player: Player, side: Side): Battle {
    return if (player == Player.BLACK)
      Battle(turn, side, whiteSide, blackChoice, whiteChoice, phase, faster)
    else Battle(turn, blackSide, side, blackChoice, whiteChoice, phase, faster)
  }

  internal fun withPhase(phase: Phase): Battle {
    return Battle(turn, blackSide, whiteSide, blackChoice, whiteChoice, phase, faster)
  }

  internal fun withChoices(blackChoice: Choice?, whiteChoice: Choice?): Battle {
    return Battle(turn, blackSide, whiteSide, blackChoice, whiteChoice, phase, faster)
  }

  internal fun withFaster(fasterPlayer: Player): Battle {
    return Battle(turn, blackSide, whiteSide, blackChoice, whiteChoice, phase, fasterPlayer)
  }

  /**
   * Returns which Pokemon deserves to go first.
   *
   * Returns null if there is a speed tie.
   */
  internal fun fasterSide(): Player? {
    val blackSpeed: Int = blackSide.active.getStat(Stat.SPEED)
    val whiteSpeed: Int = whiteSide.active.getStat(Stat.SPEED)

    if (blackSpeed > whiteSpeed) {
      return Player.BLACK
    } else if (whiteSpeed > blackSpeed) {
      return Player.WHITE
    } else {
      return null
    }
  }
}

/**
 * Simulate the battle until either it ends, or there's another choice to make.
 */
fun simulateBattle(battle: Battle, context: BattleContext, blackChoice: Choice, whiteChoice: Choice): Battle {
  var battle = battle.withChoices(blackChoice, whiteChoice)
  do {
    battle = simulatePhase(battle, context)
  } while (battle.choices(Player.BLACK).isEmpty() && battle.choices(Player.WHITE).isEmpty())
  return battle
}

/**
 * Simulate a phase of this battle. Returns the next phase.
 */
internal fun simulatePhase(battle: Battle, context: BattleContext): Battle {
  when (battle.phase) {
    Phase.BEGIN -> {
      return battle.withPhase(Phase.COMPUTE_TURN_ORDER)
    }
    Phase.COMPUTE_TURN_ORDER -> {
      return recalculatePriority(battle, context).withPhase(Phase.FIRST_ATTACK)
    }
    Phase.FIRST_ATTACK -> {
      return makeMove(battle, context, battle.faster!!).withPhase(Phase.SECOND_ATTACK)
    }
    Phase.SECOND_ATTACK -> {
      return makeMove(battle, context, battle.faster!!.other()).withPhase(Phase.END)
    }
    Phase.END -> {
      return battle.incrementTurn().withPhase(Phase.BEGIN).withChoices(null, null)
    }
  }
}

/**
 * Adjudicates a move by one player's Pokemon.
 *
 * This function does not update the phase.
 */
internal fun makeMove(battle: Battle, context: BattleContext, mover: Player): Battle {
  val movingSide = battle.side(mover)
  val otherSide = battle.side(mover.other())
  val choiceBeingExecuted = battle.choiceOf(mover)

  if (battle.side(mover).active.condition == Condition.FAINT) {
    // No move: the Pokemon is fainted.
    logger.debug("Skipping move for player $mover; their Pokemon is fainted.")
    return battle
  }

  if (choiceBeingExecuted == null) {
    throw IllegalStateException(
        "State does not make sense; trying to make a move for player $mover, but "
            + "their choice is null")
  }
  if (choiceBeingExecuted !is MoveChoice) {
    logger.debug("Skipping move for player $mover; they chose not to move, but to do a {}",
        choiceBeingExecuted.javaClass.name)
  }

  val moveBeingExecuted: MoveChoice = choiceBeingExecuted as MoveChoice

  if (moveBeingExecuted.move.basePower == 0) {
    logger.info("Not simulating move ${moveBeingExecuted.move.id}; we don't understand it")
    return battle
  }

  if (!moveBeingExecuted.move.fullyUnderstood) {
    logger.info(
        "Simulating move ${moveBeingExecuted.move.id} even though we don't fully understand it")
  }

  val effectiveness = computeTypeEffectiveness(
      moveBeingExecuted.move.type,
      otherSide.active.species.types)

  val offensiveStat = movingSide.active.getStat(moveBeingExecuted.move.category.offensiveStat())
  val defensiveStat = otherSide.active.getStat(moveBeingExecuted.move.category.defensiveStat())

  val modifiers: MutableSet<Modifier> = mutableSetOf()
  if (movingSide.active.species.types.contains(moveBeingExecuted.move.type)) {
    modifiers.add(Modifier.STAB)
  }

  val moveDamage = computeDamage(
      movingSide.active.originalSpec.level,
      offensiveStat = offensiveStat,
      defensiveStat = defensiveStat,
      movePower = moveBeingExecuted.move.basePower,
      effectiveness = effectiveness,
      damageRoll = context.random.moveDamage(),
      modifiers = modifiers)

  val newOpposingActivePokemon = otherSide.active.takeDamageAndMaybeFaint(moveDamage)

  val newOtherSide = otherSide.updateActivePokemon(newOpposingActivePokemon)

  return battle.withSide(mover.other(), newOtherSide)
}

/**
 * Recalculates and sets priority (that is, [Battle.faster]) based on the current state of the
 * battle.
 */
internal fun recalculatePriority(battle: Battle, context: BattleContext): Battle {
  return battle.withFaster(battle.fasterSide() ?:
      (if (context.random.speedTieWinner()) Player.BLACK else Player.WHITE))
}

// Phases:
// SWITCH PHASE, where Pursuit and a switch may happen
// MEGA EVOLUTION PHASE
// COMPUTE TURN ORDER
// FIRST ATTACK PHASE
// SECOND ATTACK PHASE
// END OF TURN UPKEEP PHASE

/**
 * The individual parts of each turn. Listed in order.
 */
enum class Phase {
  /**
   * This phase is a marker, so we don't have to change the handling if we add more phases.
   * Nothing actually happens in BEGIN phase, but when trainers are picking moves at the beginning
   * of their turn, that should be during BEGIN phase.
   */
  BEGIN,

  /**
   * During this phase, Battle#faster is set.
   */
  COMPUTE_TURN_ORDER,

  /**
   * During this phase, the attack of the faster Pokemon is adjudicated.
   */
  FIRST_ATTACK,

  /**
   * During this phase, the attack of the slower Pokemon is adjudicated. If
   * this Pokemon is fainted or otherwise out of action, this might not
   * happen.
   */
  SECOND_ATTACK,

  /**
   * During this phase, end-of-turn effects occur. For instance, the effects of
   * Wish happen.
   */
  END
}

/**
 * The index of each player.
 *
 * As in Go, Black is the first player, and White the second.
 */
enum class Player {
  BLACK,
  WHITE;

  fun other(): Player {
    return if (this == BLACK) WHITE else BLACK
  }
}