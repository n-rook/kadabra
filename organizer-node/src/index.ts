import { connect } from './showdown';
import { ShowdownDirector } from './showdown_director';
import { TeamClient } from './teamclient';

import * as logger from 'winston';
logger.cli();

const LOCALHOST_URL = 'ws://localhost:8000/showdown/websocket';

const teamClient = new TeamClient(8080);
connect(LOCALHOST_URL).then((connection) => {
  return new ShowdownDirector(connection, teamClient);
}).then((director) => {
  return director.setUsername('abraca001')
      .then(() => director.autoJoin());
}).then(() => {
  logger.info('Successfully joined server');
})
.catch((err) => {
  logger.error('Failed to join server', err);
});
