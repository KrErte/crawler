export interface LoginRequest {
  email: string;
  password: string;
  totpCode?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: UserDto;
  requiresTwoFactor?: boolean;
}

export interface UserDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  isAdmin: boolean;
}
