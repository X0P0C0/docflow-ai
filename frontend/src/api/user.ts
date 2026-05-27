import { http } from "@/utils/http";

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

type BackendCurrentUser = {
  id: number;
  username: string;
  nickname?: string;
  realName?: string;
  email?: string;
  phone?: string;
  avatar?: string;
  roles?: string[];
  permissions?: string[];
  capabilities?: string[];
};

type BackendLoginResult = {
  token: string;
  expireSeconds: number;
  user: BackendCurrentUser;
};

export type UserResult = {
  code: number;
  message: string;
  data: {
    avatar: string;
    username: string;
    nickname: string;
    roles: Array<string>;
    permissions: Array<string>;
    accessToken: string;
    refreshToken: string;
    expires: Date;
  };
};

export type RefreshTokenResult = UserResult;

export type UserInfo = {
  avatar: string;
  username: string;
  nickname: string;
  email: string;
  phone: string;
  description: string;
};

export type UserInfoResult = {
  code: number;
  message: string;
  data: UserInfo;
};

function toFrontendUser(user: BackendCurrentUser): UserInfo {
  return {
    avatar: user.avatar ?? "",
    username: user.username,
    nickname: user.nickname ?? user.realName ?? user.username,
    email: user.email ?? "",
    phone: user.phone ?? "",
    description: user.capabilities?.join(" / ") ?? ""
  };
}

function toTokenPayload(payload: BackendLoginResult): UserResult["data"] {
  const expiresAt = new Date(Date.now() + payload.expireSeconds * 1000);

  return {
    avatar: payload.user.avatar ?? "",
    username: payload.user.username,
    nickname:
      payload.user.nickname ?? payload.user.realName ?? payload.user.username,
    roles: payload.user.roles ?? [],
    permissions: payload.user.permissions ?? [],
    accessToken: payload.token,
    // Backend has no refresh-token endpoint yet; keep compatibility with the
    // existing auth storage shape so the session remains usable.
    refreshToken: payload.token,
    expires: expiresAt
  };
}

/** 登录 */
export const getLogin = async (data?: object) => {
  const result = await http.request<ApiResult<BackendLoginResult>>(
    "post",
    "/api/auth/login",
    { data }
  );

  return {
    code: result.code,
    message: result.message,
    data: toTokenPayload(result.data)
  } satisfies UserResult;
};

/** 当前后端未提供 refresh-token 接口，这里显式返回失败，交由现有鉴权兜底登出。 */
export const refreshTokenApi = async (
  _data?: object
): Promise<RefreshTokenResult> => {
  return Promise.reject(new Error("Refresh token API is not available"));
};

/** 账户设置-个人信息 */
export const getMine = async () => {
  const result = await http.request<ApiResult<BackendCurrentUser>>(
    "get",
    "/api/auth/me"
  );

  return {
    code: result.code,
    message: result.message,
    data: toFrontendUser(result.data)
  } satisfies UserInfoResult;
};

/** 账户设置-个人安全日志 */
export const getMineLogs = async () => {
  return {
    code: 200,
    message: "Not implemented",
    data: {
      list: [],
      total: 0,
      pageSize: 0,
      currentPage: 1
    }
  };
};
