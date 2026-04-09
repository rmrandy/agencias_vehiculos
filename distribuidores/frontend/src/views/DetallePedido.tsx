import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  getPedido,
  getPedidoReciboPdfUrl,
  type FabricaHistorialBlock,
  type FabricaPedidoStatusRow,
} from '../api/pedidos'
import { getPartImageUrl } from '../api/repuestos'
import { LoadingModal } from '../components/LoadingModal'
import { useCurrency } from '../context/CurrencyContext'
import './DetallePedido.css'

function formatDate(iso: string) {
  try {
    const d = new Date(iso)
    return (
      d.toLocaleDateString('es', { dateStyle: 'medium' }) +
      ' ' +
      d.toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' })
    )
  } catch {
    return iso
  }
}

function formatHistoryDate(iso: string) {
  return formatDate(iso)
}

type LineItem = {
  lineSource?: string
  partId?: number | null
  fabricaPartId?: number | null
  proveedorId?: number | null
  fabricaOrderId?: number | null
  fabricaBaseUrl?: string | null
  partNumber?: string | null
  partTitle?: string
  qty: number
  unitPrice: number
  lineTotal: number
  fabricaRemoteStatus?: {
    status?: string | null
    trackingNumber?: string | null
    etaDays?: number | null
  } | null
}

function lineThumbUrl(item: LineItem): string | null {
  if (item.lineSource === 'FABRICA' && item.fabricaPartId != null && item.fabricaBaseUrl) {
    return getPartImageUrl(item.fabricaPartId, 0, item.fabricaBaseUrl)
  }
  if (item.partId != null) {
    return getPartImageUrl(item.partId, 0)
  }
  return null
}

function fabricaReciboUrl(item: LineItem): string | null {
  if (
    item.lineSource === 'FABRICA' &&
    item.fabricaOrderId != null &&
    item.fabricaBaseUrl &&
    item.fabricaBaseUrl.length > 0
  ) {
    const base = item.fabricaBaseUrl.replace(/\/$/, '')
    return `${base}/api/pedidos/${item.fabricaOrderId}/recibo`
  }
  return null
}

export function DetallePedido() {
  const { formatOrder } = useCurrency()
  const { orderId } = useParams<{ orderId: string }>()
  const [data, setData] = useState<{
    order: {
      orderId: number
      orderNumber: string
      subtotal: number
      shippingTotal: number
      tariffTotal?: number
      total: number
      currency?: string
      orderType: string
      createdAt: string
    }
    items: LineItem[]
    status: { status: string; trackingNumber?: string; etaDays?: number }
    fabricaStatuses?: FabricaPedidoStatusRow[]
    fabricaHistoriales?: FabricaHistorialBlock[]
  } | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!orderId) {
      setLoading(false)
      return
    }
    let cancelled = false
    getPedido(Number(orderId))
      .then((d) => {
        if (!cancelled) setData(d)
      })
      .catch(() => {
        if (!cancelled) setData(null)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [orderId])

  /** Refresca estado desde la fábrica (el backend consulta el API remoto en cada GET). */
  useEffect(() => {
    if (!orderId) return
    const id = Number(orderId)
    const iv = setInterval(() => {
      getPedido(id)
        .then(setData)
        .catch(() => {})
    }, 45000)
    return () => clearInterval(iv)
  }, [orderId])

  if (loading)
    return (
      <div className="detalle-pedido-page">
        <LoadingModal open message="Cargando pedido..." />
      </div>
    )
  if (!data)
    return (
      <div className="detalle-pedido-page">
        <p>Pedido no encontrado.</p>
      </div>
    )

  const { order, items, status, fabricaStatuses, fabricaHistoriales } = data
  const idNum = order.orderId
  const cur = order.currency ?? 'USD'

  const statusLabel: Record<string, string> = {
    INITIATED: 'Iniciado',
    CONFIRMED: 'Confirmado',
    PREPARING: 'En preparación',
    IN_PREPARATION: 'En preparación',
    SHIPPED: 'Enviado',
    DELIVERED: 'Entregado',
    CANCELLED: 'Cancelado',
  }
  const fabHeader = fabricaStatuses?.[0]
  const primaryStatusCode = (fabHeader?.status || status.status) ?? 'INITIATED'
  const statusText = statusLabel[primaryStatusCode] ?? primaryStatusCode
  const showDualStatus = Boolean(
    fabHeader?.status && status.status && fabHeader.status.toUpperCase() !== status.status.toUpperCase()
  )
  const trackingPrimary = fabHeader?.trackingNumber || status.trackingNumber
  const etaPrimary = fabHeader?.etaDays ?? status.etaDays

  const fabricaReciboLinks = items
    .map((item) => {
      const url = fabricaReciboUrl(item)
      if (!url || item.fabricaOrderId == null) return null
      return { url, orderId: item.fabricaOrderId }
    })
    .filter((x): x is { url: string; orderId: number } => x != null)
  const uniqueFabricaRecibos = Array.from(
    new Map(fabricaReciboLinks.map((x) => [x.orderId, x])).values()
  )

  return (
    <div className="detalle-pedido-page">
      <div className="detalle-pedido-header detalle-pedido-card">
        <h1>Pedido {order.orderNumber}</h1>
        <p className="detalle-pedido-date">{formatDate(order.createdAt)}</p>
        <p className="order-type">{order.orderType === 'ENTERPRISE_API' ? 'Pedido empresarial' : 'Pedido web'}</p>
        <div className="detalle-pedido-status">
          {fabHeader && (
            <p className="detalle-sync-hint">
              Estado actualizado desde la fábrica
              {fabHeader.proveedorNombre ? ` · ${fabHeader.proveedorNombre}` : ''}
              {fabHeader.fabricaOrderId != null ? ` · Pedido #${fabHeader.fabricaOrderId}` : ''}
            </p>
          )}
          <span className="status-badge">{statusText}</span>
          {trackingPrimary && <span className="tracking">Tracking: {trackingPrimary}</span>}
          {etaPrimary != null && <span className="eta">Entrega estimada: {etaPrimary} días</span>}
          {showDualStatus && (
            <p className="detalle-local-status">
              Registro en distribuidora:{' '}
              <strong>{statusLabel[status.status] ?? status.status}</strong>
            </p>
          )}
        </div>

        {fabricaStatuses != null && fabricaStatuses.length > 1 && (
          <div className="detalle-fabrica-multi">
            <strong>Varios pedidos en fábrica:</strong>
            <ul>
              {fabricaStatuses.map((f) => (
                <li key={`${f.proveedorId}-${f.fabricaOrderId}`}>
                  {f.proveedorNombre ?? `Proveedor ${f.proveedorId}`}:{' '}
                  {statusLabel[(f.status ?? '').toUpperCase()] ?? f.status ?? '—'} (#{f.fabricaOrderId})
                </li>
              ))}
            </ul>
          </div>
        )}

        <div className="detalle-pedido-documents">
          <a
            href={getPedidoReciboPdfUrl(idNum)}
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-primary"
          >
            Descargar recibo PDF (distribuidora)
          </a>
          {uniqueFabricaRecibos.length > 0 && (
            <div className="fabrica-recibos">
              <span className="fabrica-recibos-label">Recibos en fábrica:</span>
              {uniqueFabricaRecibos.map(({ url, orderId: fo }) => (
                <a
                  key={fo}
                  href={url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn btn-secondary btn-sm"
                >
                  PDF fábrica #{fo}
                </a>
              ))}
            </div>
          )}
        </div>
      </div>

      {fabricaHistoriales != null && fabricaHistoriales.length > 0 && (
        <section
          className="detalle-pedido-card detalle-fabrica-historial"
          aria-labelledby="fabrica-historial-title"
        >
          <h2 id="fabrica-historial-title">Actualizaciones desde la fábrica</h2>
          <p className="fabrica-historial-intro">
            Historial de estados y comentarios tal como los registró el proveedor en su sistema.
          </p>
          {fabricaHistoriales.map((block) => (
            <div key={`${block.proveedorId}-${block.fabricaOrderId}`} className="fabrica-historial-block">
              <h3 className="fabrica-historial-block-title">
                {block.proveedorNombre ?? `Proveedor ${block.proveedorId}`} · Pedido fábrica #
                {block.fabricaOrderId}
              </h3>
              <ol className="fabrica-historial-timeline">
                {block.entries.map((e, idx) => (
                  <li
                    key={`${e.changedAt ?? ''}-${e.status ?? ''}-${idx}`}
                    className="fabrica-historial-entry"
                  >
                    <div className="fabrica-historial-meta">
                      {e.changedAt && (
                        <time className="fabrica-historial-time" dateTime={e.changedAt}>
                          {formatHistoryDate(e.changedAt)}
                        </time>
                      )}
                      <span className="fabrica-historial-status-pill">
                        {statusLabel[(e.status ?? '').toUpperCase()] ?? e.status ?? '—'}
                      </span>
                    </div>
                    {e.commentText != null && e.commentText.trim() !== '' && (
                      <p className="fabrica-historial-comment">{e.commentText.trim()}</p>
                    )}
                    {(e.trackingNumber || e.etaDays != null) && (
                      <p className="fabrica-historial-extra">
                        {e.trackingNumber != null && e.trackingNumber !== '' && (
                          <>Seguimiento: {e.trackingNumber}</>
                        )}
                        {e.trackingNumber != null &&
                          e.trackingNumber !== '' &&
                          e.etaDays != null &&
                          ' · '}
                        {e.etaDays != null && <>ETA: {e.etaDays} días</>}
                      </p>
                    )}
                  </li>
                ))}
              </ol>
            </div>
          ))}
        </section>
      )}

      <section className="detalle-pedido-items">
        <h2>Productos</h2>
        <ul className="detalle-items">
          {items.map((item, i) => {
            const thumb = lineThumbUrl(item)
            const fRecibo = fabricaReciboUrl(item)
            return (
              <li
                key={`${item.lineSource ?? 'L'}-${item.partId ?? ''}-${item.fabricaPartId ?? ''}-${item.proveedorId ?? ''}-${i}`}
                className="detalle-item-row"
              >
                <div className="item-thumb-wrap">
                  {thumb ? (
                    <img src={thumb} alt="" className="item-thumb" loading="lazy" />
                  ) : (
                    <span className="item-thumb-placeholder">📦</span>
                  )}
                </div>
                <div className="item-detail">
                  <span className="item-name">
                    {item.lineSource === 'FABRICA' && item.fabricaOrderId != null && (
                      <span className="item-fabrica-badge">Fábrica (pedido #{item.fabricaOrderId}) </span>
                    )}
                    {item.lineSource === 'FABRICA' &&
                    item.proveedorId != null &&
                    item.fabricaPartId != null ? (
                      <Link
                        to={`/producto/fabrica/${item.proveedorId}/${item.fabricaPartId}`}
                        className="item-name-link"
                      >
                        {item.partTitle ?? `Repuesto #${item.fabricaPartId}`}
                      </Link>
                    ) : (
                      item.partTitle ?? `Repuesto #${item.partId ?? item.fabricaPartId ?? '?'}`
                    )}
                  </span>
                  {item.partNumber && <span className="item-part-no">Código: {item.partNumber}</span>}
                  {item.fabricaRemoteStatus?.status && (
                    <span className="item-fabrica-remote">
                      Estado fábrica:{' '}
                      {statusLabel[item.fabricaRemoteStatus.status.toUpperCase()] ??
                        item.fabricaRemoteStatus.status}
                      {item.fabricaRemoteStatus.trackingNumber
                        ? ` · ${item.fabricaRemoteStatus.trackingNumber}`
                        : ''}
                    </span>
                  )}
                  {fRecibo && (
                    <a href={fRecibo} target="_blank" rel="noopener noreferrer" className="item-recibo-link">
                      Recibo PDF de esta línea (fábrica)
                    </a>
                  )}
                </div>
                <span className="item-qty">× {item.qty}</span>
                <span className="item-price">{formatOrder(Number(item.unitPrice), cur)} c/u</span>
                <span className="item-total">{formatOrder(Number(item.lineTotal), cur)}</span>
              </li>
            )
          })}
        </ul>
      </section>

      <section className="detalle-pedido-totals">
        <div className="totals-row">
          <span>Subtotal</span>
          <span>{formatOrder(Number(order.subtotal), cur)}</span>
        </div>
        {order.shippingTotal != null && Number(order.shippingTotal) > 0 && (
          <div className="totals-row">
            <span>Envío</span>
            <span>{formatOrder(Number(order.shippingTotal), cur)}</span>
          </div>
        )}
        {order.tariffTotal != null && Number(order.tariffTotal) > 0 && (
          <div className="totals-row">
            <span>Arancel (import.)</span>
            <span>{formatOrder(Number(order.tariffTotal), cur)}</span>
          </div>
        )}
        <div className="totals-row total">
          <span>Total ({cur})</span>
          <span>{formatOrder(Number(order.total), cur)}</span>
        </div>
      </section>

      <div className="detalle-pedido-actions">
        <Link to="/pedidos" className="btn btn-secondary">
          Mis pedidos
        </Link>
        <Link to="/tienda" className="btn btn-primary">
          Seguir comprando
        </Link>
      </div>
    </div>
  )
}
