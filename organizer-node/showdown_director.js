/**
 * @fileOverview A high-level class which handles a connection to Showdown.
 */

const _ = require('lodash');

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
}

module.exports = ShowdownDirector;
