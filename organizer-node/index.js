const WebSocket = require('ws');

const showdown = require('./showdown');

showdown().then((connection) => {
  connection.on('message', function(message) {
    console.log(message);
  });
})
.catch((err) => {
  console.log('EVERYTHING BROKE', err);
});
