/**
 * @fileOverview A high-level class which handles a connection to Showdown.
 */

import * as _ from 'lodash';
import * as Promise from 'bluebird';
import * as request from 'request-promise';
import * as logger from 'winston';

import { Result, IBattleOutcome } from './states';
import { ShowdownConnection, ShowdownMessage } from './showdown';
import { Team } from './team';
import { TeamClient } from './teamclient';
import { BattleClient } from './battle_client';
import { BattleDirector } from './battle_director';

const CENTRAL_SERVER_HOSTNAME = 'play.pokemonshowdown.com';
const BATTLE_MESSAGE_TYPES = new Set([
  'choice',
  'clearpoke',
  'detailschange',
  'faint',
  'gametype',
  'gen',
  'immune',
  'move',
  'player',
  'poke',
  'rated',
  'request',
  'rule',
  'teampreview',
  'tier',
  'turn',
  'upkeep',
  'start',
  'seed',
  'switch',
  'win',
  '-ability',
  '-boost',
  '-damage',
  '-heal',
  '-immune',
  '-mega',
  '-resisted',
  '-supereffective',
  '-unboost'
]);

/**
 * A private helper class to manage authentication status.
 */
class LoginStatus {
  challstr: Promise<String>;
  _resolve_challstr: (string) => void;
  _challstr_is_set: boolean;

  constructor() {
    this.challstr = new Promise((resolve) => {
      this._resolve_challstr = resolve;
    }) as Promise<String>;
    this._challstr_is_set = false;
  }

  setChallstr(challstr: string): void {
    if (this._challstr_is_set) {
      throw Error('Cannot set challstr; it is already set');
    }
    this._challstr_is_set = true;
    this._resolve_challstr(challstr);
  }
}

interface IChallenges {
  challengesFrom: {
    [key: string]: string
  };
  // I don't actually know this format right now
  challengesTo: any;
}

export class ShowdownDirector {
  connection: ShowdownConnection;
  teamClient: TeamClient;
  battleClient: BattleClient;
  challenges: IChallenges;
  _loginStatus: LoginStatus;
  _ourUsername: string;
  private _outcomeHandles:
      Array<(resolution: IBattleOutcome | Promise.Thenable<IBattleOutcome>) => void>
      = [];
  private _battleDirector: BattleDirector;

  /**
   * @param {!Showdown} connection Connection to Showdown.
   * @param {!TeamClient} teamClient
   * @param {!BattleClient} battleClient
   */
  constructor(connection, teamClient, battleClient) {
    this.connection = connection;
    this.teamClient = teamClient;
    this.battleClient = battleClient;
    this.challenges = {
      challengesFrom: {},
      challengesTo: {}
    };
    this._loginStatus = new LoginStatus();

    this.connection.on('message', this.handleMessage.bind(this));
  }

  /**
   * Handles an incoming message. Sometimes kicks off async actions.
   *
   * @param {!ShowdownMessage}
   */
  handleMessage(message: ShowdownMessage): void {
    if (!message.splitLines) {
      return;
    }

    if (!message.splitLines) {
      return;
    }

    message.splitLines.forEach((submessage) => {
      switch (submessage[0]) {
        case 'formats': {
          console.log('Detected formats message, but we do not handle it yet');
          break;
        }
        case 'challstr': {
          const challstr = submessage.slice(1).join('|');
          this._loginStatus.setChallstr(challstr);
          break;
        }
        case 'updatechallenges': {
          this.challenges = JSON.parse(submessage[1]);
          break;
        }
        case 'popup': {
          const popupLines = _.slice(submessage, 1);
          console.log('POP-UP:\n' + popupLines.join('\n'));
          break;
        }
        case 'init': {
          if (submessage[1] === 'battle') {
            logger.info(`Entered battle ${message.header}`);
            logger.error(`battle client ${this.battleClient}`);
            this._battleDirector = new BattleDirector(
              message.header,
              this._ourUsername,
              this.battleClient,
              this.connection);
            this._associateAndClearChallengeOutcomeHandles();
          } else {
            logger.info(`Entered chatroom ${message.header}`);
          }
          break;
        }
        default: {
          if (BATTLE_MESSAGE_TYPES.has(submessage[0])) {
            if (!this._battleDirector) {
              throw Error(`Received message ${submessage}, but we don't expect to be in a battle`);
            }
            this._battleDirector.handleMessage(submessage[0], submessage.slice(1));
            break;
          }

          logger.info('Received unhandled message', submessage[0]);
        }
      }
    });
  }

  /**
   * Challenge another user.
   */
  challenge(meta: string, challengedUser: string): Promise<IBattleOutcome> {
    return this.teamClient.getTeam(meta)
        .then((team) => this._useTeam(team))
        .then(() => this.connection.send(`|/challenge ${challengedUser}, ${meta}`))
        .then(() => this._nextOutcome());
  }

  /**
   * Consider accepting a challenge from someone who is challenging us.
   */
  considerAcceptingChallenge(): Promise<IBattleOutcome> {
    const challenges = this.challenges.challengesFrom;

    // For now, just consider the first challenger.
    if (_.isEmpty(challenges)) {
      return Promise.resolve({
        result: Result.NO_RESULT,
        logs: []
      });
    }
    const challenger = Object.keys(challenges)[0];
    const meta = challenges[challenger];

    return this.teamClient.getTeam(meta)
        .then((team) => this._useTeam(team))
        .then(() => this.connection.send(`|/accept ${challenger}`))
        .then(() => this._nextOutcome());

  }

  _nextOutcome(): Promise<IBattleOutcome> {
    if (this._battleDirector) {
      return this._battleDirector.getOutcome();
    } else {
      const promise = new Promise((resolve) => this._outcomeHandles.push(resolve));
      return promise as Promise<IBattleOutcome>;
    }
  }

  private _associateAndClearChallengeOutcomeHandles(): void {
    const challengeOutcomePromise = this._battleDirector.getOutcome();
    this._outcomeHandles.forEach((resolve) => resolve(challengeOutcomePromise));
    this._outcomeHandles.splice(0);
  }

  /**
   * Sends a message selecting this team for battle.
   *
   * This must be called before challenging someone or accepting a challenge.
   */
  private _useTeam(team: Team): Promise<void> {
      // TODO: Add error handling if team is rejected by server.
      const useteam = '|/utm ' + team.toShowdownPayload();
      return this.connection.send(useteam);
  }

  /**
   * Sets our username.
   *
   * This is somewhat different from logging in, since we aren't logging into
   * a passworded, tracked user. We're just setting our own name.
   *
   * @param {string} username The intended username.
   * @return {!Promise} The outcome of logging in.
   */
  setUsername(username): Promise<void> {
    console.log(`Starting the process of logging in as ${username}`);
    return this._loginStatus.challstr
        .then((challstr) => {
          const urlString = CENTRAL_SERVER_HOSTNAME + '/~~localhost/action.php';
          const queryString = `?act=getassertion&userid=${username}&challstr=${challstr}`;
          console.log(`Logging in as ${username} with challstr ${challstr}`);

          const options = {
            uri: `https://${urlString}${queryString}`,
            resolveWithFullResponse: true
          };
          return request.post(options)
              .then((res) => {
                const assertion = res.body;
                console.log('Got assertion', assertion);

                return this.connection.send(`|/trn ${username},0,${assertion}`);
              });
        })
        .then(() => {
          this._ourUsername = username;
        });
  }

  /**
   * Join automatically entered rooms.
   *
   * @return {!Promise}
   */
  autoJoin(): Promise<void> {
    return this.connection.send('|/autojoin');
  }
}
