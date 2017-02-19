/**
 * Exports a single function which converts the Pokemon Showdown file
 * moves.js to a JSON file.
 */

function toSimpleObject(movesDex) {
  const moveIds = Object.keys(movesDex);
  const returnObject = {};
  moveIds.forEach((moveId) => {
    returnObject[moveId] = convertSingleMove(movesDex[moveId]);
  });
  return returnObject;
}

export function toJson(moves_js_path) {
  const simpleObject = toSimpleObject(require(moves_js_path));
  return JSON.stringify(simpleObject, undefined, 2);
}

function convertSingleMove(move) {
  const jsonTypeObject = JSON.parse(JSON.stringify(move, moveReplacer));
  return jsonTypeObject;
}

function moveReplacer(key, value) {
  // Keep keys like 'onhit' in the file, so at least we know there is something
  // funky going on.
  if (typeof(value) === 'function') {
    return true;
  }
  return value;
}

console.log(toJson('../../../showdown/data/moves'));
