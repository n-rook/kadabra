const path = require('path');
const util = require('util');

const grpc = require('grpc');
import * as Promise from 'bluebird';

import { Team } from './team';

const protoPath = path.normalize('../proto');
const aiDescriptor = grpc.load(protoPath + '/ai.proto');

export class TeamClient {
  stub: any;
  constructor(port) {
    this.stub = Promise.promisifyAll(
      new aiDescriptor.kadabra.TeamService(
        `localhost:${port}`, grpc.credentials.createInsecure()));
  }

  getTeam(metagame): Promise<Team> {
    return this.stub.getTeamAsync(metagame)
      .then((response) => {
        return new Team(response);
      });
  }
}
