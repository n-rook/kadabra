'use strict';

const EventEmitter = require('events');

const Promise = require('bluebird');
const WebSocket = require('ws');

const URL = 'ws://localhost:8000/showdown/websocket';

class ShowdownConnection extends EventEmitter {
  constructor(connection) {
    super();

    // WebSocket
    this.connection = connection;
    this.connection.on('message', this.handleMessage.bind(this));
  }

  handleMessage(data, flags) {
    if (flags.binary) {
      console.log('This is odd: received binary data.');
      return;
    }
    console.log('Received a message!\n' + data);
    this.emit('message', parseShowdownMessage(data));
  }

  send(data) {
    console.log('> ', data);
    return Promise.fromCallback((resolver) => {
      this.connection.send(data, {}, resolver);
    });
  }
}

function parseShowdownMessage(data) {
  // Showdown websockets messages consist of the following data:
  // Header: An optional leading line that begins with >. Used for battles.
  // For instance, >battle-gen7randombattle-34
  //
  // Data: A series of lines of data.
  // Each message represents a coherent chunk of data, with several fields,
  // separated by |.
  // Example:
  // |init|battle
  // |title|nrook vs. Guest 14
  // 
  // Empty lines:
  // For some reason, sometimes Showdown transmits empty lines. I don't know why.
  const lines = data.split('\n');
  if (!lines) {
    return new ShowdownMessage('', []);
  }

  let header;
  if (lines[0].startsWith('>')) {
    header = lines[0].slice(1);
    lines.shift();
  } else {
    header = '';
  }

  const linesWithoutEmptyLines = lines.filter((l) => !!l);
  const splitLines = linesWithoutEmptyLines.map((line) => {
    const splitLine = line.split('|');
    if (!splitLine) {
      throw new Error('This should never happen');
    }
    if (splitLine[0]) {
      throw new Error('Expected prefix before | to be empty, but it was: ' +
          splitLine[0]);
    }
    return splitLine.slice(1);
  });

  return new ShowdownMessage(header, splitLines);
}

class ShowdownMessage {
  constructor(header, splitLines) {
    this.header = header;
    this.splitLines = splitLines;
  }
}

function connect(url) {
  return new Promise(function(resolve) {
    console.log('Establishing connection to ' + url)

    const connection = new WebSocket(url);
    connection.on('open', function() {
      resolve(new ShowdownConnection(connection));
    });
    
    connection.on('error', function(err) {
      console.log('Oh no! Error.', err);
    });
  });
};

module.exports = connect;
