import { ActivityType, EffortResult } from '../../models/heartbreak-hill.model';

const STORAGE_KEY = 'pacr.heartbreak-hill.efforts.v1';

/** A remembered upload snapshot for one activity type. */
export interface StoredEffort {
  result: EffortResult;
  displayName: string;
  savedAt: string; // ISO timestamp
}

type StoredEfforts = Partial<Record<ActivityType, StoredEffort>>;

/** Reads and parses the whole map; returns {} on any failure (missing/corrupt/unavailable). */
function readAll(): StoredEfforts {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return {};
    }
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' ? (parsed as StoredEfforts) : {};
  } catch {
    return {};
  }
}

/** Persists the map; swallows quota/availability errors so the feature degrades silently. */
function writeAll(map: StoredEfforts): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(map));
  } catch {
    /* localStorage unavailable or full — degrade silently */
  }
}

/** Guards against an old/partial schema after a deploy. */
function isValidEntry(entry: StoredEffort | undefined): entry is StoredEffort {
  const r = entry?.result;
  return !!r && typeof r === 'object' && typeof r.effortId === 'number' && typeof r.rank === 'number';
}

export function loadStoredEffort(type: ActivityType): StoredEffort | null {
  const entry = readAll()[type];
  return isValidEntry(entry) ? entry : null;
}

export function saveStoredEffort(type: ActivityType, result: EffortResult, displayName: string): void {
  const map = readAll();
  map[type] = { result, displayName, savedAt: new Date().toISOString() };
  writeAll(map);
}

export function clearStoredEffort(type: ActivityType): void {
  const map = readAll();
  delete map[type];
  if (Object.keys(map).length === 0) {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
  } else {
    writeAll(map);
  }
}
