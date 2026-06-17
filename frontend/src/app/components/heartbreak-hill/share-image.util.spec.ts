import { formatTempo, shareFileName } from './share-image.util';

describe('share-image.util', () => {
  describe('formatTempo', () => {
    it('formats ride speed as km/h with one decimal (de comma)', () => {
      expect(formatTempo('RIDE', 23.42, null, 'de-DE')).toBe('23,4 km/h');
    });
    it('formats ride speed with a decimal point in en', () => {
      expect(formatTempo('RIDE', 23.42, null, 'en-GB')).toBe('23.4 km/h');
    });
    it('formats run pace as m:ss /km', () => {
      expect(formatTempo('RUN', null, 275, 'de-DE')).toBe('4:35 /km');
      expect(formatTempo('RUN', null, 309, 'de-DE')).toBe('5:09 /km');
    });
    it('returns an em dash when the relevant value is missing', () => {
      expect(formatTempo('RIDE', null, null, 'de-DE')).toBe('—');
      expect(formatTempo('RUN', 20, null, 'de-DE')).toBe('—');
    });
  });

  describe('shareFileName', () => {
    it('builds a per-activity filename', () => {
      expect(shareFileName(6, 'RIDE')).toBe('heartbreak-hill-rang6-rad.png');
      expect(shareFileName(12, 'RUN')).toBe('heartbreak-hill-rang12-lauf.png');
    });
  });
});
