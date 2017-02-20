/**
 * This script converts Showdown's moves.js to a JSON file.
 */

import { convertToJson } from './convert';

const movesDex = require('../../../showdown/data/moves');
console.log(convertToJson(movesDex));
