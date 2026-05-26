export interface LoginForm {
  username: string;
  password: string;
}

export interface UserInfo {
  id: string;
  username: string;
  displayName: string;
  orgName: string;
  roles: string[];
  permissions: string[];
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  username: string;
  displayName: string;
  authorities: string[];
  user: UserInfo;
}
