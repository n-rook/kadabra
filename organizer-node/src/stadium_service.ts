/**
 * Hosts the stadium service.
 */

import * as grpc from 'grpc';

import * as logger from 'winston';

import { BattleClient } from './ai_client';
import { TeamClient } from './teamclient';
import { stadiumFile } from './proto_constants';

const stadiumDescriptor = grpc.load(stadiumFile);

const PORT = 8081;

export function startServer(
    showdownWebSocketUrl: String,
    teamClient: TeamClient,
    battleClient: BattleClient): void {
  const stadiumServer: StadiumServer = new StadiumServer(showdownWebSocketUrl, teamClient, battleClient);

  const server = new grpc.Server();
  server.addProtoService(stadiumDescriptor.kadabra.StadiumService.service, {
    selfPlay: stadiumServer.handleSelfPlay.bind(stadiumServer)
  });
  const port = server.bind(`0.0.0.0:${PORT}`, grpc.ServerCredentials.createInsecure());
  if (port !== 8081) {
    throw Error('Huh, port was ' + port);
  }
  logger.info(`Listening on port ${port}`);
  server.start();
}

class StadiumServer {
  private showdownWebSocketUrl: String;
  private teamClient: TeamClient;
  private battleClient: BattleClient;

  constructor(
      showdownWebSocketUrl: String,
      teamClient: TeamClient,
      battleClient: BattleClient) {
    this.showdownWebSocketUrl = showdownWebSocketUrl;
    this.teamClient = teamClient;
    this.battleClient = battleClient;
  }

  handleSelfPlay(selfPlayRequest): {} {
    throw Error('no');
  }
}
