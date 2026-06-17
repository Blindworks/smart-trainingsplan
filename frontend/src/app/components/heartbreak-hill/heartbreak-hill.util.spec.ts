import { buildElevationProfile, buildElevationPoints, formatGap, formatGrade } from './heartbreak-hill.util';

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

  describe('buildElevationPoints', () => {
    it('returns an empty array for fewer than 2 points', () => {
      expect(buildElevationPoints([], 400, 200)).toEqual([]);
      expect(buildElevationPoints([[50, 8, 100]], 400, 200)).toEqual([]);
    });

    it('spreads x evenly and inverts elevation (highest → smallest y)', () => {
      const pts: [number, number, number][] = [[50, 8, 100], [50.001, 8, 110], [50.002, 8, 120]];
      const out = buildElevationPoints(pts, 400, 200);
      expect(out.length).toBe(3);
      expect(out[0].x).toBe(0);
      expect(out[2].x).toBe(400);
      // lowest elevation (first) sits lower on screen → larger y than the highest (last)
      expect(out[0].y).toBeGreaterThan(out[2].y);
    });
  });
});
