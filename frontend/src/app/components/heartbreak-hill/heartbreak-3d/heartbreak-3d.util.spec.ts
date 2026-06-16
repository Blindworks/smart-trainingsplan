import { buildTerrainGrid, isWebglAvailable } from './heartbreak-3d.util';

describe('heartbreak-3d.util', () => {
  describe('buildTerrainGrid', () => {
    it('produces a heightfield matching PlaneGeometry vertex count', () => {
      const grid = buildTerrainGrid(null, 8, 4);   // synthetic, 8x4 segments
      expect(grid.segX).toBe(8);
      expect(grid.segZ).toBe(4);
      expect(grid.heights.length).toBe((8 + 1) * (4 + 1)); // 45
      expect(grid.routeXYZ.length).toBe(8 + 1);             // one route point per X column
      expect(grid.width).toBeGreaterThan(0);
      expect(grid.depth).toBeGreaterThan(0);
    });

    it('maps a rising elevation profile to a rising route (last point higher than first)', () => {
      const poly: [number, number, number][] = [
        [50.0, 8.0, 100], [50.001, 8.0, 130], [50.002, 8.0, 175]
      ];
      const grid = buildTerrainGrid(poly, 8, 4);
      const firstY = grid.routeXYZ[0][1];
      const lastY = grid.routeXYZ[grid.routeXYZ.length - 1][1];
      expect(lastY).toBeGreaterThan(firstY);
    });

    it('makes the center row the ridge (higher than an edge row at the same column)', () => {
      const grid = buildTerrainGrid(null, 8, 4);
      const segX = 8, segZ = 4;
      const col = segX;                       // the highest column of the synthetic ridge (peak at the end)
      const centerRow = segZ / 2;             // 2
      const edgeRow = 0;
      const centerH = grid.heights[centerRow * (segX + 1) + col];
      const edgeH = grid.heights[edgeRow * (segX + 1) + col];
      expect(centerH).toBeGreaterThan(edgeH);
    });

    it('returns a synthetic grid (no throw) for a too-short polyline', () => {
      expect(() => buildTerrainGrid([[50, 8, 100]], 8, 4)).not.toThrow();
      const grid = buildTerrainGrid([], 8, 4);
      expect(grid.heights.length).toBe(45);
    });
  });

  describe('isWebglAvailable', () => {
    it('returns a boolean without throwing (false in jsdom)', () => {
      expect(typeof isWebglAvailable()).toBe('boolean');
    });
  });
});
