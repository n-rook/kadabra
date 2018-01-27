/**
 * Provides a friendlier wrapper for the stub to the AI server.
 */

import * as path from 'path';
import * as util from 'util';

import * as grpc from 'grpc';
import * as Promise from 'bluebird';
import * as logger from 'winston';

import { IMoveInfo, ISideInfo } from './states';
import { ServerLog, SentMessage } from './log';
import { aiFile } from './proto_constants';
import { IPokemonSpec } from './team';

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
    this.stub = Promise.promisifyAll(
      new aiDescriptor.kadabra.BattleService(
        `localhost:${port}`, grpc.credentials.createInsecure()));
  }

  private _selectAction(
      room: string,
      activeMoves: IMoveInfo[],
      sideInfo: ISideInfo,
      logs: ReadonlyArray<ServerLog|SentMessage>,
      forceSwitch: boolean,
      teamSpec: IPokemonSpec[]): Promise<IAction> {
    console.log('Sending team spec:\n', teamSpec);

    const request = {
      room: {name: room},
      move: activeMoves,
      sideInfo: convertSideInfoToProto(sideInfo),
      forceSwitch,
      log: logs.map((m) => m.toLogLine()),
      teamSpec
    };

    return this.stub.selectActionAsync(request)
        .then((response) => actionResponseToIAction(response));
  }

  /**
   * Returns the index of the lead to use.
   */
  chooseLead(): Promise<string> {
    return this.stub.chooseLeadAsync({})
      .then((response) => response.leadIndex.toString());
  }

  selectAction(
      room: string,
      activeMoves: IMoveInfo[],
      sideInfo: ISideInfo,
      team: IPokemonSpec[],
      logs: ReadonlyArray<ServerLog|SentMessage>): Promise<IAction> {
    return this._selectAction(room, activeMoves, sideInfo, logs, false, team);
  }

  selectForceSwitchAction(
      room: string,
      sideInfo: ISideInfo,
      team: IPokemonSpec[],
      logs: ReadonlyArray<ServerLog|SentMessage>,
      ): Promise<IAction> {
    return this._selectAction(room, [], sideInfo, logs, true, team);
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
