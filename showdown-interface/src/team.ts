import * as _ from 'lodash';

function convertMoveName(name) {
  // Convert a real move name to the Showdown move name.
  const nospaces = name.replace(/\s+/g, '');
  return nospaces.toLowerCase();
}

function convertNature(name) {
  return _.capitalize(name);
}

function convertEvs(evs) {
  return toShowdownInColumnArray(statObjectToList(evs).map((n) => n.toString()), 6);
}

function convertIvs(ivs) {
  // Currently this is the same as for EVs.
  return convertEvs(ivs);
}

// Converts an object with hp, attack, defense, specialAttack, specialDefense,
// and speed parameters to an array.
function statObjectToList(statObject) {
  return [
    statObject.hp,
    statObject.attack,
    statObject.defense,
    statObject.specialAttack,
    statObject.specialDefense,
    statObject.speed];
}

function toShowdownInColumnArray(values, size) {
  if (size < values.size) {
    throw Error(
      `Could not convert array ${values} to showdown; expected size ${size}`);
  }

  const paddedValues = [];
  for (let i = 0; i < size; i++) {
    paddedValues.push(values[i] || '');
  }
  return values.join(',');
}

/**
 * A PokemonSpec proto in JSON form.
 */
export class IPokemonSpec {
  // It would be nice to define this...
  species
  item
  move
  nature
  evs
  ivs
}

/**
 * A TeamSpec object. Often sent as a response to certain RPCs.
 */
export class ITeamProto {
  pokemon: IPokemonSpec[]
}

export class Team {
  teamObject: ITeamProto;

  constructor(teamObject) {
    this.teamObject = teamObject;
  }

  toShowdownPayload(): String {
    const individualPayloads = this.teamObject.pokemon.map((data) => {
      const moves = toShowdownInColumnArray(data.move.map(convertMoveName), 4);

      const columnList = [
        data.species,
        data.item,
        '', // ability. empty, 1, or H
        moves,
        convertNature(data.nature),
        convertEvs(data.evs),
        '', // unknown
        convertIvs(data.ivs),
        '', // unknown
        '' // unknown
      ];

      return columnList.join('|');
    });

    return '|' + individualPayloads.join('|]|') + '|';
  }
}
