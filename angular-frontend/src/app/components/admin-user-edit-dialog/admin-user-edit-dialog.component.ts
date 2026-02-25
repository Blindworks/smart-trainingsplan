import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ApiService } from '../../services/api.service';
import { User } from '../../models/competition.model';

export interface AdminUserEditDialogData {
  user: User;
}

@Component({
  selector: 'app-admin-user-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './admin-user-edit-dialog.component.html',
  styleUrl: './admin-user-edit-dialog.component.scss'
})
export class AdminUserEditDialogComponent {
  saving = false;
  errorMessage = '';
  form!: ReturnType<FormBuilder['group']>;

  statusOptions: Array<{ value: NonNullable<User['status']>; label: string }> = [
    { value: 'EMAIL_VERIFICATION_PENDING', label: 'E-Mail-Bestaetigung ausstehend' },
    { value: 'ADMIN_APPROVAL_PENDING', label: 'Admin-Freigabe ausstehend' },
    { value: 'ACTIVE', label: 'Aktiv' },
    { value: 'INACTIVE', label: 'Inaktiv' },
    { value: 'BLOCKED', label: 'Blockiert' }
  ];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AdminUserEditDialogComponent>,
    private apiService: ApiService,
    @Inject(MAT_DIALOG_DATA) public data: AdminUserEditDialogData
  ) {
    this.form = this.createForm();
    this.form.patchValue({
      username: data.user.username ?? '',
      email: data.user.email ?? '',
      firstName: data.user.firstName ?? '',
      lastName: data.user.lastName ?? '',
      status: data.user.status ?? 'ACTIVE'
    });
  }

  private createForm() {
    return this.fb.group({
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      firstName: [''],
      lastName: [''],
      status: ['ACTIVE' as NonNullable<User['status']>, Validators.required]
    });
  }

  onCancel(): void {
    if (!this.saving) {
      this.dialogRef.close();
    }
  }

  onSave(): void {
    if (this.form.invalid || this.saving || !this.data.user.id) {
      return;
    }

    this.errorMessage = '';
    this.saving = true;

    const value = this.form.getRawValue();
    const trim = (input?: string | null): string | undefined => {
      const trimmed = input?.trim();
      return trimmed ? trimmed : undefined;
    };

    this.apiService.updateUser(this.data.user.id, {
      username: value.username!.trim(),
      email: value.email!.trim(),
      firstName: trim(value.firstName),
      lastName: trim(value.lastName),
      status: value.status as User['status']
    }).subscribe({
      next: (updatedUser) => this.dialogRef.close(updatedUser),
      error: () => {
        this.errorMessage = 'User konnte nicht gespeichert werden.';
        this.saving = false;
      }
    });
  }
}
