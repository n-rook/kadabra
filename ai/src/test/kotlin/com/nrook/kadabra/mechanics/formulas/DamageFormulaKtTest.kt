package com.nrook.kadabra.mechanics.formulas

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.info.Stat
import com.nrook.kadabra.info.testdata.*
import com.nrook.kadabra.mechanics.Level
import org.junit.Test

class DamageFormulaKtTest {

  @Test
  fun computeDamageRangeTest() {
    val damage = computeDamageRange(
        Level(100),
        offensiveStat = ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD.getStat(Stat.ATTACK),
        defensiveStat = BOLD_252_HP_252_DEF_4_SPD_BLASTOISE.getStat(Stat.DEFENSE),
        movePower = EARTHQUAKE.basePower,
        effectiveness = TypeDamage.NORMAL,
        modifiers = emptySet()
    )
    assertThat(damage).isEqualTo(65..77)
  }

  @Test
  fun computeDamageDistributionTest() {
    // Charizard using Earthquake against Blastoise
    val damage = computeDamageDistribution(
        Level(100),
        offensiveStat = ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD.getStat(Stat.ATTACK),
        defensiveStat = BOLD_252_HP_252_DEF_4_SPD_BLASTOISE.getStat(Stat.DEFENSE),
        movePower = EARTHQUAKE.basePower,
        effectiveness = TypeDamage.NORMAL,
        modifiers = emptySet()
    )
    assertThat(damage)
        .containsExactly(65, 66, 66, 67, 68, 69, 70, 70, 71, 72, 73, 73, 74, 75, 76, 77)
  }


  @Test
  fun stabRegressionTest() {
    // Charizard using Aerial Ace against Blastoise
    val damage = computeDamage(
        Level(100),
        offensiveStat = ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD.getStat(Stat.ATTACK),
        defensiveStat = BOLD_252_HP_252_DEF_4_SPD_BLASTOISE.getStat(Stat.DEFENSE),
        movePower = AERIAL_ACE.basePower,
        damageRoll = 85,
        effectiveness = TypeDamage.NORMAL,
        modifiers = setOf(Modifier.STAB)
    )
    assertThat(damage).isEqualTo(58)
  }

  @Test
  fun computeDamageStabTest() {
    // Charizard using Aerial Ace against Blastoise
    val damage = computeDamageDistribution(
        Level(100),
        offensiveStat = ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD.getStat(Stat.ATTACK),
        defensiveStat = BOLD_252_HP_252_DEF_4_SPD_BLASTOISE.getStat(Stat.DEFENSE),
        movePower = AERIAL_ACE.basePower,
        effectiveness = TypeDamage.NORMAL,
        modifiers = setOf(Modifier.STAB)
    )
    assertThat(damage)
        .containsExactly(58, 60, 60, 61, 61, 63, 63, 64, 64, 66, 66, 67, 67, 69, 69, 70)
  }

  @Test
  fun computeDamageStabButNotVeryEffectiveTest() {
    // Charizard using Flamethrower against Blastoise
    val damage = computeDamageDistribution(
        Level(100),
        offensiveStat = ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD.getStat(Stat.SPECIAL_ATTACK),
        defensiveStat = BOLD_252_HP_252_DEF_4_SPD_BLASTOISE.getStat(Stat.SPECIAL_DEFENSE),
        movePower = FLAMETHROWER.basePower,
        effectiveness = TypeDamage.HALF,
        modifiers = setOf(Modifier.STAB)
    )
    assertThat(damage)
        .containsExactly(45, 45, 46, 47, 48, 48, 48, 49, 49, 50, 51, 51, 51, 52, 53, 54)
  }

  @Test
  fun computeDamageStabCritNotVeryEffective() {
    // Charizard using Flamethrower against Blastoise; this time there's a crit!
    val damage = computeDamageDistribution(
        Level(100),
        offensiveStat = ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD.getStat(Stat.SPECIAL_ATTACK),
        defensiveStat = BOLD_252_HP_252_DEF_4_SPD_BLASTOISE.getStat(Stat.SPECIAL_DEFENSE),
        movePower = FLAMETHROWER.basePower,
        effectiveness = TypeDamage.HALF,
        modifiers = setOf(Modifier.STAB, Modifier.CRITICAL_HIT))
    assertThat(damage)
        .containsExactly(68, 69, 69, 71, 72, 72, 73, 74, 75, 75, 76, 77, 78, 78, 79, 81)
  }

  @Test
  fun computeDamageExpertBeltLightScreenSurf() {
    // Blastoise uses Surf against Charizard, with an Expert Belt, through Light Screen
    val damage = computeDamageDistribution(
        Level(100),
        offensiveStat = BOLD_252_HP_252_DEF_4_SPD_BLASTOISE.getStat(Stat.SPECIAL_ATTACK),
        defensiveStat = ADAMANT_252_ATK_252_SPD_4_HP_CHARIZARD.getStat(Stat.SPECIAL_DEFENSE),
        movePower = SURF.basePower,
        effectiveness = TypeDamage.DOUBLED,
        modifiers = setOf(Modifier.STAB, Modifier.SE_EXPERT_BELT, Modifier.LIGHT_SCREEN))
    assertThat(damage)
        .containsExactly(
            116, 119, 119, 120, 122, 124, 126, 126, 127, 130, 131, 131, 133, 134, 137, 138)
  }
}
