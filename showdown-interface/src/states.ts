/**
 * Contains general-purpose data interfaces.
 */

import {ServerLog, SentMessage} from './log';

export enum Result {
  WIN,
  LOSS,
  // Returned when the battle never even happened.
  NO_RESULT
}

export class IBattleOutcome {
  public readonly result: Result;
  public readonly logs: Array<ServerLog|SentMessage>;
}

export class IMoveInfo {
  id: string;
  pp: number;
  maxpp: number;
  disabled: boolean;
}

export class ISideInfo {
  team: IPokemonSideInfo[];
}

export class IPokemonSideInfo {
  species: string;
  hp: number;
  // Sometimes null, if max HP is unknown (especially if we fainted)
  maxHp: number;
  fainted: boolean;
  // Whether or not this is the active Pokemon.
  active: boolean;
  item: string;
}
