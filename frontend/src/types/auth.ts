
export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  userId: number
  fullName: string
  email: string
  role: string
}

export interface CurrentUser {
  id: number
  fullName: string
  email: string
  role: string
}


export interface RegisterRequest {
  fullName: string
  email: string
  password: string
}
