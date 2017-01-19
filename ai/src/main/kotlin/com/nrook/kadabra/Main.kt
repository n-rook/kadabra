package com.nrook.kadabra

fun main(args: Array<String>) {
    val m: PokemonSpec = PokemonSpec.newBuilder()
        .setSpecies("Magikarp")
        .build()
    println(m.species)
}
