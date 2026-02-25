import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { User } from '../../models/competition.model';
import { AdminUserEditDialogComponent } from '../admin-user-edit-dialog/admin-user-edit-dialog.component';

@Component({
  selector: 'app-admin-user-management',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatDialogModule,
    MatSnackBarModule,
    RouterLink
  ],
  templateUrl: './admin-user-management.component.html',
  styleUrl: './admin-user-management.component.scss'
})
export class AdminUserManagementComponent implements OnInit {
  users: User[] = [];
  loading = true;

  constructor(
    private apiService: ApiService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.apiService.getUsers()
      .pipe(catchError(() => of([])))
      .subscribe(users => {
        this.users = users;
        this.loading = false;
      });
  }

  getStatusLabel(status?: User['status']): string {
    switch (status) {
      case 'EMAIL_VERIFICATION_PENDING':
        return 'E-Mail-Bestätigung ausstehend';
      case 'ADMIN_APPROVAL_PENDING':
        return 'Admin-Freigabe ausstehend';
      case 'BLOCKED':
        return 'Blockiert';
      case 'INACTIVE':
        return 'Inaktiv';
      case 'ACTIVE':
        return 'Aktiv';
      default:
        return 'Unbekannt';
    }
  }

  openEditDialog(user: User): void {
    if (!user.id) {
      this.snackBar.open('User ohne ID kann nicht bearbeitet werden.', 'Schliessen', { duration: 3000 });
      return;
    }

    const dialogRef = this.dialog.open(AdminUserEditDialogComponent, {
      width: '560px',
      maxWidth: '96vw',
      data: { user }
    });

    dialogRef.afterClosed().subscribe((updatedUser?: User) => {
      if (!updatedUser || !updatedUser.id) {
        return;
      }

      const index = this.users.findIndex(u => u.id === updatedUser.id);
      if (index !== -1) {
        this.users[index] = updatedUser;
      }
    });
  }
}
