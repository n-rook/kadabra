package com.nrook.kadabra.teambuilder

import com.nrook.kadabra.info.PokemonDefinition
import com.nrook.kadabra.info.SetStatOnEvSpread
import com.nrook.kadabra.info.StatFromAbbreviation
import com.nrook.kadabra.proto.EvSpread
import com.nrook.kadabra.proto.Nature
import com.nrook.kadabra.proto.PokemonSpec
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

val MATCH_SPECIES_LINE = Regex("""([\S]+) @ ([\S]+)""")
val MATCH_ABILITY_LINE = Regex("""Ability: ([\S]+)""")
val MATCH_RELEVANT_PART_OF_EV_LINE = Regex("""EVs: (.*)""")
val MATCH_SINGLE_EV_DECLARATION = Regex("""(\d{1,3}) (\w{3})""")
val MATCH_NATURE = Regex("""(\w*) Nature""")
val MATCH_RELEVANT_PART_OF_IV_LINE = Regex("""IVs: (.*)""")
val MATCH_MOVE = Regex("""- (.*)""")

/**
 * Transforms a file into a team object.
 */
internal fun loadTeamFromFile(file : Path) : List<PokemonDefinition> {
  val lines: List<String> = Files.lines(file).collect(Collectors.toList<String>())

  var index = 0
  val team : MutableList<PokemonDefinition> = mutableListOf()
  while (true) {
    if (index >= lines.size) {
      return team
    }

    if (lines[index].isBlank()) {
      index++
      continue
    }

    val result = readSinglePokemon(lines, index)
    index = result.index
    team.add(result.definition)
  }
}

private data class ReadSinglePokemonResult(val definition: PokemonDefinition, val index: Int)
private fun readSinglePokemon(lines : List<String>, startIndex: Int) : ReadSinglePokemonResult {
  var index = startIndex

  val builder: PokemonSpec.Builder = PokemonSpec.newBuilder()
  val speciesResult = MATCH_SPECIES_LINE.find(lines[index])
      ?: throw FileParsingException("Could not find species and item information", lines[index])
  builder.species = speciesResult.groupValues[1]
  builder.item = speciesResult.groupValues[2]

  index++
  val abilityResult = MATCH_ABILITY_LINE.find(lines[index])
      ?: throw FileParsingException("Could not find ability information", lines[index])
  builder.ability = abilityResult.groupValues[1]

  index++
  builder.evs = readEvLine(lines[index])

  index++
  val nature = (MATCH_NATURE.find(lines[index])
      ?: throw FileParsingException("Could not find nature", lines[index]))
      .groupValues[1]
  builder.nature = Nature.valueOf(nature.toUpperCase())

  // Maybe IVs, maybe not
  index++
  if (MATCH_RELEVANT_PART_OF_IV_LINE.matches(lines[index])) {
    // TODO: Actually set this stuff!

    index++
  }

  while (true) {
    if (index == lines.size) {
      break
    }
    val moveResult = MATCH_MOVE.find(lines[index]) ?: break
    builder.addMove(moveResult.groupValues[1])
    index++
  }

  return ReadSinglePokemonResult(PokemonDefinition(builder.build()), index)
}

fun readEvLine(line: String) : EvSpread {
  var remainder = (MATCH_RELEVANT_PART_OF_EV_LINE.find(line)
      ?: throw FileParsingException("Could not recognize EV line", line))
      .groupValues[1]

  val builder = EvSpread.newBuilder()

  val evMatches = MATCH_SINGLE_EV_DECLARATION.findAll(remainder)
  for (match in evMatches) {
    val evValue = match.groupValues[1].toInt()
    val statName = match.groupValues[2]
    SetStatOnEvSpread(builder, StatFromAbbreviation(statName), evValue)
  }
  return builder.build()
}


/**
 * Loads teams by name from a specific directory.
 */
class FileBasedTeamLoader(val teamDirectory : Path) {

  fun loadTeam(teamName : String) : List<PokemonDefinition> {
    return loadTeamFromFile(teamDirectory.resolve(teamName))
  }
}
