import type {
  LoginRequest,
  LoginResponse,
  CurrentUser,
  RegisterRequest,
} from '../types/auth'

export async function loginApi(
  data: LoginRequest
): Promise<LoginResponse> {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Email hoặc mật khẩu không đúng')
    }

    throw new Error(
      `Đăng nhập thất bại (HTTP ${response.status})`
    )
  }

  return (await response.json()) as LoginResponse
}

export async function getCurrentUserApi(
  token: string
): Promise<CurrentUser> {
  const response = await fetch('/api/v1/auth/me', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    throw new Error(
      `Không thể lấy thông tin tài khoản (HTTP ${response.status})`
    )
  }

  return (await response.json()) as CurrentUser
}


export async function registerApi(
  data: RegisterRequest
): Promise<void> {
  const response = await fetch('/api/v1/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    if (response.status === 409) {
      throw new Error('Email này đã được đăng ký')
    }

    if (response.status === 400) {
      throw new Error(
        'Thông tin đăng ký không hợp lệ. Vui lòng kiểm tra lại.'
      )
    }

    throw new Error(
      `Đăng ký thất bại (HTTP ${response.status})`
    )
  }
}
