/**
 * Contains logic for parsing the JSON returned by the |request| Showdown message.
 */

import * as _ from 'lodash';

import {IMoveInfo, ISideInfo, IPokemonSideInfo} from './states';

export function get_moves(parsedRequest: {}): IMoveInfo[] {
  if (!parsedRequest['active']) {
    throw Error('Request in unexpected state; no "active" field');
  }
  if (!parsedRequest['active'][0]) {
    throw Error('Request in unexpected state; no active Pokemon');
  }
  const active = parsedRequest['active'][0];

  if (!active['moves']) {
    throw Error('Request in unexpected state; no "moves" field in "active"');
  }
  const moves = active['moves'];
  return moves.map((data) => {
    if (
      !data.hasOwnProperty('id') ||
      !data.hasOwnProperty('pp') ||
      !data.hasOwnProperty('maxpp') ||
      !data.hasOwnProperty('disabled')) {

      throw Error(`Move data ${JSON.stringify(data)} has unexpected format`);
    }

    return {
      id: data.id,
      pp: data.pp,
      maxpp: data.maxpp,
      disabled: data.disabled
    };
  });
}

export function get_side_info(parsedRequest: {}): ISideInfo {
  if (!parsedRequest['side'] || !parsedRequest['side']['pokemon']) {
    throw Error('Side info is missing');
  }

  const pokemonInfo: IPokemonSideInfo[] = parsedRequest['side']['pokemon'].map(
    (data) => {
      const species: string = data.details.split(',')[0];
      const condition: string[] = data.condition.split(' ');
      const hpString = condition[0];
      const currentHp = parseInt(hpString.split('/')[0], 10);
      const maxHp = hpString.includes('/')
          ? parseInt(hpString.split('/')[1], 10)
          : null;

      const riders: string[] = condition.slice(1);
      const fainted = riders.includes('fnt');
      // We should investigate how this appears if there is no item.
      const item = data.item;

      return {
        species,
        hp: currentHp,
        maxHp,
        fainted,
        item
      };
    }
  );

  return {team: pokemonInfo};
}
