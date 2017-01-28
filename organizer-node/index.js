const _ = require('lodash');
const WebSocket = require('ws');

const showdown = require('./showdown');
const TeamClient = require('./teamclient');

const LOCALHOST_URL = 'ws://localhost:8000/showdown/websocket';

const GIANT_UTM_STRING = '|/utm |alakazam|alakazite||psychic,focusblast,shadowball,substitute|Timid|,,,252,4,252||,0,,,,|||]|skarmory|leftovers|1|roost,spikes,bravebird,whirlwind|Impish|252,,252,,,4|||||]|garchomp|choiceband|H|earthquake,outrage,stoneedge,poisonjab|Jolly|,252,,,4,252|||||]|marowakalola|thickclub|1|bonemerang,flareblitz,ironhead,ironhead|Adamant|248,252,,,8,|||||]|nihilego|leftovers||powergem,sludgebomb,toxicspikes,rest|Modest|252,,,252,4,||,0,,,,|||]|togekiss|leftovers|1|airslash,nastyplot,roost,thunderwave|Calm|252,,,4,252,||,0,,,,|||'

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

          // First send 'utm'.
          connection.send(GIANT_UTM_STRING)
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

// const teamClient = new TeamClient();
// teamClient.getTeam('ou')
//     .then((team) => {
//       console.log(team);
//     })
//     .catch((err) => {
//       console.log(err);
//     })


// showdown().then((connection) => {
//   connection.on('message', function(message) {
//     console.log(message);
//   });
// })
// .catch((err) => {
//   console.log('EVERYTHING BROKE', err);
// });
