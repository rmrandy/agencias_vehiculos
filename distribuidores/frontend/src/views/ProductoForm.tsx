import { useEffect, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { listCategorias } from '../api/categorias'
import { listMarcas } from '../api/marcas'
import { getRepuesto, getGaleria, createRepuesto, updateRepuesto, addImagenToProduct, type Part, type CreateRepuestoBody, type UpdateRepuestoBody } from '../api/repuestos'
import { LoadingModal } from '../components/LoadingModal'
import { useToast } from '../context/ToastContext'
import './ProductoForm.css'

const MAX_IMAGE_SIZE = 5 * 1024 * 1024 // 5MB

function fileToBase64(file: File): Promise<{ data: string; type: string }> {
  return new Promise((resolve, reject) => {
    if (file.size > MAX_IMAGE_SIZE) {
      reject(new Error('La imagen no debe superar 5MB'))
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      const type = file.type || 'image/jpeg'
      resolve({ data: result, type })
    }
    reader.onerror = () => reject(new Error('Error al leer el archivo'))
    reader.readAsDataURL(file)
  })
}

export function ProductoForm() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const isEdit = !!id
  const partId = id ? parseInt(id, 10) : 0

  const [categories, setCategories] = useState<{ categoryId: number; name: string }[]>([])
  const [brands, setBrands] = useState<{ brandId: number; name: string }[]>([])
  const [loading, setLoading] = useState(!!id)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const [categoryId, setCategoryId] = useState(0)
  const [brandId, setBrandId] = useState(0)
  const [partNumber, setPartNumber] = useState('')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [weightLb, setWeightLb] = useState<string>('')
  const [price, setPrice] = useState<string>('')
  const [stockQuantity, setStockQuantity] = useState<string>('')
  const [lowStockThreshold, setLowStockThreshold] = useState<string>('5')
  const [active, setActive] = useState(1)
  const [imageData, setImageData] = useState<string | null>(null)
  const [imageType, setImageType] = useState<string>('')
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [galleryCount, setGalleryCount] = useState(0)
  const [addingImage, setAddingImage] = useState(false)
  const toast = useToast()

  useEffect(() => {
    listCategorias().then(setCategories).catch(() => [])
    listMarcas().then(setBrands).catch(() => [])
  }, [])

  useEffect(() => {
    if (!isEdit) return
    Promise.all([getRepuesto(partId), getGaleria(partId)])
      .then(([p, g]) => {
        setCategoryId(p.categoryId ?? 0)
        setBrandId(p.brandId ?? 0)
        setPartNumber(p.partNumber)
        setTitle(p.title)
        setDescription(p.description ?? '')
        setWeightLb(p.weightLb != null ? String(p.weightLb) : '')
        setPrice(String(p.price))
        setStockQuantity(p.stockQuantity != null ? String(p.stockQuantity) : '')
        setLowStockThreshold('5')
        setActive(p.active ?? 1)
        setGalleryCount(g.count)
      })
      .catch(() => setError('No se pudo cargar el producto'))
      .finally(() => setLoading(false))
  }, [isEdit, partId])

  function handleImageChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    fileToBase64(file)
      .then(({ data, type }) => {
        setImageData(data)
        setImageType(type)
        setImagePreview(data)
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Error en imagen'))
  }

  async function handleAddGalleryImage(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file || !isEdit) return
    setAddingImage(true)
    setError('')
    try {
      const { data, type } = await fileToBase64(file)
      const strip = data.includes(',') ? data.split(',')[1]! : data
      const result = await addImagenToProduct(partId, strip, type)
      setGalleryCount(result.count)
      toast.success('Imagen añadida a la galería')
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al añadir imagen'
      setError(msg)
      toast.error(msg)
    } finally {
      setAddingImage(false)
    }
    e.target.value = ''
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSaving(true)
    const numPrice = parseFloat(price)
    const numWeight = weightLb ? parseFloat(weightLb) : undefined
    const numStock = stockQuantity ? parseInt(stockQuantity, 10) : 0
    const numThreshold = lowStockThreshold ? parseInt(lowStockThreshold, 10) : 5
    if (isNaN(numPrice) || numPrice < 0) {
      setError('Precio inválido')
      setSaving(false)
      return
    }
    if (!isEdit && (categoryId <= 0 || brandId <= 0)) {
      setError('Seleccione categoría y marca')
      setSaving(false)
      return
    }

    const stripDataUrl = (s: string) => (s.includes(',') ? s.split(',')[1]! : s)

    if (isEdit) {
      const body: UpdateRepuestoBody = {
        categoryId: categoryId || undefined,
        brandId: brandId || undefined,
        title: title.trim(),
        description: description.trim() || undefined,
        weightLb: numWeight,
        price: numPrice,
        active,
        stockQuantity: numStock,
        lowStockThreshold: numThreshold,
      }
      if (imageData) {
        body.imageData = stripDataUrl(imageData)
        body.imageType = imageType
      }
      updateRepuesto(partId, body)
        .then(() => {
          toast.success('Producto actualizado')
          navigate('/productos', { replace: true })
        })
        .catch((err) => setError(err instanceof Error ? err.message : 'Error al guardar'))
        .finally(() => setSaving(false))
    } else {
      const body: CreateRepuestoBody = {
        categoryId: categoryId || 0,
        brandId: brandId || 0,
        partNumber: partNumber.trim(),
        title: title.trim(),
        description: description.trim() || undefined,
        weightLb: numWeight,
        price: numPrice,
        stockQuantity: numStock,
        lowStockThreshold: numThreshold,
      }
      if (imageData) {
        body.imageData = stripDataUrl(imageData)
        body.imageType = imageType
      }
      createRepuesto(body)
        .then(() => {
          toast.success('Producto creado')
          navigate('/productos', { replace: true })
        })
        .catch((err) => setError(err instanceof Error ? err.message : 'Error al crear'))
        .finally(() => setSaving(false))
    }
  }

  if (loading) return <div className="producto-form"><LoadingModal open message="Cargando producto..." /></div>

  return (
    <div className="producto-form">
      <header className="page-header">
        <h1>{isEdit ? 'Editar producto' : 'Nuevo producto'}</h1>
        <Link to="/productos" className="btn btn-secondary">Volver al listado</Link>
      </header>
      {error && <div className="form-error">{error}</div>}
      <LoadingModal open={saving} message="Guardando..." />
      <form onSubmit={handleSubmit} className="producto-form-fields">
        <div className="form-group">
          <label>Categoría *</label>
          <select value={categoryId} onChange={(e) => setCategoryId(parseInt(e.target.value, 10))} required>
            <option value={0}>Seleccione</option>
            {categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Marca *</label>
          <select value={brandId} onChange={(e) => setBrandId(parseInt(e.target.value, 10))} required>
            <option value={0}>Seleccione</option>
            {brands.map((b) => (
              <option key={b.brandId} value={b.brandId}>{b.name}</option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Número de parte *</label>
          <input
            type="text"
            value={partNumber}
            onChange={(e) => setPartNumber(e.target.value)}
            required
            disabled={isEdit}
            placeholder="Ej: MF-OIL-001"
          />
        </div>
        <div className="form-group">
          <label>Título *</label>
          <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} required />
        </div>
        <div className="form-group">
          <label>Descripción</label>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
        </div>
        <div className="form-row-two">
          <div className="form-group">
            <label>Peso (lb)</label>
            <input type="number" step="0.01" min="0" value={weightLb} onChange={(e) => setWeightLb(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Precio *</label>
            <input type="number" step="0.01" min="0" value={price} onChange={(e) => setPrice(e.target.value)} required />
          </div>
        </div>
        <div className="form-row-two">
          <div className="form-group">
            <label>Stock</label>
            <input type="number" min="0" value={stockQuantity} onChange={(e) => setStockQuantity(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Umbral bajo stock</label>
            <input type="number" min="0" value={lowStockThreshold} onChange={(e) => setLowStockThreshold(e.target.value)} />
          </div>
        </div>
        {isEdit && (
          <div className="form-group">
            <label>
              <input type="checkbox" checked={active === 1} onChange={(e) => setActive(e.target.checked ? 1 : 0)} />
              Activo
            </label>
          </div>
        )}
        <div className="form-group">
          <label>Imagen principal (máx. 5MB, JPEG/PNG/GIF/WebP)</label>
          <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" onChange={handleImageChange} />
          {imagePreview && (
            <div className="image-preview">
              <img src={imagePreview} alt="Vista previa" />
            </div>
          )}
        </div>
        {isEdit && (
          <div className="form-group">
            <label>Galería ({galleryCount} imagen{galleryCount !== 1 ? 'es' : ''})</label>
            <p className="form-hint">Añade más fotos para la página de detalle del producto.</p>
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              onChange={handleAddGalleryImage}
              disabled={addingImage}
            />
            {addingImage && <span className="adding-label">Añadiendo…</span>}
          </div>
        )}
        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Guardando…' : isEdit ? 'Guardar cambios' : 'Crear producto'}
          </button>
        </div>
      </form>
    </div>
  )
}
