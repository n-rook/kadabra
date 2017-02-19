/**
 * Exports a single function which converts the Pokemon Showdown file
 * moves.js to a JSON file.
 */

function toSimpleObject(moves_js_path: string) {
  const movesDex = require(moves_js_path);

  const moveIds = Object.keys(movesDex);
  const returnObject = {};
  moveIds.forEach((moveId) => {
    returnObject[moveId] = convertSingleMove(movesDex[moveId]);
  });
  return JSON.stringify(moveIds);
}

export function printJson(moves_js_path) {
  const simpleObject = require(moves_js_path);
  console.log(JSON.stringify(simpleObject));
}

function convertSingleMove(move) {
  const jsonTypeObject = JSON.parse(JSON.stringify(move));
  return jsonTypeObject;


    // if (move['basePowerCallback']) {
    //   jsonTypeObject['specialBasePower'] = true;
    // }
    // if (move['onHit']) {
    //   jsonTypeObject['specialOnHit'] = true;
    // }
    // if (move['onTryHit']) {
    //   jsonTypeObject[]
    // }
}

printJson('../../../../pokemonshowdown/data/moves');
