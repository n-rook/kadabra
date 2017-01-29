const showdown = require('./showdown');
const ShowdownDirector = require('./showdown_director');
const TeamClient = require('./teamclient');

const LOCALHOST_URL = 'ws://localhost:8000/showdown/websocket';

const teamClient = new TeamClient(8080);
showdown(LOCALHOST_URL).then((connection) => {
  return new ShowdownDirector(connection, teamClient);
}).then((director) => {
  return director.logIn('abraca001')
      .then(() => director.autoJoin());
}).then(() => {
  console.log('Yay!');
})
.catch((err) => {
  console.log(':(', err);
});
