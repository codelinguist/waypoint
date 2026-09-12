// Runtime household configuration. The household id is normally injected
// outside the ordinary product flow: `docker/docker-entrypoint.sh`
// substitutes the HOUSEHOLD_ID environment variable into public/config.js at
// container startup (before this bundle loads), so the same built image can
// be deployed for a different household without a rebuild. See README.md
// "Local startup" for operator instructions.
//
// A second, browser-local source (WAP-26) lets the onboarding wizard bridge
// "just created a household in this browser" to "the app already works,
// here, right now" without a container restart: `setHouseholdIdOverride`
// writes the newly created household's id to `localStorage`, and this
// module checks it before `window.__WAYPOINT_CONFIG__`. `HOUSEHOLD_ID`
// remains the durable, production way to pin a deployment to a household —
// the override only matters for the browser that ran the wizard.

declare global {
  interface Window {
    __WAYPOINT_CONFIG__?: { householdId?: string };
  }
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const OVERRIDE_STORAGE_KEY = 'waypoint.householdIdOverride';

export type HouseholdConfig =
  | { status: 'configured'; householdId: string }
  | { status: 'missing' }
  | { status: 'invalid'; rawValue: string };

function readOverride(): string | undefined {
  try {
    return window.localStorage.getItem(OVERRIDE_STORAGE_KEY)?.trim() || undefined;
  } catch {
    return undefined; // localStorage unavailable (private browsing, disabled) — fall through to the ordinary config source
  }
}

/** Records a newly created household as this browser's override, checked before `window.__WAYPOINT_CONFIG__`. */
export function setHouseholdIdOverride(householdId: string): void {
  try {
    window.localStorage.setItem(OVERRIDE_STORAGE_KEY, householdId);
  } catch {
    // localStorage unavailable — the override simply won't persist; the wizard's in-memory state still carries the session through
  }
}

export function readHouseholdConfig(): HouseholdConfig {
  const raw = readOverride() ?? window.__WAYPOINT_CONFIG__?.householdId?.trim();
  if (!raw) {
    return { status: 'missing' };
  }
  if (UUID_PATTERN.test(raw)) {
    return { status: 'configured', householdId: raw };
  }
  return { status: 'invalid', rawValue: raw };
}
