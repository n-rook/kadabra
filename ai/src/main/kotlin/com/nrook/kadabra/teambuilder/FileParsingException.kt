package com.nrook.kadabra.teambuilder

/**
 * Thrown when we hit an unexpected error reading an input file.
 */
class FileParsingException(reason: String, badLine: String)
  : RuntimeException("Error parsing line:\n$badLine\n$reason") {

}