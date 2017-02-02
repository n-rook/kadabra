
import * as logger from 'winston';
import * as Promise from 'bluebird';

import { BattleClient } from './ai_client';
import { ShowdownConnection } from './showdown';

/**
 * A helper class that sends messages to a specific room.
 */
class RoomMessageSender {
  private room: string;
  private connection: ShowdownConnection;

  constructor(room: string, connection: ShowdownConnection) {
    this.room = room;
    this.connection = connection;
  }

  send(message: string): Promise<void> {
    return this.connection.send(`${this.room}|${message}`);
  }
}

/**
 * The battle director collects information regarding a battle, and passes
 * it along to the AI server backing this bot.
 */
export class BattleDirector {

  private room: string;
  private ourUsername: string;
  private battleClient: BattleClient;
  private sender: RoomMessageSender;
  
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
  handleMessage(messageClass: string, message: string[]) {
    switch (messageClass) {
      case 'player': {
        if (message[0] === 'p1') {
          this.battleMetadataBuilder['player1'] = message[1];
        } else if (message[0] === 'p2') {
          this.battleMetadataBuilder['player2'] = message[1];
        } else {
          throw Error('Strange player message: ' + message);
        }
        break;
      }
      case 'gametype': {
        this.battleMetadataBuilder['gametype'] = message[0];
        break;
      }
      case 'gen': {
        this.battleMetadataBuilder['generation'] = message[0];
        break;
      }
      case 'tier': {
        this.battleMetadataBuilder['tier'] = message[0];
        break;
      }
      case 'rated': {
        this.battleMetadataBuilder['rated'] = true;
        break;
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
        break;
      }
      case 'request': {
        switch (this.battleState) {
          case IBattleState.BATTLE_PREPARATIONS:
            this.pendingTeamBuilderRequest = message[0];
            break;
          case IBattleState.TEAM_PREVIEW:
            // TODO: test with team whose nickname contains a | symbol
            this.handleRequest(message[0]);
            break;
          default:
            throw Error(`Cannot handle request in state ${this.battleState}`);
        }
        break;
      }
      case 'j':
      case 'join':
      case 'title':
      case 'clearpoke':
      case 'seed':
      case 'poke':
      case 'rule': {
        logger.info('Received unhandled battle message', messageClass);
        break;
      }
      default: {
        throw Error(`Received unexpected message ${messageClass}, ${message}`);
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

  private handleRequest(request: string): void {
    logger.info('Handling request...');
    const parsedRequest = JSON.parse(request);
    if (parsedRequest.teamPreview) {
      this.battleClient.chooseLead()
          .then((leadIndex) => {
            this.shiftStateTo(IBattleState.START_OF_TURN);
            return this.sender.send(`/team ${leadIndex}`);
          });
    }
  }
}

function buildBattleMetadata(battleMetadataBuilder: {}, ourUsername: string): IBattleMetadata {
  logger.error(`${battleMetadataBuilder}`);
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
