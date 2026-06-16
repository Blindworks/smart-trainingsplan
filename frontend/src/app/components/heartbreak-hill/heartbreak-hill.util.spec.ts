import { buildElevationProfile, formatGap, formatGrade } from './heartbreak-hill.util';

describe('heartbreak-hill.util', () => {
  describe('formatGap', () => {
    it('formats a positive gap with a leading +', () => {
      expect(formatGap(46)).toBe('+0:46');
      expect(formatGap(135)).toBe('+2:15');
    });
    it('renders the leader gap (0) as a dash', () => {
      expect(formatGap(0)).toBe('—');
    });
    it('handles null as a dash', () => {
      expect(formatGap(null)).toBe('—');
    });
  });

  describe('formatGrade', () => {
    it('formats a percentage with one decimal and a percent sign', () => {
      expect(formatGrade(6.4)).toBe('6,4 %');
    });
    it('returns an em dash for null', () => {
      expect(formatGrade(null)).toBe('—');
    });
  });

  describe('buildElevationProfile', () => {
    it('returns null when there are fewer than 2 points', () => {
      expect(buildElevationProfile([], 400, 200)).toBeNull();
      expect(buildElevationProfile([[50, 8, 100]], 400, 200)).toBeNull();
    });

    it('maps points to an SVG line + area path scaled to the viewBox', () => {
      const pts: [number, number, number][] = [[50, 8, 100], [50.001, 8, 110], [50.002, 8, 120]];
      const profile = buildElevationProfile(pts, 400, 200);
      expect(profile).not.toBeNull();
      // x spans 0..400 across the 3 points
      expect(profile!.line.startsWith('M0,')).toBe(true);
      expect(profile!.line).toContain('400,');
      // lowest elevation maps to the bottom (larger y), highest to the top (smaller y)
      // first point is the lowest → its y must be greater than the last point's y
      const firstY = Number(profile!.line.split(' ')[0].split(',')[1]);
      const lastSeg = profile!.line.trim().split(/[ ]/).pop()!;
      const lastY = Number(lastSeg.split(',')[1]);
      expect(firstY).toBeGreaterThan(lastY);
      // area path closes back to the baseline
      expect(profile!.area.endsWith('Z')).toBe(true);
    });
  });
});
