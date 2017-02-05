import * as Chai from 'chai';
const expect = Chai.expect;

import {get_moves} from './parse_request';

const MAROWAK_ENTRY = {
  ident: 'p1: Marowak',
  details: 'Marowak-Alola, M',
  condition: '261/261',
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

describe('Request parsing', function() {
  describe('#getMoves', function() {
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
        side: {name: 'abraca001', id: 'p1', pokemon: [MAROWAK_ENTRY]},
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
});
