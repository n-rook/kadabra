package com.nrook.kadabra.info.testdata

import com.nrook.kadabra.info.*
import com.nrook.kadabra.mechanics.MAX_IVS
import com.nrook.kadabra.mechanics.Nature
import com.nrook.kadabra.mechanics.PokemonSpec
import com.nrook.kadabra.mechanics.makeEvs

val CHARIZARD: Species = Species(
    PokemonId("charizard"),
    "Charizard",
    6,
    listOf(PokemonType.FIRE, PokemonType.FLYING),
    GenderPossibilities.MALE_OR_FEMALE,
    mapOf(
        Stat.HP to 78,
        Stat.ATTACK to 84,
        Stat.DEFENSE to 78,
        Stat.SPECIAL_ATTACK to 109,
        Stat.SPECIAL_DEFENSE to 85,
        Stat.SPEED to 100
    ),
    AbilitySet(AbilityId("Blaze"), null, AbilityId("Solar Power")),
    1, 1, setOf(), null)


val BLASTOISE: Species = Species(
    PokemonId("blastoise"),
    "Blastoise",
    9,
    listOf(PokemonType.WATER),
    GenderPossibilities.MALE_OR_FEMALE,
    mapOf(
        Stat.HP to 79,
        Stat.ATTACK to 83,
        Stat.DEFENSE to 100,
        Stat.SPECIAL_ATTACK to 85,
        Stat.SPECIAL_DEFENSE to 105,
        Stat.SPEED to 78
    ),
    AbilitySet(AbilityId("Torrent"), null, AbilityId("Rain Dish")),
    heightmm = 1600,
    weightg = 85500,
    otherForms = setOf(),
    form = null)

val MAGCARGO: Species = Species(
    PokemonId("magcargo"),
    "Magcargo",
    219,
    listOf(PokemonType.FIRE, PokemonType.ROCK),
    GenderPossibilities.MALE_OR_FEMALE,
    mapOf(
        Stat.HP to 60,
        Stat.ATTACK to 50,
        Stat.DEFENSE to 120,
        Stat.SPECIAL_ATTACK to 90,
        Stat.SPECIAL_DEFENSE to 80,
        Stat.SPEED to 30
    ),
    AbilitySet(AbilityId("Magma Armor"), AbilityId("Flame Body"), AbilityId("Weak Armor")),
    heightmm = 800,
    weightg = 55000,
    otherForms = setOf(),
    form = null
)

val EARTHQUAKE: Move = Move(
    MoveId("earthquake"),
    100,
    PokemonType.GROUND,
    MoveCategory.PHYSICAL,
    true
)

val SURF: Move = Move(
    MoveId("surf"),
    90,
    PokemonType.WATER,
    MoveCategory.SPECIAL,
    true
)

val TACKLE: Move = Move(
    MoveId("tackle"),
    40,
    PokemonType.NORMAL,
    MoveCategory.PHYSICAL,
    true
)

val FLAMETHROWER: Move = Move(
    MoveId("flamethrower"),
    90,
    PokemonType.FIRE,
    MoveCategory.SPECIAL,
    false  // burn
)

val AERIAL_ACE: Move = Move(
    MoveId("aerialace"),
    60,
    PokemonType.FLYING,
    MoveCategory.PHYSICAL,
    false  // perfect accuracy
)

val ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD = PokemonSpec(
    CHARIZARD,
    AbilityId("Blaze"),
    Gender.MALE,
    Nature.ADAMANT,
    makeEvs(mapOf(Stat.ATTACK to 252, Stat.SPEED to 252, Stat.HP to 4)),
    MAX_IVS,
    moves = listOf(EARTHQUAKE))

val BOLD_252_HP_252_DEF_4_SPD_BLASTOISE = PokemonSpec(
    BLASTOISE,
    AbilityId("Torrent"),
    Gender.FEMALE,
    Nature.BOLD,
    makeEvs(mapOf(Stat.HP to 252, Stat.DEFENSE to 252, Stat.HP to 4)),
    MAX_IVS,
    moves = listOf(SURF))
