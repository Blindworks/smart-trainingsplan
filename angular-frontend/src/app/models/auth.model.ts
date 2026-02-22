export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
  email: string;
}

export interface AuthState {
  isLoggedIn: boolean;
  token: string | null;
  userId: number | null;
  username: string | null;
  email: string | null;
}
