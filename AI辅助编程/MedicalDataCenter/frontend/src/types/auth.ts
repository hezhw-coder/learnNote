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
  expiresIn: number;
  user: UserInfo;
}
