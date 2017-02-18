package com.nrook.kadabra

import io.grpc.stub.StreamObserver
import mu.KLogging

val logger = KLogging().logger()

/**
 * Wrapper for RPC implementation classes.
 */
fun <T> handleResponse(implementation: () -> T, observer: StreamObserver<T>) {
  val result: T
  try {
    result = implementation()
  } catch (e: Exception) {
    logger.error("Error handling RPC", e)
    observer.onError(e)
    return
  }

  observer.onNext(result)
  observer.onCompleted()
}
