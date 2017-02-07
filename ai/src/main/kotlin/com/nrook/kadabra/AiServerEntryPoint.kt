package com.nrook.kadabra

fun main(args: Array<String>) {
  val aiServer = createAndStartAiServer(8080)
  aiServer.awaitTermination()
}
