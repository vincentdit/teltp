import { AbstractControl, ValidationErrors } from '@angular/forms';

export const PASSWORD_HINT =
  'At least 12 characters, with an uppercase letter, a lowercase letter, a number, and a symbol.';

export function passwordScore(value: string): number {
  if (!value) return 0;
  const met = [
    value.length >= 12,
    /[A-Z]/.test(value),
    /[a-z]/.test(value),
    /\d/.test(value),
    /[^A-Za-z0-9]/.test(value),
  ].filter(Boolean).length;
  if (met <= 1) return 1;
  if (met === 2) return 2;
  if (met <= 4) return 3;
  return 4;
}

export function passwordLabel(score: number): string {
  return ['Too weak', 'Weak', 'Fair', 'Good', 'Strong'][score] ?? 'Too weak';
}

export function strongPassword(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '');
  if (!value) return null;
  const ok = value.length >= 12
    && /[A-Z]/.test(value)
    && /[a-z]/.test(value)
    && /\d/.test(value)
    && /[^A-Za-z0-9]/.test(value);
  return ok ? null : { strong: true };
}

export function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirm')?.value;
  if (!confirm) return null;
  return password === confirm ? null : { mismatch: true };
}
