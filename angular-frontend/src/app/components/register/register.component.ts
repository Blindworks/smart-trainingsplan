import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../models/auth.model';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../services/i18n.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslatePipe
  ],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  registerForm: FormGroup;
  verificationForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  hidePassword = true;
  step: 'register' | 'verify' = 'register';
  pendingEmail = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private i18nService: I18nService
  ) {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
    this.verificationForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.register(this.registerForm.value).subscribe({
      next: (response: AuthResponse) => {
        this.isLoading = false;
        this.pendingEmail = response.email;
        this.step = 'verify';
        this.successMessage = this.i18nService.t('register.verificationSent');
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 409) {
          this.errorMessage = this.i18nService.t('register.duplicateUser');
        } else {
          this.errorMessage = this.i18nService.t('register.failed');
        }
      }
    });
  }

  onVerifySubmit(): void {
    if (this.verificationForm.invalid || !this.pendingEmail) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.verifyEmail({
      email: this.pendingEmail,
      code: this.verificationForm.value.code
    }).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/login'], { queryParams: { status: 'ACTIVE' } });
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err?.error?.message ?? this.i18nService.t('register.verificationFailed');
      }
    });
  }

  backToRegister(): void {
    this.step = 'register';
    this.errorMessage = '';
    this.successMessage = '';
    this.verificationForm.reset();
  }
}
