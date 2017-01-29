import { connect } from './showdown';
import { ShowdownDirector } from './showdown_director';
import { TeamClient } from './teamclient';

const LOCALHOST_URL = 'ws://localhost:8000/showdown/websocket';

const teamClient = new TeamClient(8080);
connect(LOCALHOST_URL).then((connection) => {
  return new ShowdownDirector(connection, teamClient);
}).then((director) => {
  return director.setUsername('abraca001')
      .then(() => director.autoJoin());
}).then(() => {
  console.log('Yay!');
})
.catch((err) => {
  console.log(':(', err);
});
