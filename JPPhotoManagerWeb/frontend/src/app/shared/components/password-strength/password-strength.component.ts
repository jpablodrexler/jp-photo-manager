import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type PasswordStrengthLevel = 'weak' | 'medium' | 'strong';

interface PasswordRule {
  label: string;
  passes: boolean;
}

/**
 * Live password strength meter: a colour bar plus a checklist of the four rules enforced by
 * the backend's PasswordValidationService (Passay). Evaluated entirely client-side — no API
 * call is made on each keystroke.
 */
@Component({
  selector: 'app-password-strength',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './password-strength.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './password-strength.component.scss',
})
export class PasswordStrengthComponent {
  @Input() password = '';

  private static readonly MIN_LENGTH = 12;
  private static readonly UPPERCASE_PATTERN = /[A-Z]/;
  private static readonly DIGIT_PATTERN = /[0-9]/;
  private static readonly SPECIAL_PATTERN = /[^A-Za-z0-9]/;

  get meetsLength(): boolean {
    return this.password.length >= PasswordStrengthComponent.MIN_LENGTH;
  }

  get hasUppercase(): boolean {
    return PasswordStrengthComponent.UPPERCASE_PATTERN.test(this.password);
  }

  get hasDigit(): boolean {
    return PasswordStrengthComponent.DIGIT_PATTERN.test(this.password);
  }

  get hasSpecial(): boolean {
    return PasswordStrengthComponent.SPECIAL_PATTERN.test(this.password);
  }

  get rules(): PasswordRule[] {
    return [
      { label: 'At least 12 characters', passes: this.meetsLength },
      { label: 'At least one uppercase letter', passes: this.hasUppercase },
      { label: 'At least one digit', passes: this.hasDigit },
      { label: 'At least one special character', passes: this.hasSpecial },
    ];
  }

  get passingRuleCount(): number {
    return this.rules.filter(rule => rule.passes).length;
  }

  get strengthLevel(): PasswordStrengthLevel {
    if (this.passingRuleCount === 4) return 'strong';
    if (this.passingRuleCount >= 2) return 'medium';
    return 'weak';
  }
}
