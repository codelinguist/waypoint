// Runtime household configuration. The household id is injected outside the
// ordinary product flow: `docker/docker-entrypoint.sh` substitutes the
// HOUSEHOLD_ID environment variable into public/config.js at container
// startup (before this bundle loads), so the same built image can be
// deployed for a different household without a rebuild. See README.md
// "Local startup" for operator instructions.

declare global {
  interface Window {
    __WAYPOINT_CONFIG__?: { householdId?: string };
  }
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export type HouseholdConfig =
  | { status: 'configured'; householdId: string }
  | { status: 'missing' }
  | { status: 'invalid'; rawValue: string };

export function readHouseholdConfig(): HouseholdConfig {
  const raw = window.__WAYPOINT_CONFIG__?.householdId?.trim();
  if (!raw) {
    return { status: 'missing' };
  }
  if (UUID_PATTERN.test(raw)) {
    return { status: 'configured', householdId: raw };
  }
  return { status: 'invalid', rawValue: raw };
}
