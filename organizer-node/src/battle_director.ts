
import * as logger from 'winston';
import * as Promise from 'bluebird';

import { BattleClient } from './ai_client';
import { get_moves, get_side_info } from './parse_request';
import { ShowdownConnection } from './showdown';

/**
 * A helper class that sends messages to a specific room.
 */
class RoomMessageSender {
  private readonly room: string;
  private readonly connection: ShowdownConnection;

  constructor(room: string, connection: ShowdownConnection) {
    this.room = room;
    this.connection = connection;
  }

  send(message: string): Promise<void> {
    const ret = this.connection.send(`${this.room}|${message}`);
    return ret;
  }
}

/**
 * A class which tracks the battle's outcome: whether we won or lost.
 */
class OutcomeTracker {

  /**
   * A promise containing the outcome of the game.
   *
   * True if we win; false if we lose; an error if we hit an error.
   */
  public readonly outcome: Promise<boolean>;

  private state: String = 'NOT_DECIDED';
  private resolve;
  private reject;

  constructor() {
    this.outcome = new Promise((resolve, reject) => {
      this.resolve = resolve;
      this.reject = reject;
    }) as Promise<boolean>;
  }

  /**
   * Record that we have won.
   */
  win() {
    if (this.state !== 'NOT_DECIDED') {
      throw Error(`Cannot have won: state already recorded as ${this.state}`);
    }
    this.state = 'WON';
    this.resolve(true);
  }

  /**
   * Record that we have lost.
   */
  lose() {
    if (this.state !== 'NOT_DECIDED') {
      throw Error(`Cannot have lost: state already recorded as ${this.state}`);
    }
    this.state = 'LOST';
    this.resolve(false);
  }

  /**
   * Record an error.
   */
  recordError(err: Error) {
    logger.error(err as any);
    this.reject(err);
  }
}

/**
 * The battle director collects information regarding a battle, and passes
 * it along to the AI server backing this bot.
 */
export class BattleDirector {

  private readonly room: string;
  private readonly ourUsername: string;
  private readonly battleClient: BattleClient;
  private readonly sender: RoomMessageSender;
  private readonly outcomeTracker: OutcomeTracker;

  private battleState: IBattleState;
  private battleMetadata: IBattleMetadata;
  private battleMetadataBuilder: {};
  private pendingTeamBuilderRequest: string;

  /**
   * @param room The room we're in, like battle-gen7ou-82
   * @param ourUsername this user's username, so we know which user is us.
   */
  constructor(
    room: string,
    ourUsername: string,
    battleClient: BattleClient,
    connection: ShowdownConnection) {
    this.room = room;
    this.ourUsername = ourUsername;
    this.battleClient = battleClient;
    this.sender = new RoomMessageSender(room, connection);
    this.outcomeTracker = new OutcomeTracker();

    this.battleState = IBattleState.BATTLE_PREPARATIONS;
    this.battleMetadataBuilder = {};
  }

  /**
   * Handles an incoming battle message.
   *
   * @param messageClass The message header. For instance, if the message is
   *  |player|p1|abraca, this is "player".
   * @param message The remainder of the message. For instance, if the message
   *  is |player|p1|abraca, this is ["p1", "abraca"].
   */
  handleMessage(messageClass: string, message: string[]): Promise<void> {
    const f: () => Promise<void> = this._handleMessage.bind(this, messageClass, message);
    return Promise.try(f)
        .catch((err) => {
          this.outcomeTracker.recordError(err);
          logger.error('Error handling battle message', err, messageClass, message);
          throw err;
        });
  }

  /**
   * Returns a promise which fulfills as true if we win, or false if we lose.
   */
  getOutcome(): Promise<boolean> {
    return this.outcomeTracker.outcome;
  }

  /**
   * Like handleMessage, but may return either undefined or a promise.
   */
  private _handleMessage(messageClass: string, message: string[]): Promise<void>|void {
    switch (messageClass) {
      case 'player': {
        if (message[0] === 'p1') {
          this.battleMetadataBuilder['player1'] = message[1];
        } else if (message[0] === 'p2') {
          this.battleMetadataBuilder['player2'] = message[1];
        } else {
          throw Error('Strange player message: ' + message);
        }
        return;
      }
      case 'gametype': {
        this.battleMetadataBuilder['gametype'] = message[0];
        return;
      }
      case 'gen': {
        this.battleMetadataBuilder['generation'] = message[0];
        return;
      }
      case 'tier': {
        this.battleMetadataBuilder['tier'] = message[0];
        return;
      }
      case 'rated': {
        this.battleMetadataBuilder['rated'] = true;
        return;
      }
      case 'teampreview': {
        // It's not clear that this is a principled approach.
        // Watch carefully to see if 'teampreview' sometimes comes early.
        this.battleMetadata = buildBattleMetadata(this.battleMetadataBuilder, this.ourUsername);
        this.shiftStateTo(IBattleState.TEAM_PREVIEW);
        if (this.pendingTeamBuilderRequest) {
          this.handleRequest(this.pendingTeamBuilderRequest);
          delete this.pendingTeamBuilderRequest;
        }
        return;
      }
      case 'request': {
        switch (this.battleState) {
          case IBattleState.BATTLE_PREPARATIONS:
            this.pendingTeamBuilderRequest = message[0];
            return;
          case IBattleState.TEAM_PREVIEW:
            // TODO: test with team whose nickname contains a | symbol
            return this.handleRequest(message[0]);
          case IBattleState.START_OF_TURN:
            return this.handleRequest(message[0]);
          default:
            throw Error(`Cannot handle request in state ${this.battleState}`);
        }
      }
      case 'win': {
        const winner = message[0];
        if (this.ourUsername === winner) {
          logger.info('We won!');
          this.outcomeTracker.win();
        } else {
          logger.info(`${winner} won :(`);
          this.outcomeTracker.lose();
        }
        return;
      }
      default: {
        logger.info('Received unhandled battle message', messageClass);
        return;
      }
    }
  }

  private shiftStateTo(newState: IBattleState): void {
    if (!legalStateTransitions.get(this.battleState).has(newState)) {
      throw Error(`Cannot transition from ${this.battleState} to ${newState}`);
    }

    logger.info(`Shifted battle state from ${this.battleState} to ${newState}`);
    this.battleState = newState;
  }

  private handleRequest(request: string): Promise<void> {
    logger.info('Handling request...');
    const parsedRequest = JSON.parse(request);
    if (parsedRequest.teamPreview) {
      return this.battleClient.chooseLead()
          .then((leadIndex) => {
            this.shiftStateTo(IBattleState.START_OF_TURN);
            return this.sender.send(`/team ${leadIndex}`);
          });
    }

    if (parsedRequest.forceSwitch && parsedRequest.forceSwitch[0]) {
      // Kind of a dubious way to detect this case
      return this.battleClient.selectSwitchAfterFaintAction(
        this.room, get_side_info(parsedRequest))
        .then((switchIndex) => {
          this.sender.send(`/switch ${switchIndex}`);
        });
    }

    if (parsedRequest.wait) {
      logger.info('Received "wait" request; doing nothing.');
      return Promise.resolve();
    }

    // This probably catches too much
    return this.battleClient.selectAction(this.room, get_moves(parsedRequest))
        .then((moveIndex) => {
          // No shift state right now
          return this.sender.send(`/move ${moveIndex}`);
        });
  }
}

function buildBattleMetadata(battleMetadataBuilder: {}, ourUsername: string): IBattleMetadata {
  const metadata = {
    player1: checkSet(battleMetadataBuilder, 'player1'),
    player2: checkSet(battleMetadataBuilder, 'player2'),
    gametype: checkSet(battleMetadataBuilder, 'gametype'),
    generation: checkSet(battleMetadataBuilder, 'generation'),
    tier: checkSet(battleMetadataBuilder, 'tier'),
    rated: !!battleMetadataBuilder['rated']
  };

  if (ourUsername === metadata['player1']) {
    metadata['us'] = IPlayer.PLAYER1;
  } else if (ourUsername === metadata['player2']) {
    metadata['us'] = IPlayer.PLAYER2;
  } else {
    throw Error(`Neither ${metadata['player1']} nor ${metadata['player2']} is ` +
        `us, ${ourUsername}`);
  }

  return metadata as IBattleMetadata;
}

function checkSet(someObject: {}, key: string): any {
  if (!someObject[key]) {
    throw Error(`${key} unset`);
  }
  return someObject[key];
}

enum IBattleState {
  /**
   * When initial data is still coming in; that is, before we have
   * IBattleMetadata.
   */
  BATTLE_PREPARATIONS,

  /**
   * Initial data has come in; time to decide on a lead.
   */
  TEAM_PREVIEW,

  /**
   * The beginning of a turn, when you get to pick a move or switch.
   */
  START_OF_TURN
}

const legalStateTransitions: Map<IBattleState, Set<IBattleState>> = new Map([
  [IBattleState.BATTLE_PREPARATIONS, new Set([IBattleState.TEAM_PREVIEW])],
  [IBattleState.TEAM_PREVIEW, new Set([IBattleState.START_OF_TURN])]
]);

enum IPlayer {
  PLAYER1,
  PLAYER2
}

class IBattleMetadata {
  /**
   * Player 1's name.
   */
  player1: string;

  /**
   * Player 2's name.
   */
  player2: string;

  /**
   * Whether we are player 1 or player 2.
   */
  us: IPlayer;

  /**
   * The type of battle. We only handle 'singles'.
   */
  gametype: string;

  /**
   * The generation of play. Generally a small integer, like '7'.
   */
  generation: string;

  /**
   * The tier defining the ban list.
   */
  tier: string;

  /**
   * Whether this is a rated match.
   */
  rated: boolean;
}
