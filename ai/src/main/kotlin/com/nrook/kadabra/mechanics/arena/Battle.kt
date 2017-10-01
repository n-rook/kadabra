package com.nrook.kadabra.mechanics.arena

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableTable
import com.google.common.collect.Maps
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.Condition
import com.nrook.kadabra.mechanics.EffectFlag
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
 *
 * @property choices A table from a player and a phase to the choice they made in that phase.
 *    Most cells are empty.
 */
data class Battle(
    val turn: Int,
    val blackSide: Side,
    val whiteSide: Side,
    val choices: ImmutableTable<Player, Phase, Choice>,

    val phase: Phase,
    val faster: Player?
) {

  fun side(player: Player): Side {
    return when (player) {
      Player.BLACK -> blackSide
      Player.WHITE -> whiteSide
    }
  }

  fun move(player: Player): Choice? {
    return choices[player, Phase.BEGIN]
  }

  /**
   * Returns the choices available to both Pokemon.
   *
   * If it isn't an appropriate time for either player to make a choice, returns the empty list
   * for both players.
   */
  fun choices(player: Player): List<Choice> {
    if (!this.phase.choice) {
      return listOf()
    }

    when (this.phase) {
      Phase.BEGIN -> {
        return beginningOfTurnCommands(player)
      }
      Phase.FIRST_MOVE_SWITCH -> {
        return moveSwitchChoices(this, player)
      }
      Phase.SECOND_MOVE_SWITCH -> {
        return moveSwitchChoices(this, player)
      }
      Phase.END -> {
        val choosingSide = side(player)
        if (choosingSide.active.condition == Condition.FAINT) {
          return choosingSide.bench.keys.toList().map(::SwitchChoice)
        } else {
          return emptyList()
        }
      }
      else -> {
        throw RuntimeException("This should never happen")
      }
    }
  }

  private fun beginningOfTurnCommands(player: Player): List<Choice> {
    val side = side(player)
    val moves = side.active.moves.map(::MoveChoice)
    val switches = SwitchChoice.forSide(side)
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
    return Battle(turn + 1, blackSide, whiteSide, choices, phase, faster)
  }

  internal fun withSide(player: Player, side: Side): Battle {
    return if (player == Player.BLACK)
      Battle(turn, side, whiteSide, choices, phase, faster)
    else
      Battle(turn, blackSide, side, choices, phase, faster)
  }

  internal fun withPhase(phase: Phase): Battle {
    return Battle(turn, blackSide, whiteSide, choices, phase, faster)
  }

  internal fun incrementPhase(): Battle {
    return withPhase(this.phase.next())
  }

  internal fun withChoices(blackChoice: Choice?, whiteChoice: Choice?): Battle {
    val newChoices: ImmutableTable.Builder<Player, Phase, Choice> = ImmutableTable.builder()
    newChoices.putAll(choices)

    fun addChoice(player: Player, choice: Choice?) {
      if (choice != null) {
        if (choices.contains(player, phase)) {
          throw IllegalStateException(
              "$player already made a choice, ${choices[player, phase]}. " +
                  "They can't make a second choice, $choice.")
        }
        newChoices.put(player, phase, choice)
      }
    }
    addChoice(Player.BLACK, blackChoice)
    addChoice(Player.WHITE, whiteChoice)

    return Battle(turn, blackSide, whiteSide, newChoices.build(), phase, faster)
  }

  internal fun clearChoices(): Battle {
    return Battle(turn, blackSide, whiteSide, ImmutableTable.of(), phase, faster)
  }

  internal fun withFaster(fasterPlayer: Player): Battle {
    return Battle(turn, blackSide, whiteSide, choices, phase, fasterPlayer)
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
      ImmutableTable.of(),
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
    throw IllegalArgumentException("$player cannot make no choice during ${battle.phase}; the " +
        "following choices are possible: $validChoices")
  }
  if (choice != null && !validChoices.contains(choice)) {
    throw IllegalArgumentException("$player cannot make choice $choice right now; the only " +
        "choices possible are $validChoices")
  }
}

internal fun moveSwitchChoices(battle: Battle, player: Player): ImmutableList<Choice> {
  val side = battle.side(player)
  if (!side.active.effects.contains(EffectFlag.SELF_SWITCH)) {
    return ImmutableList.of()
  }

  val ourTurn = when (battle.phase) {
    Phase.FIRST_MOVE_SWITCH -> battle.faster == player
    Phase.SECOND_MOVE_SWITCH -> battle.faster != player
    else -> throw IllegalArgumentException("Bad phase ${battle.phase}")
  }

  if (ourTurn && !side.bench.isEmpty()) {
    // If you're the only Pokemon left, switch effects don't trigger.
    return SwitchChoice.forSide(side) as ImmutableList<Choice>
  }
  return ImmutableList.of()
}

/**
 * Simulate a phase of this battle. Returns the next phase.
 */
internal fun simulatePhase(battle: Battle, context: BattleContext): Battle {
  when (battle.phase) {
    Phase.BEGIN -> {
      return battle.incrementPhase()
    }
    Phase.COMPUTE_TURN_ORDER -> {
      return recalculatePriority(battle, context).incrementPhase()
    }
    Phase.FIRST_ACTION -> {
      return takeAction(battle, context, battle.faster!!).incrementPhase()
    }
    Phase.FIRST_MOVE_SWITCH -> {
      return makeAfterMoveSwitch(battle, context, battle.faster!!)
          .incrementPhase()
    }
    Phase.SECOND_ACTION -> {
      return takeAction(battle, context, battle.faster!!.other())
          .incrementPhase()
    }
    Phase.SECOND_MOVE_SWITCH -> {
      return makeAfterMoveSwitch(battle, context, battle.faster!!.other())
          .incrementPhase()
    }
    Phase.PRIORITY_BEFORE_END -> {
      return recalculatePriorityForEndOfTurnSwitch(battle, context).incrementPhase()
    }
    Phase.END -> {
      return battle.incrementPhase()
    }
    Phase.FIRST_SWITCH_AFTER_FAINT -> {
      val switcher = battle.faster!!
      if (battle.side(switcher).active.condition != Condition.FAINT) {
        return battle.incrementPhase()
      }

      return switchAfterFaint(battle, switcher, context)
          .incrementPhase()
    }
    Phase.SECOND_SWITCH_AFTER_FAINT -> {
      val switcher = battle.faster!!.other()
      if (battle.side(switcher).active.condition != Condition.FAINT) {
        return battle.incrementPhase()
            .incrementTurn()
            .clearChoices()
      }

      val nextTurn = switchAfterFaint(battle, switcher, context)
          .incrementPhase()
          .incrementTurn()
          .clearChoices()
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
  val choice = battle.move(mover)
  return when (choice) {
    is MoveChoice -> makeMove(battle, context, mover)
    is SwitchChoice -> makeSwitch(battle, context, mover, choice)
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
  val choiceBeingExecuted = battle.move(mover)

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

  val move = (choiceBeingExecuted as MoveChoice).move
  context.logger.useMove(mover, move, movingSide.active)

  if (move.basePower == 0) {
    logger.debug("Not simulating move ${move.id}; we don't understand it")
    return battle
  }

  if (!move.fullyUnderstood()) {
    logger.debug(
        "Simulating move ${move.id} even though we don't fully understand it")
  }

  val effectiveness = computeTypeEffectiveness(
      move.type,
      otherSide.active.species.types)

  val offensiveStat = movingSide.active.getStat(move.category.offensiveStat())
  val defensiveStat = otherSide.active.getStat(move.category.defensiveStat())

  val modifiers: MutableSet<Modifier> = mutableSetOf()
  if (movingSide.active.species.types.contains(move.type)) {
    modifiers.add(Modifier.STAB)
  }

  val moveDamage = computeDamage(
      movingSide.active.originalSpec.level,
      offensiveStat = offensiveStat,
      defensiveStat = defensiveStat,
      movePower = move.basePower,
      effectiveness = effectiveness,
      damageRoll = context.random.moveDamage(),
      modifiers = modifiers)
  context.logger.attack(mover, movingSide.active, otherSide.active, moveDamage)

  val newOpposingActivePokemon = otherSide.active.takeDamageAndMaybeFaint(moveDamage)
  val newOtherSide = otherSide.updateActivePokemon(newOpposingActivePokemon)

  var newAttacker = movingSide.active
  if (move.selfSwitch) {
    newAttacker = newAttacker.withEffect(EffectFlag.SELF_SWITCH)
  }
  val newMovingSide = movingSide.updateActivePokemon(newAttacker)

  return battle.withSide(mover, newMovingSide)
      .withSide(mover.other(), newOtherSide)
}

/**
 * Adjudicates a switch ordered by a player after a move like U-Turn, if warranted.
 */
internal fun makeAfterMoveSwitch(battle: Battle, context: BattleContext, switcher: Player): Battle {
  val side = battle.side(switcher)
  if (!side.active.effects.contains(EffectFlag.SELF_SWITCH)) {
    return battle
  }

  if (side.bench.isEmpty()) {
    // If the bench is empty, U-Turn does not switch you out.
    val newActive = side.active.clearEffect(EffectFlag.SELF_SWITCH)
    return battle.withSide(switcher, side.updateActivePokemon(newActive))
  }

  val switch = (battle.choices[switcher, Phase.SECOND_MOVE_SWITCH]
      ?: battle.choices[switcher, Phase.FIRST_MOVE_SWITCH]) as SwitchChoice?

  return makeSwitch(battle, context, switcher, switch!!)
}

/**
 * Adjudicates a switch ordered by a player.
 *
 * This method is used both at the beginning of the turn and after a U-Turn connects.
 */
internal fun makeSwitch(battle: Battle, context: BattleContext, mover: Player, switch: SwitchChoice): Battle {
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
  val choice = battle.move(player)!!
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

  val choice = battle.choices[switcher, Phase.END] as SwitchChoice
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
 *
 * @property choice Whether or not players get to make a choice at this time. For all of these,
 *    the player doesn't necessarily get to make a choice if this is true, but they definitely
 *    don't make a choice if it's false.
 */
enum class Phase(val choice: Boolean = false) {
  /**
   * During this phase, trainers decide which moves to use.
   */
  BEGIN(true),

  /**
   * During this phase, Battle#faster is set.
   */
  COMPUTE_TURN_ORDER,

  /**
   * During this phase, the action of the faster Pokemon is adjudicated.
   */
  FIRST_ACTION,

  /**
   * If the first mover successfully used a self-switch move, this is where they switch.
   */
  FIRST_MOVE_SWITCH(true),

  /**
   * During this phase, the action of the slower Pokemon is adjudicated. If
   * this Pokemon is fainted or otherwise out of action, this might not
   * happen.
   */
  SECOND_ACTION,

  /**
   * If the second mover successfully used a self-switch move, this is where they switch.
   */
  SECOND_MOVE_SWITCH(true),

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
  END(true),

  /**
   * The faster Pokemon gets the opportunity to switch out if fainted.
   */
  FIRST_SWITCH_AFTER_FAINT,

  /**
   * The slower Pokemon gets the opportunity to switch out if fainted.
   */
  SECOND_SWITCH_AFTER_FAINT;

  // TODO: Consider adding a true 'end of turn' phase which clears choices.

  /**
   * Returns the phase after this one.
   *
   * If it's the last phase, returns the first phase.
   */
  fun next(): Phase {
    return Phase.values()[(this.ordinal + 1) % Phase.values().size]
  }
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