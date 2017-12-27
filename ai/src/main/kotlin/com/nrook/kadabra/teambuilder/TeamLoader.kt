package com.nrook.kadabra.teambuilder

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.io.Resources
import com.nrook.kadabra.info.AbilityId
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.info.StatFromAbbreviation
import com.nrook.kadabra.info.TeamPokemon
import com.nrook.kadabra.mechanics.*

private val MATCH_SPECIES_LINE = Regex("""(.*) @ ([\S].*\S)""")
private val MATCH_ABILITY_LINE = Regex("""Ability: (.+)""")
private val MATCH_RELEVANT_PART_OF_EV_LINE = Regex("""EVs: (.*)""")
private val MATCH_SINGLE_EV_DECLARATION = Regex("""(\d{1,3}) (\w{3})""")
private val MATCH_NATURE = Regex("""(\w*) Nature""")
private val MATCH_RELEVANT_PART_OF_IV_LINE = Regex("""IVs: (.*)""")
private val MATCH_MOVE = Regex("""- (.*)""")
private val MATCH_SPECIES_AND_NICKNAME_LINE = Regex("""(.*) \((.*)\)""")
private val MATCH_LEVEL_LINE = Regex("""Level: ([\d]+)""")
private val MATCH_SHINY_LINE = Regex("""Shiny: Yes""")

/**
 * Loads a team from a file.
 */
class TeamLoader(private val pokedex: Pokedex) {

  /**
   * Load a team in a resource. Used by tests.
   */
  fun loadTeamFromResource(resourceLookupUrl: String): ImmutableList<TeamPokemon> {
    return loadTeamFromLines(Resources.readLines(
        Resources.getResource(resourceLookupUrl), charset("UTF-8")))
  }

  /**
   * Load a team from the format Showdown's team builder export uses.
   */
  fun loadTeamFromLines(lines: List<String>): ImmutableList<TeamPokemon> {
    // For some reason, some of the dex exporters append whitespace to the end of lines.
    val trimmedLines = lines.map { it.trim() }

    var index = 0
    val team: ImmutableList.Builder<TeamPokemon> = ImmutableList.builder()
    while (true) {
      if (index >= trimmedLines.size) {
        return team.build()
      }

      if (trimmedLines[index].isEmpty()) {
        index++
        continue
      }

      val result = readSinglePokemon(trimmedLines, index)
      index = result.index
      team.add(result.definition)
    }
  }

  private fun readSinglePokemon(lines : List<String>, startIndex: Int) : ReadSingleResult {
    var index = startIndex

    val speciesResult = MATCH_SPECIES_LINE.find(lines[index])
        ?: throw FileParsingException("Could not find species and item information", lines[index])
    val speciesAndNickname = readSpeciesAndNickname(speciesResult.groupValues[1])
    // We ignore nicknames here
    val species = pokedex.getSpeciesByName(speciesAndNickname.species)
    val item = speciesResult.groupValues[2]

    index++
    val abilityResult = MATCH_ABILITY_LINE.find(lines[index])
        ?: throw FileParsingException("Could not find ability information", lines[index])
    val ability = AbilityId(abilityResult.groupValues[1])

    index++
    val levelResult = MATCH_LEVEL_LINE.find(lines[index])
    var level: Int?
    if (levelResult == null) {
      level = null
      index--  // no level supplied
    } else {
      level = levelResult.groupValues[1].toInt()
    }

    index++
    val shinyResult = MATCH_SHINY_LINE.find(lines[index])
    val shiny = shinyResult != null
    if (shinyResult == null) {
      index--  // no shininess supplied
    }

    index++
    val evs: EvSpread;
    if (MATCH_RELEVANT_PART_OF_EV_LINE.matches(lines[index])) {
      evs = readEvLine(lines[index])
      index++
    } else {
      // If EVs are absent, that means they're all zero
      evs = NO_EVS
    }

    val nature: Nature
    val natureString = MATCH_NATURE.find(lines[index])
    if (natureString == null) {
      nature = Nature.HARDY
    } else {
      nature = Nature.valueOf(natureString.groupValues[1].toUpperCase())
      index++
    }

    if (MATCH_RELEVANT_PART_OF_IV_LINE.matches(lines[index])) {
      // TODO: Actually set this stuff!
      index++
    }
    val ivs = com.nrook.kadabra.mechanics.MAX_IVS

    val moves: ImmutableList.Builder<Move> = ImmutableList.builder()
    while (true) {
      if (index == lines.size) {
        break
      }
      val moveResult = MATCH_MOVE.find(lines[index]) ?: break
      val moveName = moveResult.groupValues[1]  // e.g. "Close Combat"
      moves.add(pokedex.getMoveByExtendedName(moveName))
      index++
    }

    // TODO: Actually read in gender. I'll never actually do this but w/e
    val teamPokemon = TeamPokemon(
        species, item, ability, null, nature, evs, ivs, Level(level ?: 100), moves.build())
    return ReadSingleResult(teamPokemon, index)
  }

  /**
   * Reads the part of a Pokemon's definition that comes before the @.
   */
  private fun readSpeciesAndNickname(speciesAndNickname: String): SpeciesAndNickname {
    var speciesAndNicknameWithoutGender: String
    var gender: Gender?
    if (speciesAndNickname.endsWith(" (M)")) {
      gender = Gender.MALE
      speciesAndNicknameWithoutGender = speciesAndNickname.removeSuffix(" (M)")
    } else if (speciesAndNickname.endsWith(" (F)")) {
      gender = Gender.FEMALE
      speciesAndNicknameWithoutGender = speciesAndNickname.removeSuffix(" (F)")
    } else {
      gender = null  // random; that is, unspecified
      speciesAndNicknameWithoutGender = speciesAndNickname
    }

    if (speciesAndNicknameWithoutGender.contains("(")) {
      val matchResult = MATCH_SPECIES_AND_NICKNAME_LINE.matchEntire(
          speciesAndNicknameWithoutGender)!!
      return SpeciesAndNickname(matchResult.groupValues[2], matchResult.groupValues[1])
    } else {
      return SpeciesAndNickname(speciesAndNicknameWithoutGender, null)
    }
  }

  private fun readEvLine(line: String) : EvSpread {
    val remainder = (MATCH_RELEVANT_PART_OF_EV_LINE.find(line)
        ?: throw FileParsingException("Could not recognize EV line", line))
        .groupValues[1]

    val builder: ImmutableMap.Builder<Stat, Int> = ImmutableMap.builder()

    val evMatches = MATCH_SINGLE_EV_DECLARATION.findAll(remainder)
    for (match in evMatches) {
      val evValue = match.groupValues[1].toInt()
      val statName = match.groupValues[2]
      builder.put(StatFromAbbreviation(statName), evValue)
    }
    return makeEvs(builder.build())
  }
}

private data class SpeciesAndNickname(val species: String, val nickname: String?)
private data class ReadSingleResult(val definition: TeamPokemon, val index: Int)

