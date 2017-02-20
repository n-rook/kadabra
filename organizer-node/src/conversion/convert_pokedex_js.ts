/**
 * This script converts Showdown's pokedex.js to a JSON file.
 */

import { convertToJson } from './convert';

const movesDex = require('../../../showdown/data/pokedex');
console.log(convertToJson(movesDex));
