import * as sourceMapSupport from 'source-map-support';
sourceMapSupport.install();

import { connect } from './showdown';
import { ShowdownDirector } from './showdown_director';
import { TeamClient } from './teamclient';
import { BattleClient } from './battle_client';
import { startServer } from './stadium_service';

import * as logger from 'winston';
logger.cli();

const LOCALHOST_URL = 'ws://localhost:8000/showdown/websocket';

const teamClient = new TeamClient(8080);
const battleClient = new BattleClient(8080);

startServer(LOCALHOST_URL, teamClient, battleClient);
