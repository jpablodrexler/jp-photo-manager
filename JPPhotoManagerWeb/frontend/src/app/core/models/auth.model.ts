export interface LoginResponse {
  username: string;
  expiresAt: string;
}

export interface MeResponse {
  username: string;
  role: string;
}

export interface Session {
  username: string;
  expiresAt: number;
  role: string;
}

export interface SessionInfo {
  id: number;
  deviceHint: string;
  lastUsedAt: string;
  current: boolean;
}
