package com.nrook.kadabra.mechanics.arena

import mu.KLogger
import java.io.Writer


/**
 * A "Writer" which just logs all messages.
 */
class KLoggingWriter(val logger: KLogger): Writer() {
  override fun write(cbuf: CharArray?, off: Int, len: Int) {
    logger.info(String(cbuf!!, off, len))
  }

  override fun flush() {
  }

  override fun close() {
  }
}