import * as EventEmitter from 'events';

import * as Promise from 'bluebird';
import * as WebSocket from 'ws';

import * as logger from 'winston';
const showdownLogger = new (logger.Logger)({
  transports: [
    new (logger.transports.Console)()
  ]
});

export class ShowdownConnection extends EventEmitter {

  _connection: WebSocket;
  constructor(connection) {
    super();

    // WebSocket
    this._connection = connection;
    this._connection.on('message', this.handleMessage.bind(this));
  }

  handleMessage(data, flags): void {
    if (flags.binary) {
      logger.warn('Received binary data from Showdown');
      return;
    }
    showdownLogger.info('<', data);
    this.emit('message', parseShowdownMessage(data));
  }

  send(data): Promise<any> {
    showdownLogger.info('>', data);
    return Promise.fromCallback((resolver) => {
      this._connection.send(data, {}, resolver);
    });
  }
}

function parseShowdownMessage(data): ShowdownMessage {
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

export class ShowdownMessage {
  header: string;
  splitLines: string[][];

  constructor(header, splitLines) {
    this.header = header;
    this.splitLines = splitLines;
  }
}

export function connect(url): Promise<ShowdownConnection> {
  return new Promise(function(resolve) {
    logger.info('Establishing connection to', url);

    const connection = new WebSocket(url);
    connection.on('open', function() {
      resolve(new ShowdownConnection(connection));
    });
    
    connection.on('error', function(err) {
      logger.error('Connection error:', err);
    });
  }) as Promise<ShowdownConnection>;
}
