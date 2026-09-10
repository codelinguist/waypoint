// Local-dev default: no household configured, so the app shows the
// missing-configuration state until you set one below.
//
// In the built Docker image this file is regenerated at container startup
// from the HOUSEHOLD_ID environment variable (see docker/docker-entrypoint.sh)
// — do not rely on committing a real household id here.
window.__WAYPOINT_CONFIG__ = {
  householdId: '',
};
