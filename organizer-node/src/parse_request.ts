/**
 * Contains logic for parsing the JSON returned by the |request| Showdown message.
 */

import * as _ from 'lodash';

import {IMoveInfo} from './states';

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
