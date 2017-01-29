const path = require('path');
const util = require('util');

const grpc = require('grpc');
import * as Promise from 'bluebird';

import Team = require('./team');

const protoPath = path.normalize('../proto');
const aiDescriptor = grpc.load(protoPath + '/ai.proto');

export class TeamClient {
  stub: any
  constructor(port) {
    this.stub = new aiDescriptor.kadabra.TeamService(`localhost:${port}`, grpc.credentials.createInsecure());
  }

  getTeam(metagame): Promise<Team> {
    return Promise.fromCallback((callback) => {
      this.stub.getTeam({metagame}, callback);
    }).then((response) => {
      console.log(util.inspect(response, {showHidden: false, depth: null}));
      return new Team(response);
    });
  }
}
