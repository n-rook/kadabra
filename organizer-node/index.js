const WebSocket = require('ws');

const showdown = require('./showdown');
const TeamClient = require('./teamclient');

const teamClient = new TeamClient();
teamClient.getTeam('ou')
    .then((team) => {
      console.log(team);
    })
    .catch((err) => {
      console.log(err);
    })


// showdown().then((connection) => {
//   connection.on('message', function(message) {
//     console.log(message);
//   });
// })
// .catch((err) => {
//   console.log('EVERYTHING BROKE', err);
// });
