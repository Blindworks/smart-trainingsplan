import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navigation',
  imports: [
    RouterModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss'
})
export class NavigationComponent {
  constructor(private authService: AuthService) {}

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  getUsername(): string {
    return this.authService.getCurrentUsername() ?? 'Profil';
  }

  getInitial(): string {
    const name = this.authService.getCurrentUsername();
    return name ? name.charAt(0).toUpperCase() : 'P';
  }

  logout(): void {
    this.authService.logout();
  }
}
