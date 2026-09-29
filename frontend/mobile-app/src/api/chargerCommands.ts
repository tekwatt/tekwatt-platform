import { api } from './client';
import type { Charger, ChargingSession, Connector, UserProfile } from '../types';

const pause = (milliseconds: number) => new Promise<void>(resolve => setTimeout(resolve, milliseconds));

async function waitForAcceptance(messageId: string, token: string): Promise<void> {
  for (let attempt = 0; attempt < 12; attempt++) {
    const result = await api.ocppCommandResult(messageId, token);
    if (result.result === 'ACCEPTED') return;
    if (result.result === 'REJECTED' || result.result === 'FAILED') {
      throw new Error(result.message || 'The charger rejected the request.');
    }
    await pause(1500);
  }
  throw new Error('The charger has not confirmed the command. Check its status before trying again.');
}

export async function startChargingOnCharger(
  tenantId: string, profile: UserProfile, charger: Charger, connector: Connector, token: string,
): Promise<ChargingSession> {
  if (!profile.assignedChargerIds?.includes(charger.id)) throw new Error('This charger is not assigned to your account.');
  const existing = (await api.sessions(tenantId, token)).find(item => item.userId === profile.id
    && item.connectorId === connector.id && item.status === 'ACTIVE');
  if (existing) throw new Error('This connector already has an active session on your account. Open My Sessions before starting again.');
  const { messageId } = await api.remoteStartForMe(charger.id, connector.id, token);
  try { await waitForAcceptance(messageId, token); }
  catch (reason) {
    const started = (await api.sessions(tenantId, token)).find(item => item.userId === profile.id
      && item.connectorId === connector.id && item.status === 'ACTIVE');
    if (started) return started;
    throw reason;
  }
  for (let attempt = 0; attempt < 20; attempt++) {
    const sessions = await api.sessions(tenantId, token);
    const started = sessions.find(item => item.userId === profile.id && item.chargerId === charger.id
      && item.connectorId === connector.id && item.status === 'ACTIVE');
    if (started) return started;
    await pause(1500);
  }
  throw new Error('The charger accepted the start request but has not reported a charging session yet. Check My Sessions before sending another request.');
}

export async function stopChargingOnCharger(session: ChargingSession, charger: Charger, token: string): Promise<ChargingSession> {
  const { messageId } = await api.remoteStopForMe(session.id, token);
  try { await waitForAcceptance(messageId, token); }
  catch (reason) {
    const updated = (await api.sessions(charger.tenantId, token)).find(item => item.id === session.id);
    if (updated && updated.status !== 'ACTIVE') return updated;
    throw reason;
  }
  for (let attempt = 0; attempt < 20; attempt++) {
    const sessions = await api.sessions(charger.tenantId, token);
    const updated = sessions.find(item => item.id === session.id);
    if (updated && updated.status !== 'ACTIVE') return updated;
    await pause(1500);
  }
  throw new Error('The charger accepted the stop request but has not reported its final meter reading. Do not start another car or pay for this session yet.');
}
