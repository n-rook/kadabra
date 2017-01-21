const path = require('path');

const grpc = require('grpc');
const Promise = require('bluebird');

const protoPath = path.normalize('../proto');
const aiDescriptor = grpc.load(protoPath + '/ai.proto');

class TeamClient {
  constructor(port) {
    this.stub = new aiDescriptor.kadabra.TeamService('localhost:' + 8080, grpc.credentials.createInsecure());
    console.log('stub', this.stub);
  }

  getTeam(metagame) {
    return Promise.fromCallback((callback) => {
      this.stub.getTeam({metagame}, callback);
    }).then((response) => {
      console.log(response);
    });
  }
}

module.exports = TeamClient;
