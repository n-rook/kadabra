package com.nrook.kadabra

import com.google.common.truth.Truth.assertThat
import com.nrook.kadabra.ai.Ai
import com.nrook.kadabra.proto.BattleServiceGrpc
import com.nrook.kadabra.proto.LeadRequest
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

class BattleServiceTest {

  lateinit var server: Server
  lateinit var inProcessChannel: ManagedChannel

  @Before
  fun setUp() {
    val serverName : String = "Test server " + javaClass.canonicalName
    server = InProcessServerBuilder.forName(serverName)
        .addService(BattleService(Ai()))
        .directExecutor()
        .build()
    server.start()
    inProcessChannel = InProcessChannelBuilder.forName(serverName).build()
  }

  @After
  fun tearDown() {
    inProcessChannel.shutdownNow()
    server.shutdown()
  }

  @Test
  fun chooseLead() {
    val stub = BattleServiceGrpc.newBlockingStub(inProcessChannel)
    val response = stub.chooseLead(LeadRequest.getDefaultInstance())
    assertThat(response.leadIndex).isAtLeast(1)
    assertThat(response.leadIndex).isAtMost(6)
  }
}
