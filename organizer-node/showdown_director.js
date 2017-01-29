/**
 * @fileOverview A high-level class which handles a connection to Showdown.
 */

const Promise = require('bluebird');

const request = require('request-promise');

const _ = require('lodash');

const CENTRAL_SERVER_HOSTNAME = 'play.pokemonshowdown.com';

class LoginStatus {
  constructor() {
    this.challstr = new Promise((resolve) => {
      this._resolve_challstr = resolve;
    });
    this._challstr_is_set = false;
  }

  setChallstr(challstr) {
    if (this._challstr_is_set) {
      throw Error('Cannot set challstr; it is already set');
    }
    this._challstr_is_set = true;
    this._resolve_challstr(challstr);
  }
}

class ShowdownDirector {
  /**
   * @param {!Showdown} connection Connection to Showdown.
   * @param {!TeamClient} teamClient
   */
  constructor(connection, teamClient) {
    this.connection = connection;
    this.teamClient = teamClient;
    this.challenges = {
      challengesFrom: {},
      challengesTo: {}
    };
    this._loginStatus = new LoginStatus();

    this.connection.on('message', this.handleMessage.bind(this));
  }

  /**
   * @param {!Showdown.Message}
   */
  handleMessage(message) {
    if (!message.splitLines) {
      return;
    }

    console.log(message);

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
          // const challstr = submessage[2];
          console.log(`Received challenge string: ${challstr}`);
          this._loginStatus.setChallstr(challstr);
          break;
        }
        case 'updatechallenges': {
          this.challenges = JSON.parse(submessage[1]);
          this.considerAcceptingChallenge();
          break;
        }
        case 'popup': {
          const popupLines = _.slice(submessage, 1);
          console.log('POP-UP:\n' + popupLines.join('\n'));
          break;
        }
        default: {
          console.log('Got message', submessage[0]);
        }
      }
    });
  }

  /**
   * Consider accepting a challenge from someone who is challenging us.
   */
  considerAcceptingChallenge() {
    const challenges = this.challenges.challengesFrom;
    
    // For now, just consider the first challenger.
    if (!challenges) {
      return;
    }
    const challenger = Object.keys(challenges)[0];
    const meta = challenges[challenger];
    
    this.teamClient.getTeam(meta)
        .then((team) => {
          // TODO: Add error handling if team is rejected by server.
          const useteam = '|/utm ' + team.toShowdownPayload();
          return this.connection.send(useteam);
        })
        .then(() => {
          return this.connection.send(`|/accept ${challenger}`);
        })
        .catch((err) => {
          console.log(`Failed to challenge ${challenger}: `, err);
        });
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
  setUsername(username) {
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
        });
  }

  /**
   * Join automatically entered rooms.
   *
   * @return {!Promise}
   */
  autoJoin() {
    return this.connection.send('|/autojoin');
  }
}

module.exports = ShowdownDirector;
