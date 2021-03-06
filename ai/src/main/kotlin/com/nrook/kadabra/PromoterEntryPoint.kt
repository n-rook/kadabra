package com.nrook.kadabra

import com.nrook.kadabra.proto.Player
import com.nrook.kadabra.proto.StadiumServiceGrpc
import io.grpc.ManagedChannelBuilder

class TestClass

fun main(args: Array<String>) {
  val channel = ManagedChannelBuilder.forAddress("localhost", 8081).usePlaintext(true).build()
  val client: StadiumClient = StadiumClient(StadiumServiceGrpc.newBlockingStub(channel));
  val result = client.selfPlay()
  when (result.winner) {
    Player.ONE -> {
      println("Player 1 won")
    }
    Player.TWO -> {
      println("Player 2 won")
    }
    Player.UNKNOWN -> {
      println("Error.")
      println(result.error)
    }
  }

  println("--------------------")
  println("Player 1 logs:\n")
  println(result.playerOneLogsList.joinToString("\n"))
  println("\n\n--------------------")
  println("Player 2 logs:\n")
  println(result.playerTwoLogsList.joinToString("\n"))
}
