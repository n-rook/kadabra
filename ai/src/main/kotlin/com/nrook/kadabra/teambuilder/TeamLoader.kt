package com.nrook.kadabra.teambuilder

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.io.Resources
import com.nrook.kadabra.info.AbilityId
import com.nrook.kadabra.info.Move
import com.nrook.kadabra.info.Pokedex
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.info.StatFromAbbreviation
import com.nrook.kadabra.info.TeamPokemon
import com.nrook.kadabra.mechanics.EvSpread
import com.nrook.kadabra.mechanics.Level
import com.nrook.kadabra.mechanics.Nature
import com.nrook.kadabra.mechanics.makeEvs

private val MATCH_SPECIES_AND_NICKNAME_LINE = Regex("""(.*) \((.*)\)""")

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
    val evs = readEvLine(lines[index])

    index++
    val natureString = (MATCH_NATURE.find(lines[index])
        ?: throw FileParsingException("Could not find nature", lines[index]))
        .groupValues[1]
    val nature = Nature.valueOf(natureString.toUpperCase())

    // Maybe IVs, maybe not
    index++
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
        species, item, ability, null, nature, evs, ivs, Level(100), moves.build())
    return ReadSingleResult(teamPokemon, index)
  }

  /**
   * Reads the part of a Pokemon's definition that comes before the @.
   */
  private fun readSpeciesAndNickname(speciesAndNickname: String): SpeciesAndNickname {
    if (speciesAndNickname.contains("(")) {
      val matchResult = MATCH_SPECIES_AND_NICKNAME_LINE.matchEntire(speciesAndNickname)!!
      return SpeciesAndNickname(matchResult.groupValues[2], matchResult.groupValues[1])
    } else {
      return SpeciesAndNickname(speciesAndNickname, null)
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

