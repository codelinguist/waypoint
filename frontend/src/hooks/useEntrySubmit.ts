import { useCallback, useRef, useState } from 'react';
import { EntryConflictError, EntryValidationError } from '../api/client';

export type EntrySubmitState<TResponse> =
  | { status: 'idle' }
  | { status: 'submitting' }
  | { status: 'success'; data: TResponse }
  | { status: 'error'; kind: 'validation' | 'conflict' | 'other'; message: string; details: string[] };

/**
 * Drives a single create/correction form submission. Distinguishes a
 * validation failure (400, fixable by editing the form) from a correction's
 * stale-revision conflict (409, fixable only by reloading the current value)
 * so a form can render each differently. Race safety mirrors
 * useCalculatorSubmit: only the response belonging to the most recently
 * started submission is ever applied to state.
 *
 * `run` resolves with the response on success or `undefined` on failure (or
 * if superseded by a newer submission), so a caller can chain
 * post-success actions (close the form, trigger a list refresh) inline
 * instead of watching `state` in an effect.
 */
export function useEntrySubmit<TRequest, TResponse>(submit: (request: TRequest) => Promise<TResponse>) {
  const [state, setState] = useState<EntrySubmitState<TResponse>>({ status: 'idle' });
  const requestSeqRef = useRef(0);

  const run = useCallback(
    async (request: TRequest): Promise<TResponse | undefined> => {
      const seq = ++requestSeqRef.current;
      setState({ status: 'submitting' });

      try {
        const data = await submit(request);
        if (requestSeqRef.current !== seq) return undefined; // superseded by a newer submission
        setState({ status: 'success', data });
        return data;
      } catch (error) {
        if (requestSeqRef.current !== seq) return undefined; // superseded by a newer submission

        if (error instanceof EntryValidationError) {
          const details = error.details.filter((detail): detail is string => typeof detail === 'string');
          setState({ status: 'error', kind: 'validation', message: error.message, details });
          return undefined;
        }
        if (error instanceof EntryConflictError) {
          setState({ status: 'error', kind: 'conflict', message: error.message, details: [] });
          return undefined;
        }
        const message = error instanceof Error ? error.message : 'Could not reach the server.';
        setState({ status: 'error', kind: 'other', message, details: [] });
        return undefined;
      }
    },
    [submit]
  );

  const reset = useCallback(() => setState({ status: 'idle' }), []);

  return { state, run, reset };
}

/** Joins a validation error's message with its per-field details, matching useCalculatorSubmit's convention. */
export function formatEntryErrorMessage(state: Extract<EntrySubmitState<unknown>, { status: 'error' }>): string {
  if (state.kind === 'validation' && state.details.length > 0) {
    return `${state.message}: ${state.details.join('; ')}`;
  }
  return state.message;
}
