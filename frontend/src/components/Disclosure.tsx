import { useEffect, useId, useRef, type ReactNode } from 'react';

/**
 * A labeled toggle button plus a collapsible panel. Controlled by the caller
 * (`open`/`onToggle`) rather than owning its own state, so a form inside can
 * close itself after a successful submission. Unlike CurrencyCard's
 * always-mounted/`hidden` expand-collapse (read-only records worth keeping
 * in the DOM), this unmounts its children while closed: a form has no state
 * worth preserving before it's opened, and an unopened form's fields would
 * otherwise collide (same labels — "Name", "Currency", ...) with a sibling
 * form's fields already in the DOM, for both assistive tech and tests. Moves
 * focus to the panel's first focusable element whenever it opens, so opening
 * an add/correct form doesn't strand keyboard/screen-reader users on a now-
 * hidden button.
 */
export function Disclosure({
  open,
  onToggle,
  closedLabel,
  openLabel,
  children,
}: {
  open: boolean;
  onToggle: () => void;
  closedLabel: string;
  openLabel?: string;
  children: ReactNode;
}) {
  const panelId = useId();
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (open) {
      panelRef.current?.querySelector<HTMLElement>('input, select, textarea, button')?.focus();
    }
  }, [open]);

  return (
    <>
      <button type="button" className="toggle-btn" aria-expanded={open} aria-controls={panelId} onClick={onToggle}>
        {open ? (openLabel ?? 'Cancel') : closedLabel}
      </button>
      {open && (
        <div id={panelId} ref={panelRef}>
          {children}
        </div>
      )}
    </>
  );
}
