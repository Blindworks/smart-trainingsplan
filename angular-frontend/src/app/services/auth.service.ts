import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthResponse, AuthState, LoginRequest, RegisterRequest } from '../models/auth.model';
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
          isLoggedIn: true,
          token,
          userId: user.userId,
          username: user.username,
          email: user.email,
          role: user.role ?? null
        };
      } catch {
        return { isLoggedIn: false, token: null, userId: null, username: null, email: null, role: null };
      }
    }
    return { isLoggedIn: false, token: null, userId: null, username: null, email: null, role: null };
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

  private handleAuthResponse(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(response));
    this.authState.next({
      isLoggedIn: true,
      token: response.token,
      userId: response.userId,
      username: response.username,
      email: response.email,
      role: response.role
    });
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.authState.next({ isLoggedIn: false, token: null, userId: null, username: null, email: null, role: null });
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

  isAdmin(): boolean {
    return this.getCurrentRole() === 'ADMIN';
  }
}
