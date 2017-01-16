const WebSocket = require('ws');

const URL = 'ws://localhost:8000/showdown/websocket';
const connection = new WebSocket(URL);

connection.on('open', function() {
  console.log('connected');
});

connection.on('message', function(data, flags) {
  if (flags.binary) {
    console.log('This is odd: received binary data.');
    return;
  }
  console.log('Received a message!\n' + data);

  if (data.includes('challstr')) {
    console.log('Let us try this');
    connection.send('hello buddy');
  }
});
