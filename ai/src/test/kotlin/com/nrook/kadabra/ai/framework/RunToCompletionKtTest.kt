package com.nrook.kadabra.ai.framework

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableTable
import com.google.common.truth.Truth
import com.nrook.kadabra.ai.perfect.RandomAi
import com.nrook.kadabra.info.AbilityId
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.info.testdata.*
import com.nrook.kadabra.mechanics.*
import com.nrook.kadabra.mechanics.arena.*
import com.nrook.kadabra.mechanics.rng.MoveDamagePolicy
import com.nrook.kadabra.mechanics.rng.RandomNumberGenerator
import com.nrook.kadabra.mechanics.rng.RandomPolicy
import org.junit.Test
import java.util.*

class RunToCompletionKtTest {

  lateinit var charizardSpec: PokemonSpec
  lateinit var blastoiseSpec: PokemonSpec

  @Test
  fun works() {
    charizardSpec = PokemonSpec(
        CHARIZARD,
        AbilityId("Blaze"),
        Gender.FEMALE,
        Nature.ADAMANT,
        NO_EVS,
        MAX_IVS,
        Level(100),
        listOf(FLAMETHROWER, EARTHQUAKE))
    val blackSide = Side(newActivePokemonFromSpec(charizardSpec), ImmutableMap.of())

    blastoiseSpec = PokemonSpec(
        BLASTOISE,
        AbilityId("Torrent"),
        Gender.FEMALE,
        Nature.MODEST,
        NO_EVS,
        MAX_IVS,
        Level(100),
        listOf(SURF))
    val whiteSide = Side(newActivePokemonFromSpec(blastoiseSpec), ImmutableMap.of())

    val random = Random()
    val rng = RandomNumberGenerator(RandomPolicy(MoveDamagePolicy.ONE), Random())

    val context = BattleContext(rng, debugLogger())
    val battle = Battle(1, blackSide, whiteSide, ImmutableTable.of(), Phase.BEGIN, null)

    val winner = runToCompletion(battle, RandomAi(random), RandomAi(random), context)

    // Blastoise has a serious advantage here, so it should never lose.
    Truth.assertThat(winner).isEqualTo(Player.WHITE)
  }
}
