/**
 * Hosts the stadium service.
 */

import * as grpc from 'grpc';

import * as logger from 'winston';

import {stadiumFile} from './proto_constants';
const stadiumDescriptor = grpc.load(stadiumFile);

const PORT = 8081;

export function startServer(): void {
  const server = new grpc.Server();
  server.addProtoService(stadiumDescriptor.kadabra.StadiumService.service, {
    selfPlay: handleSelfPlay
  });
  const port = server.bind(`0.0.0.0:${PORT}`, grpc.ServerCredentials.createInsecure());
  if (port !== 8081) {
    throw Error('Huh, port was ' + port);
  }
  logger.info(`Listening on port ${port}`);
  server.start();
}

function handleSelfPlay(selfPlayRequest): {} {
  throw Error('no');
}
