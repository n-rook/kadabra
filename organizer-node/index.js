const _ = require('lodash');
const WebSocket = require('ws');

const showdown = require('./showdown');
const TeamClient = require('./teamclient');

const LOCALHOST_URL = 'ws://localhost:8000/showdown/websocket';

const teamClient = new TeamClient();
showdown(LOCALHOST_URL).then((connection) => {
  connection.on('message', function(message) {
    console.log(message);

    if (!message.splitLines) {
      return;
    }

    message.splitLines.forEach((submessage) => {
      switch (submessage[0]) {
        case 'formats': {
          console.log('Detected formats message');
          if (_.includes(submessage, '[Gen 7] OU,e')) {
            console.log('Oh hey, OU is detected');
          }
          break;
        }
        case 'updatechallenges': {
          console.log('We got challenged');
          const challengeData = JSON.parse(submessage[1]);
          console.log('We are challenged by these folks', Object.keys(challengeData.challengesFrom));
          console.log('Well, we should get them back!');

          teamClient.getTeam('ou')
              .then((team) => {
                const utmString = '|/utm ' + team.toShowdownPayload();
                console.log('Sending UTM string', utmString);
                return connection.send(utmString);
              })
              .then(() => {
                const sendString = '|/accept ' + Object.keys(challengeData.challengesFrom)[0]
                return connection.send(sendString);
              });
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
  });
});
