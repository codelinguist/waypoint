import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { useCalculatorSubmit } from './useCalculatorSubmit';
import { CalculatorValidationError } from '../api/client';

describe('useCalculatorSubmit', () => {
  it('starts idle and transitions through submitting to success', async () => {
    const submit = vi.fn().mockResolvedValue({ value: 42 });
    const { result } = renderHook(() => useCalculatorSubmit(submit));

    expect(result.current.state.status).toBe('idle');

    await act(async () => {
      await result.current.run({ input: 1 });
    });

    expect(result.current.state).toEqual({ status: 'success', data: { value: 42 } });
  });

  it('surfaces a validation error message distinctly, without failing silently', async () => {
    const submit = vi.fn().mockRejectedValue(new CalculatorValidationError('currency must not be blank', []));
    const { result } = renderHook(() => useCalculatorSubmit(submit));

    await act(async () => {
      await result.current.run({ input: 1 });
    });

    expect(result.current.state).toEqual({ status: 'error', message: 'currency must not be blank' });
  });

  it('appends field-level validation details to the generic top-level message', async () => {
    // The API's top-level message is a generic "Request validation failed"
    // (backend ApiExceptionHandler#handleValidation); the actionable reason
    // lives in `details` only, so it must be surfaced too.
    const submit = vi
      .fn()
      .mockRejectedValue(
        new CalculatorValidationError('Request validation failed', ['currency: currency must be a 3-letter currency code'])
      );
    const { result } = renderHook(() => useCalculatorSubmit(submit));

    await act(async () => {
      await result.current.run({ input: 1 });
    });

    expect(result.current.state).toEqual({
      status: 'error',
      message: 'Request validation failed: currency: currency must be a 3-letter currency code',
    });
  });

  it('never lets an older, slower submission overwrite a newer one already applied', async () => {
    let resolveOlder: (value: { value: string }) => void = () => {};
    const olderRequest = new Promise<{ value: string }>((resolve) => {
      resolveOlder = resolve;
    });

    const submit = vi
      .fn()
      .mockImplementationOnce(() => olderRequest)
      .mockImplementationOnce(() => Promise.resolve({ value: 'newer' }));

    const { result } = renderHook(() => useCalculatorSubmit(submit));

    act(() => {
      void result.current.run({ input: 1 });
    });
    expect(result.current.state.status).toBe('submitting');

    await act(async () => {
      await result.current.run({ input: 2 });
    });
    expect(result.current.state).toEqual({ status: 'success', data: { value: 'newer' } });

    await act(async () => {
      resolveOlder({ value: 'older' });
    });

    expect(result.current.state).toEqual({ status: 'success', data: { value: 'newer' } });
  });

  it('a resubmission clears a previous error and starts submitting again', async () => {
    const submit = vi
      .fn()
      .mockRejectedValueOnce(new CalculatorValidationError('bad input', []))
      .mockResolvedValueOnce({ value: 'ok' });
    const { result } = renderHook(() => useCalculatorSubmit(submit));

    await act(async () => {
      await result.current.run({ input: 1 });
    });
    expect(result.current.state.status).toBe('error');

    act(() => {
      void result.current.run({ input: 2 });
    });
    expect(result.current.state.status).toBe('submitting');

    await waitFor(() => expect(result.current.state).toEqual({ status: 'success', data: { value: 'ok' } }));
  });
});
