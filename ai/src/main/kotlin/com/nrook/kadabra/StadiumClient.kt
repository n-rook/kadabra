package com.nrook.kadabra

import com.nrook.kadabra.proto.SelfPlayRequest
import com.nrook.kadabra.proto.SelfPlayResponse
import com.nrook.kadabra.proto.StadiumServiceGrpc.StadiumServiceBlockingStub

/**
 * A client to interact with the stadium server.
 */
class StadiumClient(private val stub: StadiumServiceBlockingStub) {
  fun selfPlay(): SelfPlayResponse {
    return stub.selfPlay(SelfPlayRequest.getDefaultInstance())
  }
}
