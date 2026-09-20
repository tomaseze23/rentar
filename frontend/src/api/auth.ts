import { ApiError, request } from './http'

export const authApi = {
  async login(email: string, password: string): Promise<{ token: string }> {
    try {
      return await request<{ token: string }>('/api/auth/login', {
        method: 'POST',
        body: { email, password },
      })
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        throw new ApiError(401, 'Email o contraseña incorrectos.')
      }
      throw error
    }
  },
}
