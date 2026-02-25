import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { getRepuesto, getGaleria, getPartImageUrl, type Part } from '../api/repuestos'
import { getComentarios, createComentario, type Comentario } from '../api/comentarios'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useToast } from '../context/ToastContext'
import { LoadingModal } from '../components/LoadingModal'
import './DetalleProducto.css'

export function DetalleProducto() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user, isLoggedIn } = useAuth()
  const { add } = useCart()
  const toast = useToast()
  const [part, setPart] = useState<Part | null>(null)
  const [galleryCount, setGalleryCount] = useState(0)
  const [imageIndex, setImageIndex] = useState(0)
  const [comments, setComments] = useState<Comentario[]>([])
  const [loading, setLoading] = useState(true)
  const [commentBody, setCommentBody] = useState('')
  const [commentRating, setCommentRating] = useState(5)
  const [replyToId, setReplyToId] = useState<number | null>(null)
  const [replyBody, setReplyBody] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const partId = id ? parseInt(id, 10) : 0

  useEffect(() => {
    if (!partId) {
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    Promise.all([getRepuesto(partId), getGaleria(partId), getComentarios(partId)])
      .then(([p, g, c]) => {
        if (!cancelled) {
          setPart(p)
          setGalleryCount(g.count)
          setComments(c)
        }
      })
      .catch(() => { if (!cancelled) setPart(null) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [partId])

  async function handleSubmitComment(e: React.FormEvent) {
    e.preventDefault()
    if (!user || !commentBody.trim()) return
    setSubmitting(true)
    try {
      const newComment = await createComentario(partId, {
        userId: user.userId,
        rating: commentRating,
        body: commentBody.trim(),
      })
      setComments((prev) => [...prev, newComment])
      setCommentBody('')
    } catch {
      // ignore
    } finally {
      setSubmitting(false)
    }
  }

  async function handleSubmitReply(e: React.FormEvent, parentId: number) {
    e.preventDefault()
    if (!user || !replyBody.trim()) return
    setSubmitting(true)
    try {
      const newComment = await createComentario(partId, {
        userId: user.userId,
        parentId,
        body: replyBody.trim(),
      })
      function injectReply(tree: Comentario[], pid: number, reply: Comentario): Comentario[] {
        return tree.map((c) =>
          c.reviewId === pid
            ? { ...c, children: [...c.children, reply] }
            : { ...c, children: injectReply(c.children, pid, reply) }
        )
      }
      setComments((prev) => injectReply(prev, parentId, newComment))
      setReplyToId(null)
      setReplyBody('')
    } catch {
      // ignore
    } finally {
      setSubmitting(false)
    }
  }

  function handleAddToCart() {
    if (!part) return
    if (!isLoggedIn) {
      navigate('/login')
      return
    }
    add(part, 1)
    toast.success('Agregado al carrito')
  }

  if (loading) {
    return <LoadingModal open message="Cargando producto..." />
  }
  if (!part) {
    return (
      <div className="detalle-producto-page">
        <p>Producto no encontrado.</p>
        <Link to="/tienda" className="btn btn-primary">Volver al catálogo</Link>
      </div>
    )
  }

  const imageUrl = galleryCount > 0 ? getPartImageUrl(partId, imageIndex) : null

  return (
    <div className="detalle-producto-page">
      <nav className="detalle-breadcrumb">
        <Link to="/tienda">Catálogo</Link>
        <span className="sep">/</span>
        <span>{part.title}</span>
      </nav>

      <div className="detalle-grid">
        <section className="detalle-gallery">
          {galleryCount > 0 ? (
            <>
              <div className="gallery-main">
                <img src={imageUrl!} alt={part.title} />
              </div>
              {galleryCount > 1 && (
                <div className="gallery-thumbs">
                  {Array.from({ length: galleryCount }, (_, i) => (
                    <button
                      key={i}
                      type="button"
                      className={`thumb ${i === imageIndex ? 'active' : ''}`}
                      onClick={() => setImageIndex(i)}
                    >
                      <img src={getPartImageUrl(partId, i)} alt="" />
                    </button>
                  ))}
                </div>
              )}
            </>
          ) : (
            <div className="gallery-placeholder">
              <span>Sin imagen</span>
            </div>
          )}
        </section>

        <section className="detalle-info">
          <h1 className="detalle-title">{part.title}</h1>
          <p className="detalle-code">Código: {part.partNumber}</p>
          <p className="detalle-price">${Number(part.price).toFixed(2)}</p>
          {part.description && <p className="detalle-desc">{part.description}</p>}
          {part.weightLb != null && (
            <p className="detalle-meta">Peso: {Number(part.weightLb).toFixed(2)} lb</p>
          )}
          <p className={`detalle-stock ${part.inStock ? 'in-stock' : ''}`}>
            {part.inStock ? `En stock (${part.availableQuantity ?? part.stockQuantity ?? 0} disponibles)` : 'Sin stock'}
          </p>
          <button
            type="button"
            className="btn btn-primary btn-lg"
            disabled={!part.inStock}
            onClick={handleAddToCart}
          >
            Agregar al carrito
          </button>
        </section>
      </div>

      <section className="detalle-comments">
        <h2>Comentarios y valoraciones</h2>
        {isLoggedIn && user ? (
          <form onSubmit={handleSubmitComment} className="comment-form root-form">
            <label>Tu valoración (1-5 estrellas)</label>
            <select
              value={commentRating}
              onChange={(e) => setCommentRating(parseInt(e.target.value, 10))}
              className="rating-select"
            >
              {[5, 4, 3, 2, 1].map((r) => (
                <option key={r} value={r}>{r} ★</option>
              ))}
            </select>
            <label>Comentario</label>
            <textarea
              value={commentBody}
              onChange={(e) => setCommentBody(e.target.value)}
              placeholder="Escribe tu comentario..."
              rows={3}
              required
            />
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Enviando…' : 'Publicar'}
            </button>
          </form>
        ) : (
          <p className="comment-login-hint">Inicia sesión para comentar.</p>
        )}

        <div className="comment-tree">
          {comments.map((c) => (
            <CommentNode
              key={c.reviewId}
              comment={c}
              replyToId={replyToId}
              setReplyToId={setReplyToId}
              replyBody={replyBody}
              setReplyBody={setReplyBody}
              onSubmitReply={handleSubmitReply}
              submitting={submitting}
              isLoggedIn={!!isLoggedIn}
            />
          ))}
        </div>
      </section>
    </div>
  )
}

function CommentNode({
  comment,
  replyToId,
  setReplyToId,
  replyBody,
  setReplyBody,
  onSubmitReply,
  submitting,
  isLoggedIn,
  depth = 0,
}: {
  comment: Comentario
  replyToId: number | null
  setReplyToId: (id: number | null) => void
  replyBody: string
  setReplyBody: (s: string) => void
  onSubmitReply: (e: React.FormEvent, parentId: number) => void
  submitting: boolean
  isLoggedIn: boolean
  depth?: number
}) {
  const isReply = replyToId === comment.reviewId
  return (
    <div className={`comment-node ${depth > 0 ? 'reply' : ''}`} style={{ marginLeft: depth * 24 }}>
      <div className="comment-header">
        <span className="comment-author">{comment.fullName || comment.userEmail || 'Usuario'}</span>
        {comment.rating != null && (
          <span className="comment-rating">{'★'.repeat(comment.rating)}</span>
        )}
        {comment.createdAt && (
          <span className="comment-date">
            {new Date(comment.createdAt).toLocaleDateString()}
          </span>
        )}
      </div>
      <p className="comment-body">{comment.body}</p>
      {isLoggedIn && depth < 3 && (
        <>
          {!isReply ? (
            <button
              type="button"
              className="btn-link"
              onClick={() => setReplyToId(comment.reviewId)}
            >
              Responder
            </button>
          ) : (
            <form onSubmit={(e) => onSubmitReply(e, comment.reviewId)} className="reply-form">
              <textarea
                value={replyBody}
                onChange={(e) => setReplyBody(e.target.value)}
                placeholder="Escribe tu respuesta..."
                rows={2}
                required
              />
              <div className="reply-actions">
                <button type="submit" className="btn btn-sm btn-primary" disabled={submitting}>
                  Enviar
                </button>
                <button type="button" className="btn btn-sm" onClick={() => { setReplyToId(null); setReplyBody('') }}>
                  Cancelar
                </button>
              </div>
            </form>
          )}
        </>
      )}
      {comment.children.length > 0 && (
        <div className="comment-children">
          {comment.children.map((child) => (
            <CommentNode
              key={child.reviewId}
              comment={child}
              replyToId={replyToId}
              setReplyToId={setReplyToId}
              replyBody={replyBody}
              setReplyBody={setReplyBody}
              onSubmitReply={onSubmitReply}
              submitting={submitting}
              isLoggedIn={isLoggedIn}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  )
}
