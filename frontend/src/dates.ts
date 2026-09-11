// Date/time presentation helpers. `valuedAt`/`balanceAsOf` are LocalDate
// strings ("YYYY-MM-DD") with no time component and no timezone — they are
// parsed as calendar dates, never through `new Date(string)` directly
// (which treats a bare "YYYY-MM-DD" as UTC midnight and can render a day
// earlier in negative-UTC-offset timezones).

const MONTH_DAY_YEAR = new Intl.DateTimeFormat('en-US', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  timeZone: 'UTC',
});

const DATE_TIME_UTC = new Intl.DateTimeFormat('en-US', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
  timeZone: 'UTC',
});

/** Formats a LocalDate string ("2026-09-08") as "Sep 8, 2026". */
export function formatLocalDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return MONTH_DAY_YEAR.format(new Date(Date.UTC(year, month - 1, day)));
}

/** Formats an ISO instant as "Sep 10, 2026, 2:32 PM UTC". */
export function formatInstantUtc(isoInstant: string): string {
  return `${DATE_TIME_UTC.format(new Date(isoInstant))} UTC`;
}

/** Formats a local Date (e.g. a refresh-failure timestamp) as "2:41 PM UTC". */
export function formatTimeUtc(date: Date): string {
  const time = new Intl.DateTimeFormat('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    timeZone: 'UTC',
  }).format(date);
  return `${time} UTC`;
}
