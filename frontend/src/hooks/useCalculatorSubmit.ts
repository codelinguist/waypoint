import { useCallback, useRef, useState } from 'react';
import { CalculatorValidationError } from '../api/client';

export type CalculatorSubmitState<TResponse> =
  | { status: 'idle' }
  | { status: 'submitting' }
  | { status: 'success'; data: TResponse }
  | { status: 'error'; message: string };

/**
 * Drives a single stateless calculator form: submit a request, show the
 * result or surface a validation/request error. Race safety mirrors
 * useFinancialPosition: only the response belonging to the most recently
 * started submission is ever applied to state, so a slow superseded request
 * can never overwrite a newer submission's result.
 */
export function useCalculatorSubmit<TRequest, TResponse>(submit: (request: TRequest) => Promise<TResponse>) {
  const [state, setState] = useState<CalculatorSubmitState<TResponse>>({ status: 'idle' });
  const requestSeqRef = useRef(0);

  const run = useCallback(
    async (request: TRequest) => {
      const seq = ++requestSeqRef.current;
      setState({ status: 'submitting' });

      try {
        const data = await submit(request);
        if (requestSeqRef.current !== seq) return; // superseded by a newer submission
        setState({ status: 'success', data });
      } catch (error) {
        if (requestSeqRef.current !== seq) return; // superseded by a newer submission

        const message = formatErrorMessage(error);
        setState({ status: 'error', message });
      }
    },
    [submit]
  );

  return { state, run };
}

/**
 * The API's top-level validation message is a generic "Request validation
 * failed" (see backend ApiExceptionHandler#handleValidation), with the
 * useful per-field reason only in `details` — append it so the household
 * sees what to fix, not just that something failed.
 */
function formatErrorMessage(error: unknown): string {
  if (error instanceof CalculatorValidationError) {
    const details = error.details.filter((detail): detail is string => typeof detail === 'string');
    return details.length > 0 ? `${error.message}: ${details.join('; ')}` : error.message;
  }
  return error instanceof Error ? error.message : 'Could not reach the server.';
}
