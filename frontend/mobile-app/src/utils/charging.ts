import type { ChargingSession } from '../types';

const ACTIVE_SESSION_STATUSES = new Set(['ACTIVE', 'STARTED', 'CHARGING']);

export function isActiveChargingSession(session: ChargingSession) {
  return ACTIVE_SESSION_STATUSES.has(session.status.toUpperCase());
}

export function chargingDurationMs(session: ChargingSession, now = Date.now()) {
  if (!session.startedAt) return 0;
  const startedAt = Date.parse(session.startedAt);
  if (!Number.isFinite(startedAt)) return 0;
  const stoppedAt = session.stoppedAt ? Date.parse(session.stoppedAt) : now;
  return Math.max(0, (Number.isFinite(stoppedAt) ? stoppedAt : now) - startedAt);
}

export function formatChargingDuration(durationMs: number) {
  const totalSeconds = Math.floor(Math.max(0, durationMs) / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (hours > 0) return `${hours}h ${String(minutes).padStart(2, '0')}m`;
  return `${minutes}m ${String(seconds).padStart(2, '0')}s`;
}
