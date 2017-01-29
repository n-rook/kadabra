const _ = require('lodash');
const WebSocket = require('ws');

const showdown = require('./showdown');
const ShowdownDirector = require('./showdown_director');
const TeamClient = require('./teamclient');

const LOCALHOST_URL = 'ws://localhost:8000/showdown/websocket';

const teamClient = new TeamClient();
showdown(LOCALHOST_URL).then((connection) => {
  const showdownDirector = new ShowdownDirector(connection, teamClient);
});
