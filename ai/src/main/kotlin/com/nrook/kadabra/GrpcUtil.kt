package com.nrook.kadabra

import io.grpc.stub.StreamObserver

/**
 * Wrapper for RPC implementation classes.
 */
fun <T> handleResponse(implementation: () -> T, observer: StreamObserver<T>) {
  val result: T
  try {
    result = implementation()
  } catch (e: Exception) {
    observer.onError(e)
    observer.onCompleted()
    return
  }

  observer.onNext(result)
  observer.onCompleted()
}
