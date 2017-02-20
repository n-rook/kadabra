/**
 * Functions to aid in converting Pokemon Showdown's data to JSON.
 */

/**
 * Converts an object to a "plain old Javascript object": an object which can
 * easily be converted to JSON without losing any data.
 */
export function convertToSimpleObject(data: {}): {} {
  const keys = Object.keys(data);
  const returnObject = {};
  keys.forEach((key) => {
    returnObject[key] = JSON.parse(JSON.stringify(data[key], trueReplacer));
  });
  return returnObject
}

/**
 * Converts an object to a JSON string, with the rules followed by
 * convertToSimpleObject.
 */
export function convertToJson(data: {}): string {
  return JSON.stringify(convertToSimpleObject(data), undefined, 2);
}

/**
 * A replacer function which converts functions to the boolean value true.
 *
 * This function is a "replacer", suitable for use as the "replacer" argument to the
 * JSON.stringify function. It's used to give a clue that complex logic is
 * present in the data being examined.
 */
function trueReplacer(key, value) {
  if (typeof(value) === 'function') {
    return true;
  }
  return value;
}
