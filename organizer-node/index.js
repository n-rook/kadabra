const WebSocket = require('ws');

const showdown = require('./showdown');
const TeamClient = require('./teamclient')

const teamClient = new TeamClient();
teamClient.getTeam('magikarp');


// showdown().then((connection) => {
//   connection.on('message', function(message) {
//     console.log(message);
//   });
// })
// .catch((err) => {
//   console.log('EVERYTHING BROKE', err);
// });
