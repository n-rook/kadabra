
import * as Chai from 'chai';
const expect = Chai.expect;

import * as sinon from 'sinon';

import {BattleClient} from './battle_client';
import {BattleDirector} from './battle_director';
import {ShowdownConnection} from './showdown';

const REAL_INIT_LINES = [
  '|title|abraca001 vs. nrook',
  '|j|abraca001',
  '|j|nrook',
  '|title|abraca001 vs. nrook',
  '|player|p1|abraca001|1',
  '|request|{"teamPreview":true,"side":{"name":"abraca001","id":"p1","pokemon":[{"ident":"p1: Alakazam","details":"Alakazam, F","condition":"251/251","active":true,"stats":{"atk":122,"def":126,"spa":369,"spd":227,"spe":372},"moves":["psychic","focusblast","shadowball","substitute"],"baseAbility":"synchronize","item":"alakazite","pokeball":"pokeball"},{"ident":"p1: Skarmory","details":"Skarmory, F","condition":"271/271","active":false,"stats":{"atk":196,"def":416,"spa":104,"spd":176,"spe":177},"moves":["roost","spikes","bravebird","whirlwind"],"baseAbility":"keeneye","item":"leftovers","pokeball":"pokeball"},{"ident":"p1: Garchomp","details":"Garchomp, F","condition":"357/357","active":false,"stats":{"atk":359,"def":226,"spa":176,"spd":207,"spe":333},"moves":["earthquake","outrage","stoneedge","poisonjab"],"baseAbility":"sandveil","item":"choiceband","pokeball":"pokeball"},{"ident":"p1: Marowak","details":"Marowak-Alola, M","condition":"261/261","active":false,"stats":{"atk":284,"def":256,"spa":122,"spd":198,"spe":126},"moves":["bonemerang","flareblitz","ironhead"],"baseAbility":"cursedbody","item":"thickclub","pokeball":"pokeball"},{"ident":"p1: Nihilego","details":"Nihilego","condition":"359/359","active":false,"stats":{"atk":127,"def":130,"spa":388,"spd":299,"spe":242},"moves":["powergem","sludgebomb","toxicspikes","rest"],"baseAbility":"beastboost","item":"leftovers","pokeball":"pokeball"},{"ident":"p1: Togekiss","details":"Togekiss, F","condition":"311/311","active":false,"stats":{"atk":122,"def":226,"spa":277,"spd":361,"spe":196},"moves":["airslash","nastyplot","roost","thunderwave"],"baseAbility":"hustle","item":"leftovers","pokeball":"pokeball"}]},"rqid":1}',
  '|player|p2|nrook|2',
  '|gametype|singles',
  '|gen|7',
  '|tier|[Gen 7] OU',
  '|seed|',
  '|clearpoke',
  '|poke|p1|Alakazam, F|item',
  '|poke|p1|Skarmory, F|item',
  '|poke|p1|Garchomp, F|item',
  '|poke|p1|Marowak-Alola, M|item',
  '|poke|p1|Nihilego|item',
  '|poke|p1|Togekiss, F|item',
  '|poke|p2|Alakazam, F|item',
  '|poke|p2|Skarmory, M|item',
  '|poke|p2|Garchomp, M|item',
  '|poke|p2|Marowak-Alola, F|item',
  '|poke|p2|Nihilego|item',
  '|poke|p2|Togekiss, M|item',
  '|rule|Baton Pass Clause: Limit one Baton Passer, can\'t pass Spe and other stats simultaneously',
  '|rule|Sleep Clause Mod: Limit one foe put to sleep',
  '|rule|Species Clause: Limit one of each Pokémon',
  '|rule|OHKO Clause: OHKO moves are banned',
  '|rule|Moody Clause: Moody is banned',
  '|rule|Evasion Moves Clause: Evasion moves are banned',
  '|rule|Endless Battle Clause: Forcing endless battles is banned',
  '|rule|HP Percentage Mod: HP is shown in percentages',
  '|teampreview'
];

describe('BattleDirector', function() {

  let battleClient: any;
  let showdownConnection: any;

  beforeEach(function() {
    battleClient = sinon.createStubInstance(BattleClient);
    showdownConnection = sinon.createStubInstance(ShowdownConnection);
  });

  it('initialization', function() {
    const director = new BattleDirector('room', 'abraca001', battleClient, showdownConnection);

    battleClient.chooseLead.returns(Promise.resolve(1));
    showdownConnection.send.returns(Promise.resolve());

    const promises = REAL_INIT_LINES.map((line) => {
      const splitLine = line.split('|');
      return director.handleMessage(splitLine[1], splitLine.slice(2));
    });

    return Promise.all(promises).then(() => {
      sinon.assert.calledWith(showdownConnection.send, 'room|/team 1');
    });
  });
});
