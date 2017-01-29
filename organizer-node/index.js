// const _ = require('lodash');

const showdown = require('./showdown');
const ShowdownDirector = require('./showdown_director');
const TeamClient = require('./teamclient');

const LOCALHOST_URL = 'ws://localhost:8000/showdown/websocket';

const teamClient = new TeamClient(8080);
showdown(LOCALHOST_URL).then((connection) => {
  const unused = new ShowdownDirector(connection, teamClient);
});
