/**
 * Hosts the stadium service.
 */

import * as Promise from 'bluebird';
import * as grpc from 'grpc';

import * as logger from 'winston';

import { connect } from './showdown';
import { BattleClient } from './ai_client';
import { ShowdownDirector } from './showdown_director';
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

  handleSelfPlay(selfPlayRequest, callback): void {
    Promise.resolve(this._handleSelfPlay(selfPlayRequest))
        .asCallback(callback);
  }

  _handleSelfPlay(selfPlayRequest): Promise<{}> {
    // const connectionOne = this._newShowdownDirector('abraca001');
    // const connectionTwo = this._newShowdownDirector('abraca002');

    // Let's do them in order?
    return this._newShowdownDirector('abraca001')
        .then((directorOne) => {
          return this._newShowdownDirector('abraca002')
              .then((directorTwo) => [directorOne, directorTwo])
        })
        .then(([directorOne, directorTwo]) => {
          return directorOne.challenge('gen7ou', 'abraca002')
              .then(() => directorTwo.considerAcceptingChallenge());
        })
        .then(() => {
          // not done yet...
          return {};
        });
  }

  _newShowdownDirector(username: string): Promise<ShowdownDirector> {
    const director: Promise<ShowdownDirector> = connect(this.showdownWebSocketUrl)
        .then((connection) => new ShowdownDirector(
            connection, this.teamClient, this.battleClient));
    return director.then((d) => {
      return d.setUsername(username).then(() => d);
    });
  }
}
