package com.nrook.kadabra.inference

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nrook.kadabra.info.Gender
import com.nrook.kadabra.mechanics.arena.Player
import org.junit.Before
import org.junit.Test

class RequestDeserializerKtTest {

  lateinit var gson: Gson

  @Before
  fun setUp() {
    gson = registerRequestDeserializers(GsonBuilder())
        .create()
  }

  @Test
  fun deserializeTeamPreviewMessage() {
    val actualTeamBuilderRequest = """{"teamPreview":true,"maxTeamSize":6,"side":{"name":"abraca001","id":"p2","pokemon":[{"ident":"p2: Nihilego","details":"Nihilego","condition":"359/359","active":true,"stats":{"atk":127,"def":130,"spa":353,"spd":299,"spe":335},"moves":["powergem","sludgewave","thunderbolt","hiddenpowerice60"],"baseAbility":"beastboost","item":"choicescarf","pokeball":"pokeball"},{"ident":"p2: Scizor","details":"Scizor, F","condition":"343/343","active":false,"stats":{"atk":296,"def":291,"spa":131,"spd":228,"spe":170},"moves":["bulletpunch","roost","uturn","swordsdance"],"baseAbility":"technician","item":"scizorite","pokeball":"pokeball"},{"ident":"p2: Skarmory","details":"Skarmory, F","condition":"334/334","active":false,"stats":{"atk":196,"def":416,"spa":104,"spd":177,"spe":176},"moves":["roost","spikes","defog","whirlwind"],"baseAbility":"keeneye","item":"rockyhelmet","pokeball":"pokeball"},{"ident":"p2: Magnezone","details":"Magnezone","condition":"313/313","active":false,"stats":{"atk":158,"def":266,"spa":394,"spd":216,"spe":188},"moves":["flashcannon","thunderbolt","hiddenpowerfire60","voltswitch"],"baseAbility":"magnetpull","item":"assaultvest","pokeball":"pokeball"},{"ident":"p2: Tangrowth","details":"Tangrowth, F","condition":"404/404","active":false,"stats":{"atk":236,"def":287,"spa":256,"spd":218,"spe":122},"moves":["knockoff","gigadrain","earthquake","hiddenpowerice60"],"baseAbility":"chlorophyll","item":"assaultvest","pokeball":"pokeball"},{"ident":"p2: Greninja","details":"Greninja, F","condition":"285/285","active":false,"stats":{"atk":227,"def":153,"spa":305,"spd":178,"spe":377},"moves":["gunkshot","icebeam","hiddenpowerfire60","spikes"],"baseAbility":"torrent","item":"lifeorb","pokeball":"pokeball"}]},"rqid":3}"""

    val requestMessage: RequestMessage = deserializeRequest(actualTeamBuilderRequest)

    assertThat(requestMessage).isInstanceOf(TeamPreviewRequest::class.java)

    val teamPreviewRequest = requestMessage as TeamPreviewRequest
    assertThat(teamPreviewRequest.name).isEqualTo("abraca001")
    assertThat(teamPreviewRequest.id).isEqualTo(Player.WHITE)

    assertThat(teamPreviewRequest.pokemon).hasSize(6)
    val nihilego = teamPreviewRequest.pokemon[0]
    assertThat(nihilego.pokemon.player).isEqualTo(Player.WHITE)
    assertThat(nihilego.pokemon.name).isEqualTo(Nickname("Nihilego"))
    assertThat(nihilego.details.species).isEqualTo("Nihilego")
    assertThat(nihilego.details.gender).isEqualTo(Gender.GENDERLESS)
  }
}