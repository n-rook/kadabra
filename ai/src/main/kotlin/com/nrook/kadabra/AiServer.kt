package com.nrook.kadabra

import io.grpc.Server
import io.grpc.ServerBuilder

/**
 * Wrapper class for the AI server.
 */
class AiServer(val server: Server) {
  fun shutdown() {
    server.shutdown()
  }

  fun awaitTermination() {
    server.awaitTermination()
  }
}

fun createAndStartAiServer(port: Int): AiServer {
  val server = ServerBuilder.forPort(port)
      .addService(TeamService())
      .build()
  server.start()
  return AiServer(server)
}
