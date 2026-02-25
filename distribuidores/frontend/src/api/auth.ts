import { apiFetch } from './config'

export interface User {
  userId: number
  email: string
  fullName?: string
  phone?: string
  status: string
  roles: string[]
}

export async function login(email: string, password: string): Promise<User> {
  return apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export interface RegisterParams {
  email: string
  password: string
  fullName?: string
  phone?: string
}

export interface RegisterResponse {
  userId: number
  email: string
  message: string
}

export async function register(params: RegisterParams): Promise<RegisterResponse> {
  return apiFetch('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({
      email: params.email,
      password: params.password,
      fullName: params.fullName || undefined,
      phone: params.phone || undefined,
    }),
  })
}
