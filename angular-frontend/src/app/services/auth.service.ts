import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthResponse, AuthState, EmailVerificationRequest, LoginRequest, MessageResponse, RegisterRequest } from '../models/auth.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'auth_user';
  private baseUrl = environment.apiUrl;

  private authState = new BehaviorSubject<AuthState>(this.loadInitialState());
  public authState$ = this.authState.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  private loadInitialState(): AuthState {
    const token = localStorage.getItem(this.TOKEN_KEY);
    const userStr = localStorage.getItem(this.USER_KEY);
    if (token && userStr) {
      try {
        const user = JSON.parse(userStr) as AuthResponse;
        return {
          isLoggedIn: !!token,
          token,
          userId: user.userId,
          username: user.username,
          email: user.email,
          role: user.role ?? null,
          status: user.status ?? null
        };
      } catch {
        return { isLoggedIn: false, token: null, userId: null, username: null, email: null, role: null, status: null };
      }
    }
    return { isLoggedIn: false, token: null, userId: null, username: null, email: null, role: null, status: null };
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  verifyEmail(request: EmailVerificationRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/auth/verify-email`, request);
  }

  private handleAuthResponse(response: AuthResponse): void {
    if (response.token) {
      localStorage.setItem(this.TOKEN_KEY, response.token);
      localStorage.setItem(this.USER_KEY, JSON.stringify(response));
      this.authState.next({
        isLoggedIn: true,
        token: response.token,
        userId: response.userId,
        username: response.username,
        email: response.email,
        role: response.role,
        status: response.status
      });
      return;
    }

    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.authState.next({
      isLoggedIn: false,
      token: null,
      userId: null,
      username: null,
      email: null,
      role: null,
      status: response.status ?? null
    });
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.authState.next({ isLoggedIn: false, token: null, userId: null, username: null, email: null, role: null, status: null });
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return this.authState.getValue().isLoggedIn;
  }

  getToken(): string | null {
    return this.authState.getValue().token;
  }

  getCurrentUserId(): number | null {
    return this.authState.getValue().userId;
  }

  getCurrentUsername(): string | null {
    return this.authState.getValue().username;
  }

  getCurrentRole(): string | null {
    return this.authState.getValue().role;
  }

  getCurrentStatus(): string | null {
    return this.authState.getValue().status;
  }

  isAdmin(): boolean {
    return this.getCurrentRole() === 'ADMIN';
  }
}
