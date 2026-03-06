import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-pace-input',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './pace-input.component.html',
  styleUrl: './pace-input.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PaceInputComponent),
      multi: true
    }
  ]
})
export class PaceInputComponent implements ControlValueAccessor {
  value = '';
  disabled = false;
  @Input()
  placeholder = 'MM:ss';

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: string | null | undefined): void {
    this.value = this.normalizeIncomingValue(value ?? '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onInput(event: Event): void {
    const inputValue = (event.target as HTMLInputElement).value;
    this.value = this.formatWithMask(inputValue);
    this.onChange(this.value);
  }

  onBlur(): void {
    this.value = this.padToDoubleDigits(this.value);
    this.onChange(this.value);
    this.onTouched();
  }

  private normalizeIncomingValue(input: string): string {
    const trimmed = input.trim();
    if (!trimmed) return '';

    const matchedPace = /^(\d{1,2}):(\d{1,2})$/.exec(trimmed);
    if (matchedPace) {
      return `${matchedPace[1].padStart(2, '0')}:${matchedPace[2].padStart(2, '0')}`;
    }

    return this.formatWithMask(trimmed);
  }

  private formatWithMask(input: string): string {
    const digits = input.replace(/\D/g, '').slice(0, 4);
    if (!digits) return '';
    if (digits.length <= 2) return digits;

    const minutes = digits.slice(0, -2);
    const seconds = digits.slice(-2);
    return `${minutes}:${seconds}`;
  }

  private padToDoubleDigits(input: string): string {
    const matchedPace = /^(\d{1,2}):(\d{1,2})$/.exec(input);
    if (!matchedPace) return input;

    return `${matchedPace[1].padStart(2, '0')}:${matchedPace[2].padStart(2, '0')}`;
  }
}
