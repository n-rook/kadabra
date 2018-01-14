package com.nrook.kadabra.inference

import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.Species
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.mechanics.Condition
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.arena.Player

/**
 * A class which reads in a battle state from a list of logs.
 */
class BattleLoader(private val pokedex: Pokedex) {

  /**
   * Parse the state of a battle.
   */
  fun parseBattle(ourTeam: List<PokemonSpec>, events: List<BattleEvent>): OngoingBattle {
    val teamPreview = parseTeamPreviewBattle(ourTeam, events)
    val eventsList = ImmutableList.copyOf(events)

    val startIndex = eventsList.indexOfFirst { it is StartEvent }
    val battleEvents = eventsList.subList(startIndex, eventsList.size)

    var ongoingBattle = teamPreview.toOngoingBattle()
    for ((index, event) in battleEvents.withIndex()) {
      try {
        when (event) {
          is SwitchEvent -> {
            ongoingBattle = updateSwitchOrDragEvent(event, ongoingBattle)
          }
          is DragEvent -> {
            ongoingBattle = updateSwitchOrDragEvent(event, ongoingBattle)
          }
          is DamageEvent -> {
            ongoingBattle = updateDamageEvent(event, ongoingBattle)
          }
          is HealEvent -> {
            ongoingBattle = updateHealEvent(event, ongoingBattle)
          }
          is TurnEvent -> {
            ongoingBattle = updateTurnEvent(event, ongoingBattle)
          }
        }
      } catch (e: RuntimeException) {
        logger.error("Error parsing event #$index", e)
        throw e
      }
    }

    return ongoingBattle
  }

  private fun updateSwitchOrDragEvent(
      event: SwitchOrDragEvent, state: OngoingBattle): OngoingBattle {
    if (event.player == state.us) {
      val switchedInIndex =
          state.ourSide.bench.indexOfFirst { it.nickname == event.identifier }
      val ourNewSide = if (state.ourSide.active == null)
        state.ourSide.comeIn(switchedInIndex)
      else
        state.ourSide.swap(switchedInIndex)
      return state.updateOurSide(ourNewSide)
    } else {
      val matchIndex = findNicknameMatchOnTheirSide(event, state)
      val theirNewSide = if (state.theirSide.active == null)
        state.theirSide.comeIn(matchIndex, event.identifier)
      else
        state.theirSide.swap(matchIndex, event.identifier)
      return state.updateTheirSide(theirNewSide)
    }
  }

  private fun updateDamageEvent(event: DamageEvent, state: OngoingBattle): OngoingBattle {
    assertIsActive(state, event.pokemon)
    if (event.pokemon.player == state.us) {
      if (event.newCondition.status == Condition.FAINT) {
        return state.updateOurSide(state.ourSide.faint())
      }

      val newActive = state.ourSide.active!!.updateHp(event.newCondition.hp)
          .updateCondition(event.newCondition.status)
      return state.updateOurSide(state.ourSide.updateActive(newActive))
    } else {
      if (event.newCondition.status == Condition.FAINT) {
        return state.updateTheirSide(state.theirSide.faint())
      }

      val newActive = state.theirSide.active!!
          .updateHp(HpFraction(event.newCondition.hp, event.newCondition.maxHp))
          .updateCondition(event.newCondition.status)
      return state.updateTheirSide(state.theirSide.updateActive(newActive))
    }
  }

  private fun updateHealEvent(event: HealEvent, state: OngoingBattle): OngoingBattle {
    assertIsActive(state, event.pokemon)
    if (event.pokemon.player == state.us) {
      val newActive = state.ourSide.active!!.updateHp(event.newCondition.hp)
          .updateCondition(event.newCondition.status)
      return state.updateOurSide(state.ourSide.updateActive(newActive))
    } else {
      val newActive = state.theirSide.active!!
          .updateHp(HpFraction(event.newCondition.hp, event.newCondition.maxHp))
          .updateCondition(event.newCondition.status)
      return state.updateTheirSide(state.theirSide.updateActive(newActive))
    }
  }

  private fun updateTurnEvent(event: TurnEvent, state: OngoingBattle): OngoingBattle {
    return OngoingBattle(
        state.us,
        state.ourSide,
        state.theirSide,
        event.turn,
        state.phase
    )
  }

  private fun assertIsActive(state: OngoingBattle, identifier: PokemonIdentifier) {
    if (identifier.player == state.us) {
      val active = state.ourSide.active
        ?: throw IllegalStateException(
          "Expected ${identifier.name.nickname} to be active, but no Pokemon is active for us")
      if (active.nickname != identifier.name) {
        throw IllegalStateException(
            "Expected ${identifier.name.nickname} to be active, but ${active.nickname} was")
      }
    } else {
      val active = state.theirSide.active
          ?: throw IllegalStateException(
          "Expected ${identifier.name.nickname} to be active, but no Pokemon is active for them")
      if (active.nickname != identifier.name) {
        throw IllegalStateException(
            "Expected ${identifier.name.nickname} to be active, but ${active.nickname} was")
      }
    }
  }

  private fun findNicknameMatchOnTheirSide(
      event: SwitchOrDragEvent, state: OngoingBattle): Int {
    val nicknameMatch =
        state.theirSide.bench.indexOfFirst { it.nickname == event.identifier }
    if (nicknameMatch != -1) {
      return nicknameMatch
    }

    // We don't recognize this nickname. Let's try a species match instead.
    val speciesMatch =
        state.theirSide.bench.indexOfFirst { it.species.name == event.details.species }
    if (speciesMatch == -1) {
      throw IllegalStateException(
          "Could not find switch target nicknamed ${event.identifier.nickname}.")
    }
    return speciesMatch
  }

  /**
   * Parse the state of a battle up to team preview/lead select.
   */
  fun parseTeamPreviewBattle(ourTeam: List<PokemonSpec>, events: List<BattleEvent>)
      : TeamPreviewBattleState {
    val us = findUs(events)
    val ourBench = findOurSide(events, ourTeam)
    val theirBench = findTheirSide(us, events)
    return TeamPreviewBattleState(us, ourBench, theirBench)
  }

  /**
   * Return which player we are.
   */
  private fun findUs(events: List<BattleEvent>): Player {
    val firstTeamPreviewRequest = events.firstOrNull {
      val request = (it as? RequestEvent)?.request
      request != null && request is TeamPreviewRequest
    } ?: throw IllegalArgumentException("Could not find TeamPreviewRequest")
    return ((firstTeamPreviewRequest as RequestEvent).request as TeamPreviewRequest).id
  }

  /**
   * Look up and return full information about the Pokemon on our side.
   */
  private fun findOurSide(
      events: List<BattleEvent>,
      ourTeam: List<PokemonSpec>): ImmutableList<OurBenchedPokemon> {
    val teamPreviewEvent = events.filterIsInstance(RequestEvent::class.java)
        .first { it.request is TeamPreviewRequest }
    val teamPreviewRequest = teamPreviewEvent.request as TeamPreviewRequest

    val ourTeamAsBenchedPokemon: ImmutableList.Builder<OurBenchedPokemon> = ImmutableList.builder()
    for (pokemon in teamPreviewRequest.pokemon) {
      val spec = ourTeam
          // TODO: This may not work, or it may only work for some species (i.e. not Arceus)
          .find { it.species.name == pokemon.details.species }
      if (spec == null) {
        throw IllegalArgumentException(
            "Could not find corresponding Pokemon for ${pokemon.details.species}. Choices were: " +
                ourTeam.map { it.species.name }.joinToString(", "))
      }

      ourTeamAsBenchedPokemon.add(
          OurBenchedPokemon(
              spec.species,
              pokemon.pokemon.name,
              spec,
              spec.getStat(Stat.HP),
              Condition.OK))
    }

    return ourTeamAsBenchedPokemon.build()
  }

  private fun findTheirSide(
      us: Player,
      events: List<BattleEvent>): ImmutableList<TheirBenchedPokemon> {
    val theirPokeEvents = events.filterIsInstance(PokeEvent::class.java)
        .filter { it.player != us }
    val theirBench = theirPokeEvents.map {
      TheirBenchedPokemon(
          pokedex.getSpeciesByName(it.details.species),
          it.details.gender,
          it.details.shiny,
          it.details.level,
          null,
          HpFraction(100, 100),
          Condition.OK
      )
    }
    return ImmutableList.copyOf(theirBench)
  }
}

/**
 * A battle state parsed up to team preview.
 *
 * @property us Which player we are.
 * @property ourBench The Pokemon on our bench. In order: if we want to lead with the first Pokemon
 *    we have, we'll be sending a message like
 *
 */
data class TeamPreviewBattleState(
    val us: Player,
    val ourBench: ImmutableList<OurBenchedPokemon>,
    val theirBench: ImmutableList<TheirBenchedPokemon>
) {
  /**
   * Converts this to an [OngoingBattle], with nobody sent out yet.
   */
  fun toOngoingBattle(): OngoingBattle {
    return OngoingBattle(
        us,
        OurSide(null, ourBench),
        TheirSide(null, theirBench),
        1,
        DecisionPhase.TEAM_PREVIEW
    )
  }
}

/**
 * A benched Pokemon on our side.
 */
data class OurBenchedPokemon(
    val species: Species,
    val nickname: Nickname,
    val originalSpec: PokemonSpec,
    val hp: Int,
    val condition: Condition
)

/**
 * A benched Pokemon.
 *
 * We don't necessarily know the nicknames of benched Pokemon, if they're opponents.
 * For active Pokemon, we do know, since the nickname is said when they are switched in.
 */
data class TheirBenchedPokemon(
    val species: Species,
    val gender: Gender,
    val shiny: Boolean,
    val level: Level,
    val nickname: Nickname?,
    val hp: HpFraction,
    val condition: Condition
)

/**
 * The information we have about an opponent's HP, represented as a fraction.
 *
 * We don't get perfect information about our opponents' HP. We get this fraction instead. The
 * denominator is 100 if HP Percentage Mod is on, as it is in competitive ladders, and 48 otherwise.
 */
data class HpFraction(val numerator: Int, val denominator: Int)

/**
 * Information about an ongoing battle.
 */
data class OngoingBattle(
    val us: Player,
    val ourSide: OurSide,
    val theirSide: TheirSide,
    val turn: Int,

    /**
     * The phase of the turn. Null if the phase is unknown.
     *
     * Generally this should be known if we're making a decision. After all, we can't make a
     * decision unless we know what to do, right?
     */
    val phase: DecisionPhase?
) {
  fun updateOurSide(newOurSide: OurSide): OngoingBattle {
    return OngoingBattle(us, newOurSide, theirSide, turn, phase)
  }

  fun updateTheirSide(newTheirSide: TheirSide): OngoingBattle {
    return OngoingBattle(us, ourSide, newTheirSide, turn, phase)
  }
}

/**
 * Which section of a turn we are in.
 */
enum class DecisionPhase {

  /**
   * A special state before the battle proper begins.
   */
  TEAM_PREVIEW,

  /**
   * The beginning of the turn, in which players pick moves.
   */
  BEGIN,

  /**
   * The faster Pokemon has used U-Turn, and thus the player needs to choose where to go.
   */
  FIRST_MOVE_SWITCH,

  /**
   * The slower Pokemon has used U-Turn.
   */
  SECOND_MOVE_SWITCH,

  /**
   * Moves have been made; now players with KOed Pokemon have to send out new ones.
   */
  END
}

/**
 * Our side of the battle.
 *
 * @param active Our active Pokemon. Null if our Pokemon has fainted.
 */
data class OurSide(
    val active: OurActivePokemon?,
    val bench: ImmutableList<OurBenchedPokemon>
) {

  /**
   * Updates our active Pokemon.
   */
  fun updateActive(newActive: OurActivePokemon): OurSide {
    if (active == null) {
      throw IllegalArgumentException("Active Pokemon is null, we can't update them")
    }
    if (active.nickname != newActive.nickname) {
      throw IllegalArgumentException("Can't change Pokemon identity with this method. " +
          "Old nickname was ${active.nickname}, new is ${newActive.nickname}")
    }

    return OurSide(newActive, bench)
  }

  /**
   * Bring in a new Pokemon. Used only when we have no active Pokemon.
   */
  fun comeIn(benchIndex: Int): OurSide {
    if (active != null) {
      throw IllegalArgumentException("There is already a Pokemon active, ${active.species.name}.")
    }

    val unbenched = bench[benchIndex]
    val newActive = OurActivePokemon(
        unbenched.species,
        unbenched.nickname,
        unbenched.originalSpec,
        unbenched.hp,
        unbenched.condition)

    val newBench = ImmutableList.copyOf(Iterables.concat(
        bench.subList(0, benchIndex),
        bench.subList(benchIndex + 1, bench.size)))

    return OurSide(newActive, newBench)
  }

  /**
   * Bring in the Pokemon at the given index, and replace them with the active Pokemon.
   *
   * @throws IllegalStateException if there is no active Pokemon right now.
   */
  fun swap(benchIndex: Int): OurSide {
    if (active == null) {
      throw IllegalArgumentException("Active Pokemon is null, we can't switch someone in")
    }

    val unbenched = bench[benchIndex]
    val newActive = OurActivePokemon(
        unbenched.species,
        unbenched.nickname,
        unbenched.originalSpec,
        unbenched.hp,
        unbenched.condition)

    val newBenchedPokemon = OurBenchedPokemon(
        active.species,
        active.nickname,
        active.originalSpec,
        active.hp,
        active.condition)

    val newBench = ImmutableList.copyOf(Iterables.concat(
        bench.subList(0, benchIndex),
        ImmutableList.of(newBenchedPokemon),
        bench.subList(benchIndex + 1, bench.size)))
    return OurSide(newActive, newBench)
  }

  /**
   * Get rid of the active Pokemon.
   */
  fun faint(): OurSide {
    if (active == null) {
      throw IllegalStateException("There already is no active Pokemon!")
    }
    return OurSide(null, bench)
  }
}

/**
 * An active Pokemon on our side.
 */
data class OurActivePokemon(
    val species: Species,
    val nickname: Nickname,
    val originalSpec: PokemonSpec,
    val hp: Int,
    val condition: Condition
) {
  fun updateHp(newHp: Int): OurActivePokemon {
    if (newHp <= 0) {
      throw IllegalArgumentException("Bad HP $newHp")
    }

    return OurActivePokemon(species, nickname, originalSpec, newHp, condition)
  }

  fun updateCondition(newCondition: Condition): OurActivePokemon {
    if (newCondition == Condition.FAINT) {
      throw IllegalArgumentException("Fainted Pokemon aren't active")
    }
    return OurActivePokemon(species, nickname, originalSpec, hp, newCondition)
  }
}

data class TheirSide(
    val active: TheirActivePokemon?,
    val bench: ImmutableList<TheirBenchedPokemon>
) {

  /**
   * Updates their active Pokemon.
   */
  fun updateActive(newActive: TheirActivePokemon): TheirSide {
    if (active == null) {
      throw IllegalArgumentException("Active Pokemon is null, we can't update them")
    }
    if (active.nickname != newActive.nickname) {
      throw IllegalArgumentException("Can't change Pokemon identity with this method. " +
          "Old nickname was ${active.nickname}, new is ${newActive.nickname}")
    }

    return TheirSide(newActive, bench)
  }

  /**
   * Bring in a new Pokemon. Used only when they have no active Pokemon.
   *
   * @param benchIndex The index on the bench list of the Pokemon coming in.
   * @param nickname The nickname of the Pokemon coming in. Pokemon being switched in are the major
   * opportunity to discover the nicknames of one's opponents.
   */
  fun comeIn(benchIndex: Int, nickname: Nickname): TheirSide {
    if (active != null) {
      throw IllegalArgumentException("There is already a Pokemon active, ${active.species.name}.")
    }

    val unbenched = bench[benchIndex]
    if (unbenched.nickname != null && unbenched.nickname != nickname) {
      throw IllegalArgumentException(
          "Tried to bring out an enemy Pokemon with the nickname $nickname. However, we already " +
              "think the target is nicknamed ${unbenched.nickname}.")
    }

    val newActive = TheirActivePokemon(
        unbenched.species,
        unbenched.gender,
        unbenched.shiny,
        unbenched.level,
        nickname,
        unbenched.hp,
        unbenched.condition)

    val newBench = ImmutableList.copyOf(Iterables.concat(
        bench.subList(0, benchIndex),
        bench.subList(benchIndex + 1, bench.size)))

    return TheirSide(newActive, newBench)
  }

  /**
   * Bring in the Pokemon at the given index, and replace them with the active Pokemon.
   *
   * @param benchIndex The index on the bench list of the Pokemon coming in.
   * @param nickname The nickname of the Pokemon coming in. Pokemon being switched in are the major
   * opportunity to discover the nicknames of one's opponents.
   * @throws IllegalStateException if there is no active Pokemon right now.
   */
  fun swap(benchIndex: Int, nickname: Nickname): TheirSide {
    if (active == null) {
      throw IllegalArgumentException("Active Pokemon is null, we can't switch someone in")
    }

    val unbenched = bench[benchIndex]
    if (unbenched.nickname != null && unbenched.nickname != nickname) {
      throw IllegalArgumentException(
          "Tried to bring out an enemy Pokemon with the nickname $nickname. However, we already " +
              "think the target is nicknamed ${unbenched.nickname}.")
    }

    val newActive = TheirActivePokemon(
        unbenched.species,
        unbenched.gender,
        unbenched.shiny,
        unbenched.level,
        nickname,
        unbenched.hp,
        unbenched.condition)

    val newBenchedPokemon = TheirBenchedPokemon(
        active.species,
        active.gender,
        active.shiny,
        active.level,
        active.nickname,
        active.hp,
        active.condition)

    val newBench = ImmutableList.copyOf(Iterables.concat(
        bench.subList(0, benchIndex),
        ImmutableList.of(newBenchedPokemon),
        bench.subList(benchIndex + 1, bench.size)))
    return TheirSide(newActive, newBench)
  }

  /**
   * Get rid of the active Pokemon.
   */
  fun faint(): TheirSide {
    if (active == null) {
      throw IllegalStateException("There already is no active Pokemon!")
    }
    return TheirSide(null, bench)
  }
}

/**
 * An active Pokemon on their side.
 *
 * Unlike with [TheirBenchedPokemon], we don't have to worry about nicknames; when a Pokemon is
 * brought out, we find out their nickname.
 */
data class TheirActivePokemon(
    val species: Species,
    val gender: Gender,
    val shiny: Boolean,
    val level: Level,
    val nickname: Nickname,
    val hp: HpFraction,
    val condition: Condition
) {
  fun updateHp(newHpFraction: HpFraction): TheirActivePokemon {
    if (newHpFraction.numerator == 0) {
      throw IllegalArgumentException("Don't use updateHp to send HP to 0")
    }
    return TheirActivePokemon(species, gender, shiny, level, nickname, newHpFraction, condition)
  }

  fun updateCondition(newCondition: Condition): TheirActivePokemon {
    if (newCondition == Condition.FAINT) {
      throw IllegalArgumentException("Fainted Pokemon aren't active")
    }
    return TheirActivePokemon(species, gender, shiny, level, nickname, hp, condition)
  }
}