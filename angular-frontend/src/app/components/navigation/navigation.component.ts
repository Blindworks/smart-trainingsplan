import { Component, HostListener } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../services/auth.service';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../services/i18n.service';
import { Language } from '../../i18n/translations';

@Component({
  selector: 'app-navigation',
  imports: [
    RouterModule,
    MatIconModule,
    MatButtonModule,
    TranslatePipe
  ],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss'
})
export class NavigationComponent {
  mobileMenuOpen = false;
  readonly languages: readonly Language[];

  constructor(
    private authService: AuthService,
    private i18nService: I18nService
  ) {
    this.languages = this.i18nService.getSupportedLanguages();
  }

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth > 1024 && this.mobileMenuOpen) {
      this.mobileMenuOpen = false;
    }
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  getUsername(): string {
    return this.authService.getCurrentUsername() ?? this.i18nService.t('common.profile');
  }

  getInitial(): string {
    const name = this.authService.getCurrentUsername();
    return name ? name.charAt(0).toUpperCase() : 'P';
  }

  logout(): void {
    this.closeMobileMenu();
    this.authService.logout();
  }

  getLanguage(): Language {
    return this.i18nService.getLanguage();
  }

  onLanguageChange(language: string): void {
    this.i18nService.setLanguage(language);
  }

  toggleLanguage(): void {
    const current = this.i18nService.getLanguage();
    const next = this.languages.find(l => l !== current) ?? this.languages[0];
    this.i18nService.setLanguage(next);
  }
}
