import * as Chai from 'chai';
const expect = Chai.expect;

import {get_moves, get_side_info} from './parse_request';

const MAROWAK = {
  ident: 'p1: Marowak',
  details: 'Marowak-Alola, M',
  condition: '164/261',
  active: true,
  stats: {
    atk: 284,
    def: 256,
    spa: 122,
    spd: 198,
    spe: 126
  },
  moves: ['bonemerang', 'flareblitz', 'ironhead'],
  baseAbility: 'cursedbody',
  item: 'thickclub',
  pokeball: 'pokeball'
};

const SKARMORY = {
  ident: 'p1: Skarmory',
  details: 'Skarmory, F',
  condition: '0 fnt',
  active: true,
  stats: {
    atk: 196,
    def: 416,
    spa: 104,
    spd: 176,
    spe: 177
  },
  moves: ['roost', 'spikes', 'bravebird', 'whirlwind'],
  baseAbility: 'keeneye',
  item: 'leftovers',
  pokeball: 'pokeball'
};

describe('Request parsing', function() {
  describe('#get_moves', function() {
    it('gets moves', function() {
      const request = {
        active: [{
          moves: [{
            move: 'Bonemerang',
            id: 'bonemerang',
            pp: 15,
            maxpp: 16,
            target: 'normal',
            disabled: false
          }, {
            move: 'Flare Blitz',
            id: 'flareblitz',
            pp: 24,
            maxpp: 24,
            target: 'normal',
            disabled: true
          }]
        }],
        side: {name: 'abraca001', id: 'p1', pokemon: [MAROWAK]},
      rqid: 2};
      const moves = get_moves(request);

      expect(moves).to.be.of.length(2);

      const bonemerang = moves[0];
      expect(bonemerang.id).to.equal('bonemerang');
      expect(bonemerang.pp).to.equal(15);
      expect(bonemerang.maxpp).to.equal(16);
      expect(bonemerang.disabled).to.equal(false);

      const flareblitz = moves[1];
      expect(flareblitz.id).to.equal('flareblitz');
      expect(flareblitz.pp).to.equal(24);
      expect(flareblitz.maxpp).to.equal(24);
      expect(flareblitz.disabled).to.equal(true);
    });
  });

  describe('#get_side_info', function() {
    it('works when no active pokemon', function() {
      const request = {
        forceSwitch: [true],
        side: {name: 'abraca001', id: 'p1', pokemon: [SKARMORY, MAROWAK]},
        rqid: 2
      };
      const sideInfo = get_side_info(request);
      expect(sideInfo.team).to.be.of.length(2);

      const skarmoryInfo = sideInfo.team[0];
      expect(skarmoryInfo.species).to.equal('Skarmory');
      expect(skarmoryInfo.hp).to.equal(0);
      expect(skarmoryInfo.maxHp).to.be.null;
      expect(skarmoryInfo.fainted).to.be.true;
      expect(skarmoryInfo.item).to.equal('leftovers');

      const marowakInfo = sideInfo.team[1];
      expect(marowakInfo.species).to.equal('Marowak-Alola');
      expect(marowakInfo.hp).to.equal(164);
      expect(marowakInfo.maxHp).to.equal(261);
      expect(marowakInfo.fainted).to.be.false;
      expect(marowakInfo.item).to.equal('thickclub');
    });
  });
});
