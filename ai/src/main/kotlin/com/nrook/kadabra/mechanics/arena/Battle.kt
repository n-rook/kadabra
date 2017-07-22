package com.nrook.kadabra.mechanics.arena

import com.google.common.collect.Maps
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.Condition
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.formulas.Modifier
import com.nrook.kadabra.mechanics.formulas.computeDamage
import com.nrook.kadabra.mechanics.formulas.computeTypeEffectiveness
import com.nrook.kadabra.mechanics.newActivePokemonFromSpec
import com.nrook.kadabra.mechanics.newBenchedPokemonFromSpec
import mu.KLogging

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
        return beginningOfTurnCommands(player)
      }
      Phase.COMPUTE_TURN_ORDER -> {
        return listOf()
      }
      Phase.FIRST_ACTION -> {
        return listOf()
      }
      Phase.SECOND_ACTION -> {
        return listOf()
      }
      Phase.PRIORITY_BEFORE_END -> {
        return listOf()
      }
      Phase.END -> {
        val choosingSide = side(player)
        if (choosingSide.active.condition == Condition.FAINT) {
          return choosingSide.bench.keys.toList().map(::SwitchChoice)
        } else {
          return emptyList()
        }
      }
      Phase.FIRST_SWITCH_AFTER_FAINT -> {
        return listOf()
      }
      Phase.SECOND_SWITCH_AFTER_FAINT -> {
        return listOf()
      }
    }
  }

  private fun beginningOfTurnCommands(player: Player): List<Choice> {
    val side = side(player)
    val moves = side.active.moves.map(::MoveChoice)
    val switches = side.bench.keys.map { SwitchChoice(it) }
    return moves + switches
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

  internal fun clearChoices(): Battle {
    return Battle(turn, blackSide, whiteSide, null, null, phase, faster)
  }

  internal fun withFaster(fasterPlayer: Player): Battle {
    return Battle(turn, blackSide, whiteSide, blackChoice, whiteChoice, phase, fasterPlayer)
  }

  /**
   * Returns which Pokemon is faster. Does not include priority.
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
 * Initializes a battle, and simulates until the first choice.
 */
fun startBattle(blackTeam: List<PokemonSpec>, blackLead: Int,
                whiteTeam: List<PokemonSpec>, whiteLead: Int,
                context: BattleContext): Battle {
  val battle = Battle(
      1,
      teamSpecToSide(blackTeam, blackLead),
      teamSpecToSide(whiteTeam, whiteLead),
      null,
      null,
      Phase.BEGIN,
      null)
  context.logger.startOfTurnOverview(battle)
  return battle
}

/**
 * Converts a list of PokemonSpec objects to a Side.
 *
 * This does not adjudicate ETB effects for the lead Pokemon.
 */
private fun teamSpecToSide(team: List<PokemonSpec>, leadIndex: Int): Side {
  val lead = newActivePokemonFromSpec(team[leadIndex])
  val rest = team.slice(0..leadIndex - 1) + team.slice(leadIndex + 1..team.size - 1)
  val bench = Maps.uniqueIndex(rest.map { newBenchedPokemonFromSpec(it) }, { it!!.species.id })
  return Side(lead, bench)
}

/**
 * Simulate the battle until either it ends, or there's another choice to make.
 *
 * @param blackChoice The choice made by Black. If Black doesn't have to make a choice right now,
 *  null.
 * @param whiteChoice The choice made by White. If White doesn't have to make a choice right now,
 *  null.
 */
fun simulateBattle(battle: Battle, context: BattleContext, blackChoice: Choice?, whiteChoice: Choice?): Battle {
  validateChoice(battle, blackChoice, Player.BLACK)
  validateChoice(battle, whiteChoice, Player.WHITE)

  var battle = battle.withChoices(blackChoice, whiteChoice)
  do {
    battle = simulatePhase(battle, context)
  } while (battle.choices(Player.BLACK).isEmpty() && battle.choices(Player.WHITE).isEmpty())
  return battle
}

/**
 * Throw an exception if the given player can't make the given choice right now.
 */
internal fun validateChoice(battle: Battle, choice: Choice?, player: Player) {
  val validChoices = battle.choices(player)
  if (choice == null && validChoices.isNotEmpty()) {
    throw IllegalArgumentException("$player cannot make no choice right now; the following " +
        "choices are possible: $validChoices")
  }
  if (choice != null && !validChoices.contains(choice)) {
    throw IllegalArgumentException("$player cannot make choice $choice right now; the only " +
        "choices possible are $validChoices")
  }
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
      return recalculatePriority(battle, context).withPhase(Phase.FIRST_ACTION)
    }
    Phase.FIRST_ACTION -> {
      return takeAction(battle, context, battle.faster!!).withPhase(Phase.SECOND_ACTION)
    }
    Phase.SECOND_ACTION -> {
      return takeAction(battle, context, battle.faster!!.other())
          .withPhase(Phase.PRIORITY_BEFORE_END)
    }
    Phase.PRIORITY_BEFORE_END -> {
      return recalculatePriorityForEndOfTurnSwitch(battle, context).withPhase(Phase.END)
          .clearChoices()
    }
    Phase.END -> {
      return battle.withPhase(Phase.FIRST_SWITCH_AFTER_FAINT)
    }
    Phase.FIRST_SWITCH_AFTER_FAINT -> {
      val switcher = battle.faster!!
      if (battle.choiceOf(switcher) == null) {
        return battle.withPhase(Phase.SECOND_SWITCH_AFTER_FAINT)
      }

      return switchAfterFaint(battle, switcher, context)
          .withPhase(Phase.SECOND_SWITCH_AFTER_FAINT)
    }
    Phase.SECOND_SWITCH_AFTER_FAINT -> {
      val switcher = battle.faster!!.other()
      if (battle.choiceOf(switcher) == null) {
        return battle.withPhase(Phase.BEGIN).incrementTurn()
      }

      val nextTurn = switchAfterFaint(battle, switcher, context)
          .withPhase(Phase.BEGIN)
          .incrementTurn()
      context.logger.startOfTurnOverview(battle)
      return nextTurn
    }
  }
}

/**
 * Adjudicates an action taken by one player.
 *
 * This function does not update the phase.
 */
internal fun takeAction(battle: Battle, context: BattleContext, mover: Player): Battle {
  val choice = battle.choiceOf(mover)
  return when (choice) {
    is MoveChoice -> makeMove(battle, context, mover)
    is SwitchChoice -> makeSwitch(battle, context, mover)
    else -> throw IllegalArgumentException("Unknown choice type $choice")
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
  context.logger.useMove(mover, moveBeingExecuted.move, movingSide.active)

  if (moveBeingExecuted.move.basePower == 0) {
    logger.debug("Not simulating move ${moveBeingExecuted.move.id}; we don't understand it")
    return battle
  }

  if (!moveBeingExecuted.move.fullyRepresented) {
    logger.debug(
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
  context.logger.attack(mover, movingSide.active, otherSide.active, moveDamage)

  val newOpposingActivePokemon = otherSide.active.takeDamageAndMaybeFaint(moveDamage)

  val newOtherSide = otherSide.updateActivePokemon(newOpposingActivePokemon)

  return battle.withSide(mover.other(), newOtherSide)
}

/**
 * Adjudicates a switch ordered by a player at the beginning of a turn.
 */
internal fun makeSwitch(battle: Battle, context: BattleContext, mover: Player): Battle {
  val switch = battle.choiceOf(mover) as SwitchChoice
  val switchingSide = battle.side(mover)
  val switchedIn = switchingSide.bench[switch.target]
      ?: throw IllegalArgumentException(
          "Invalid switch target ${switch.target}; the bench is ${switchingSide.bench}")

  context.logger.switch(mover, switchingSide.active, switchedIn)

  val newActivePokemon = switchedIn.toActive()
  val newBenchedPokemon = switchingSide.active.toBenched()
  val updatedSwitchingSide = switchingSide.updateActivePokemon(newActivePokemon)
      .switch(switch.target, newActivePokemon, newBenchedPokemon)
  return battle.withSide(mover, updatedSwitchingSide)
}

/**
 * Recalculates and sets priority (that is, [Battle.faster]) based on the current state of the
 * battle.
 */
internal fun recalculatePriority(battle: Battle, context: BattleContext): Battle {
  val faster = calculateFasterAction(battle) ?:
      ((if (context.random.speedTieWinner()) Player.BLACK else Player.WHITE))
  return battle.withFaster(faster)
}

/**
 * Recalculates and sets [Battle.faster] based on which Pokemon is faster.
 *
 * Unlike [recalculatePriority], this function ignores the moves selected by players, since they
 * aren't relevant.
 */
internal fun recalculatePriorityForEndOfTurnSwitch(battle: Battle, context: BattleContext): Battle {
  val faster = battle.fasterSide() ?:
      ((if (context.random.speedTieWinner()) Player.BLACK else Player.WHITE))
  return battle.withFaster(faster)
}

/**
 * Computes which Pokemon should go first.
 *
 * @param battle A battle in the [Phase.COMPUTE_TURN_ORDER] phase, in which both players have
 *     selected an action. This method isn't suitable for calculating end-of-turn switch-after-faint
 *     priority.
 * @return the Pokemon who should go first. Null if both are tied.
 */
internal fun calculateFasterAction(battle: Battle): Player? {
  if (battle.phase != Phase.COMPUTE_TURN_ORDER) {
    throw IllegalArgumentException(
        "CalculateFasterAction does not work during the phase ${battle.phase}")
  }

  // First, see which action has higher priority.
  val blackPriority = calculatePriority(battle, Player.BLACK)
  val whitePriority = calculatePriority(battle, Player.WHITE)
  if (blackPriority > whitePriority) {
    return Player.BLACK
  } else if (whitePriority > blackPriority) {
    return Player.WHITE
  }

  // Both actions have the same priority, which means it comes down to speed.
  return battle.fasterSide()
}

/**
 * Calculates the priority of a given action.
 *
 * Priority is a numeric value associated with an action. Actions with higher priorities always go
 * before actions with lower priorities. This is how an attack like Quick Attack goes first.
 * Switching has very high priority, such that it almost always goes before moves.
 */
internal fun calculatePriority(battle: Battle, player: Player): Int {
  val choice = battle.choiceOf(player)!!
  if (choice is SwitchChoice) {
    return 6
  }
  return 0
}

internal fun switchAfterFaint(battle: Battle, switcher: Player, context: BattleContext): Battle {
  val switchingSide = battle.side(switcher)
  if (switchingSide.active.condition != Condition.FAINT) {
    throw IllegalArgumentException("Pokemon ${switchingSide.active} is not fainted")
  }

  val choice = battle.choiceOf(switcher) as SwitchChoice
  val switchTarget = switchingSide.bench[choice.target]!!
  context.logger.switchAfterFaint(switcher, switchingSide.active, switchTarget)

  val newActivePokemon = switchTarget.toActive()
  val updatedSwitchingSide = switchingSide.updateActivePokemon(newActivePokemon)
      .removeFromBench(choice.target)
  return battle.withSide(switcher, updatedSwitchingSide)
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
   * During this phase, trainers decide which moves to use.
   */
  BEGIN,

  /**
   * During this phase, Battle#faster is set.
   */
  COMPUTE_TURN_ORDER,

  /**
   * During this phase, the action of the faster Pokemon is adjudicated.
   */
  FIRST_ACTION,

  /**
   * During this phase, the action of the slower Pokemon is adjudicated. If
   * this Pokemon is fainted or otherwise out of action, this might not
   * happen.
   */
  SECOND_ACTION,

  /**
   * Before end-of-turn effects, priority is calculated. Again.
   */
  PRIORITY_BEFORE_END,

  /**
   * During this phase, end-of-turn effects occur. For instance, the effects of
   * Wish happen.
   *
   * At the end of this phase, trainers get to decide what to do with fainted Pokemon.
   */
  END,

  /**
   * The faster Pokemon gets the opportunity to switch out if fainted.
   */
  FIRST_SWITCH_AFTER_FAINT,

  /**
   * The slower Pokemon gets the opportunity to switch out if fainted.
   */
  SECOND_SWITCH_AFTER_FAINT

  // TODO: Consider adding a true 'end of turn' phase which clears choices.
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