export function ConfigMissing() {
  return (
    <div className="config-state">
      <p>
        <strong>No household is configured for this app instance.</strong>
      </p>
      <p>
        Set the <code>HOUSEHOLD_ID</code> environment variable to an existing household&apos;s ID and restart the
        app.
      </p>
    </div>
  );
}

export function ConfigInvalid({ rawValue }: { rawValue: string }) {
  return (
    <div className="config-state">
      <p>
        <strong>The configured household ID is malformed.</strong>
      </p>
      <p>
        The <code>HOUSEHOLD_ID</code> environment variable is set to <code>{rawValue}</code>, which is not a valid
        household ID. Correct the <code>HOUSEHOLD_ID</code> environment variable and restart the app.
      </p>
    </div>
  );
}

export function ConfigNotFound({ householdId }: { householdId: string }) {
  return (
    <div className="config-state">
      <p>
        <strong>No household was found for the configured ID.</strong>
      </p>
      <p>
        The app is configured to use household <code>{householdId}</code>, but no household with that ID exists.
        Check the <code>HOUSEHOLD_ID</code> environment variable and restart the app.
      </p>
    </div>
  );
}
