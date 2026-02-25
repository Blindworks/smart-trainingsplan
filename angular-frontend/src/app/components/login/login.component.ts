import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
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
    MatProgressSpinnerModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  loginForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  infoMessage = '';
  hidePassword = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });

    const registrationStatus = this.route.snapshot.queryParamMap.get('status');
    if (registrationStatus) {
      this.infoMessage = this.messageForStatus(registrationStatus);
    }
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/overview']);
      },
      error: (err) => {
        this.isLoading = false;

        if (err.status === 401) {
          this.errorMessage = 'Ungültige Anmeldedaten';
          return;
        }

        if (err.status === 403) {
          const status = err?.error?.status as string | undefined;
          this.errorMessage = this.messageForStatus(status ?? '');
          return;
        }

        this.errorMessage = 'Anmeldung fehlgeschlagen';
      }
    });
  }

  private messageForStatus(status: string): string {
    const statusMessages: Record<string, string> = {
      EMAIL_VERIFICATION_PENDING: 'Registrierung erfolgreich. Bitte bestätige zuerst deine E-Mail-Adresse.',
      ADMIN_APPROVAL_PENDING: 'Dein Konto wartet auf Freigabe durch einen Admin.',
      BLOCKED: 'Dein Konto ist blockiert. Bitte kontaktiere den Support.',
      INACTIVE: 'Dein Konto ist inaktiv. Bitte kontaktiere den Support.',
      ACTIVE: 'Dein Konto ist aktiv.'
    };
    return statusMessages[status] ?? 'Anmeldung derzeit nicht möglich.';
  }
}
