/**
 * Provides a friendlier wrapper for the stub to the AI server.
 */

import * as path from 'path';
import * as util from 'util';

import * as grpc from 'grpc';
import * as Promise from 'bluebird';
import * as logger from 'winston';

const protoPath = path.normalize('../proto');
const aiDescriptor = grpc.load(protoPath + '/ai.proto');

export class BattleClient {
  private stub: any;
  
  constructor(port) {
    this.stub = new aiDescriptor.kadabra.BattleService(`localhost:${port}`, grpc.credentials.createInsecure());
  }

  /**
   * Returns the index of the lead to use.
   */
  chooseLead(): Promise<string> {
    return Promise.fromCallback((callback) => {
      this.stub.chooseLead({}, callback);
    }).then((response) => {
      logger.info(util.inspect(response, {showHidden: false, depth: null}));
      return response.leadIndex.toString();
    });
  }
}
