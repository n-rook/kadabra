/**
 * Provides a friendlier wrapper for the stub to the AI server.
 */

import * as path from 'path';
import * as util from 'util';

import * as grpc from 'grpc';
import * as Promise from 'bluebird';
import * as logger from 'winston';

import { IMoveInfo, ISideInfo } from './states';
import {aiFile} from './proto_constants';

const aiDescriptor = grpc.load(aiFile);

// Represents a decision to use a specific move.
export class IMoveAction {
  // The index of the move to use, from 1 to 4.
  readonly index: Number;

  // Whether or not to mega evolve.
  readonly megaEvolve: boolean;
}

// Represents a single action. This is a union class: exactly one of move and switch should be set.
export class IAction {
  readonly move: Number;
  readonly switch: Number;
}

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

  selectAction(room: string, activeMoves: IMoveInfo[]): Promise<IMoveAction> {
    // Try promisifyAll?
    const request = {room: {name: room}};

    // The format is currently the same.
    request['move'] = activeMoves;

    return Promise.fromCallback((callback) => {
      this.stub.selectAction(request, callback);
    }).then((response) => {
      logger.info(util.inspect(response, {showHidden: false, depth: null}));
      return response.move.index;
    });
  }

  selectSwitchAfterFaintAction(room: string, sideInfo: ISideInfo): Promise<number> {
    const request = {
      room: {name: room},
      sideInfo: convertSideInfoToProto(sideInfo)
    };

    return Promise.fromCallback((callback) => {
      this.stub.selectSwitchAfterFaint(request, callback);
    }).then((response) => {
      return response.switch.index;
    });
  }
}

function convertSideInfoToProto(info: ISideInfo): {} {
  return {
    team: info.team.map((pokemonInfo) => {
      const returnInfo = {
        species: pokemonInfo.species,
        hp: pokemonInfo.hp,
        fainted: pokemonInfo.fainted,
        item: pokemonInfo.item
      };
      if (pokemonInfo.maxHp !== null) {
        returnInfo['maxHp'] = pokemonInfo.maxHp;
      }
      return returnInfo;
    })
  };
}
