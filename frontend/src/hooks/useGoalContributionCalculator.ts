import { useCallback, useRef, useState } from 'react';
import { calculateGoalContribution, GoalContributionRequestError } from '../api/client';
import type { GoalContributionRequestBody, GoalContributionResult } from '../api/types';

export type CalculatorState =
  | { status: 'idle' }
  | { status: 'submitting' }
  | { status: 'result'; data: GoalContributionResult }
  | { status: 'error'; message: string; details: string[] };

/**
 * Runs the stateless goal-contribution calculation. Race safety mirrors
 * useFinancialPosition/useGoals: only the most recently started submission
 * is ever applied, so a slow earlier submit can never overwrite a later one.
 */
export function useGoalContributionCalculator() {
  const [state, setState] = useState<CalculatorState>({ status: 'idle' });
  const requestSeqRef = useRef(0);
  const abortRef = useRef<AbortController | null>(null);

  const submit = useCallback(async (request: GoalContributionRequestBody) => {
    const seq = ++requestSeqRef.current;
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    setState({ status: 'submitting' });

    try {
      const data = await calculateGoalContribution(request, controller.signal);
      if (requestSeqRef.current !== seq) return;
      setState({ status: 'result', data });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      if (requestSeqRef.current !== seq) return;

      if (error instanceof GoalContributionRequestError) {
        setState({ status: 'error', message: error.message, details: error.details });
      } else {
        const message = error instanceof Error ? error.message : 'Could not reach the server.';
        setState({ status: 'error', message, details: [] });
      }
    }
  }, []);

  return { state, submit };
}
