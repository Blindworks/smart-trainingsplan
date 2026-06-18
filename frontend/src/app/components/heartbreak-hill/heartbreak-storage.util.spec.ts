import { loadStoredEffort, saveStoredEffort, clearStoredEffort } from './heartbreak-storage.util';
import { ActivityType, EffortResult } from '../../models/heartbreak-hill.model';

const STORAGE_KEY = 'pacr.heartbreak-hill.efforts.v1';

function makeResult(overrides: Partial<EffortResult> = {}): EffortResult {
  return {
    effortId: 42,
    editToken: 'tok-abc',
    rank: 7,
    totalCount: 120,
    elapsedSeconds: 933,
    elapsedFormatted: '15:33',
    gapToLeaderSeconds: 46,
    percentileBeaten: 88,
    avgSpeedKmh: 23.4,
    avgPaceSecondsPerKm: null,
    status: 'MATCHED',
    ...overrides,
  };
}

describe('heartbreak-storage.util', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('round-trips a saved effort for a type', () => {
    saveStoredEffort('RIDE', makeResult(), 'Ben');
    const loaded = loadStoredEffort('RIDE');
    expect(loaded).not.toBeNull();
    expect(loaded!.result.effortId).toBe(42);
    expect(loaded!.result.rank).toBe(7);
    expect(loaded!.displayName).toBe('Ben');
    expect(typeof loaded!.savedAt).toBe('string');
  });

  it('returns null for a type that was never saved', () => {
    saveStoredEffort('RIDE', makeResult(), 'Ben');
    expect(loadStoredEffort('RUN')).toBeNull();
  });

  it('keeps RIDE and RUN independent', () => {
    saveStoredEffort('RIDE', makeResult({ effortId: 1, rank: 3 }), 'Rider');
    saveStoredEffort('RUN', makeResult({ effortId: 2, rank: 9 }), 'Runner');
    expect(loadStoredEffort('RIDE')!.result.effortId).toBe(1);
    expect(loadStoredEffort('RUN')!.result.effortId).toBe(2);
    // re-saving RIDE must not wipe RUN
    saveStoredEffort('RIDE', makeResult({ effortId: 11, rank: 1 }), 'Rider2');
    expect(loadStoredEffort('RIDE')!.result.effortId).toBe(11);
    expect(loadStoredEffort('RUN')!.result.effortId).toBe(2);
  });

  it('clears only the given type', () => {
    saveStoredEffort('RIDE', makeResult(), 'Rider');
    saveStoredEffort('RUN', makeResult(), 'Runner');
    clearStoredEffort('RIDE');
    expect(loadStoredEffort('RIDE')).toBeNull();
    expect(loadStoredEffort('RUN')).not.toBeNull();
  });

  it('returns null for corrupt JSON', () => {
    localStorage.setItem(STORAGE_KEY, 'not-json{');
    expect(loadStoredEffort('RIDE')).toBeNull();
  });

  it('returns null when the snapshot lacks required fields', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ RIDE: { displayName: 'x', savedAt: 'y' } }));
    expect(loadStoredEffort('RIDE')).toBeNull();
  });

  it('does not throw when localStorage.setItem fails', () => {
    const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceeded');
    });
    expect(() => saveStoredEffort('RIDE', makeResult(), 'Ben')).not.toThrow();
    spy.mockRestore();
  });
});
