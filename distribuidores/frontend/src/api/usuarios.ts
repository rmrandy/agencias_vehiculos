import { apiFetch } from './config'

export interface UsuarioDto {
  userId: number
  email: string
  fullName: string | null
  phone: string | null
  status: string
  createdAt: string | null
  roles: string[]
}

/** Listar todos los usuarios (requiere ADMIN). */
export async function getUsuarios(adminUserId: number): Promise<UsuarioDto[]> {
  return apiFetch(`/api/usuarios?userId=${adminUserId}`)
}

/** Actualizar usuario (estado o roles). Requiere ADMIN. */
export async function updateUsuario(
  id: number,
  body: { adminUserId: number; status?: string; roleNames?: string[] }
): Promise<{ userId: number; status: string }> {
  return apiFetch(`/api/usuarios/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  })
}
