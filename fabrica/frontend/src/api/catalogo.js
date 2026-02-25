import { apiFetch } from './config'

// ============================================================================
// CATEGORÍAS
// ============================================================================
export async function listCategorias() {
  return apiFetch('/api/categorias')
}

export async function getCategoria(id) {
  return apiFetch(`/api/categorias/${id}`)
}

export async function createCategoria(data) {
  return apiFetch('/api/categorias', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function updateCategoria(id, data) {
  return apiFetch(`/api/categorias/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deleteCategoria(id) {
  return apiFetch(`/api/categorias/${id}`, { method: 'DELETE' })
}

// ============================================================================
// MARCAS
// ============================================================================
export async function listMarcas() {
  return apiFetch('/api/marcas')
}

export async function getMarca(id) {
  return apiFetch(`/api/marcas/${id}`)
}

export async function createMarca(data) {
  return apiFetch('/api/marcas', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function updateMarca(id, data) {
  return apiFetch(`/api/marcas/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deleteMarca(id) {
  return apiFetch(`/api/marcas/${id}`, { method: 'DELETE' })
}

// ============================================================================
// VEHÍCULOS
// ============================================================================
export async function listVehiculos() {
  return apiFetch('/api/vehiculos')
}

export async function getVehiculo(id) {
  return apiFetch(`/api/vehiculos/${id}`)
}

export async function createVehiculo(data) {
  return apiFetch('/api/vehiculos', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function updateVehiculo(id, data) {
  return apiFetch(`/api/vehiculos/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deleteVehiculo(id) {
  return apiFetch(`/api/vehiculos/${id}`, { method: 'DELETE' })
}

// ============================================================================
// REPUESTOS
// ============================================================================
export async function listRepuestos(params = {}) {
  const query = new URLSearchParams()
  if (params.categoryId) query.set('categoryId', params.categoryId)
  if (params.brandId) query.set('brandId', params.brandId)
  const qs = query.toString()
  return apiFetch(`/api/repuestos${qs ? '?' + qs : ''}`)
}

export async function getRepuesto(id) {
  return apiFetch(`/api/repuestos/${id}`)
}

export async function getRepuestoByNumber(partNumber) {
  return apiFetch(`/api/repuestos/numero/${partNumber}`)
}

export async function createRepuesto(data) {
  return apiFetch('/api/repuestos', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function updateRepuesto(id, data) {
  return apiFetch(`/api/repuestos/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deleteRepuesto(id) {
  return apiFetch(`/api/repuestos/${id}`, { method: 'DELETE' })
}

// Búsqueda por nombre, descripción, especificaciones
export async function buscarRepuestos(params = {}) {
  const query = new URLSearchParams()
  if (params.nombre) query.set('nombre', params.nombre)
  if (params.descripcion) query.set('descripcion', params.descripcion)
  if (params.especificaciones) query.set('especificaciones', params.especificaciones)
  const qs = query.toString()
  return apiFetch(`/api/repuestos/busqueda${qs ? '?' + qs : ''}`)
}

// Exportar repuestos a JSON
export async function exportRepuestos(userId) {
  const qs = userId != null ? `?userId=${userId}` : ''
  return apiFetch(`/api/repuestos/export${qs}`)
}

// Importar repuestos desde JSON
export async function importRepuestos({ userId, fileName, items }) {
  return apiFetch('/api/repuestos/import', {
    method: 'POST',
    body: JSON.stringify({ userId, fileName, items: items || [] }),
  })
}

// Carga masiva de inventario
export async function importInventarioRepuestos({ userId, fileName, items }) {
  return apiFetch('/api/repuestos/import-inventario', {
    method: 'POST',
    body: JSON.stringify({ userId, fileName, items: items || [] }),
  })
}

// Alta de inventario (agregar unidades y registrar en log)
export async function addInventarioRepuesto(partId, { userId, cantidad }) {
  return apiFetch(`/api/repuestos/${partId}/inventario/alta`, {
    method: 'POST',
    body: JSON.stringify({ userId, cantidad }),
  })
}
