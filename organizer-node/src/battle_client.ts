/**
 * Provides a friendlier wrapper for the stub to the AI server.
 */

import * as path from 'path';
import * as util from 'util';

import * as grpc from 'grpc';
import * as Promise from 'bluebird';
import * as logger from 'winston';

import { IMoveInfo, ISideInfo } from './states';
import { aiFile } from './proto_constants';

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
  readonly move?: IMoveAction;
  readonly switch?: Number;
}

function actionResponseToIAction(response: {}): IAction {
  if (response['move']) {
    const index = response['move']['index']
    if (typeof(index) !== 'number') {
      throw Error(`Could not parse move action ${response}`);
    }
    return {
      move: {
        index,
        megaEvolve: false
      }
    };
  } else if (response['switch']) {
    const index = response['switch']['index']
    if (typeof(index) !== 'number') {
      throw Error(`Could not parse switch action ${response}`);
    }
    return {switch: index}
  }

  throw Error(`Could not parse action ${response}`);
}

export class BattleClient {
  private stub: any;

  constructor(port) {
    this.stub = new aiDescriptor.kadabra.BattleService(`localhost:${port}`, grpc.credentials.createInsecure());
  }

  private _selectAction(room: string, activeMoves: IMoveInfo[],
      sideInfo: ISideInfo, forceSwitch: boolean): Promise<IAction> {
    // Try promisifyAll?
    const request = {
      room: {name: room},
      move: activeMoves,
      sideInfo: convertSideInfoToProto(sideInfo),
      forceSwitch
    };
    logger.info('HELLO HELLO HELLO');
    logger.info(util.inspect(request, {showHidden: true, depth: null}));

    return Promise.fromCallback((callback) => {
      this.stub.selectAction(request, callback);
    }).then((response) => {
      logger.info(util.inspect(response, {showHidden: false, depth: null}));
      return actionResponseToIAction(response);
    });
  }

  /**
   * Returns the index of the lead to use.
   */
  chooseLead(): Promise<string> {
    return Promise.fromCallback((callback) => {
      this.stub.chooseLead({}, callback);
    }).then((response) => {
      return response.leadIndex.toString();
    });
  }

  selectAction(room: string, activeMoves: IMoveInfo[], sideInfo: ISideInfo): Promise<IAction> {
    return this._selectAction(room, activeMoves, sideInfo, false);
  }

  selectForceSwitchAction(room: string, sideInfo: ISideInfo): Promise<IAction> {
    return this._selectAction(room, [], sideInfo, true);
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
