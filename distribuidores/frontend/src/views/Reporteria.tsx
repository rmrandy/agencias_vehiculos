import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCurrency } from '../context/CurrencyContext'
import {
  defaultReportDateRange,
  getMasVendidos,
  getPedidosPorEstado,
  getVentasDiarias,
  type MasVendidoRow,
  type PedidoEstadoRow,
  type VentaDiariaRow,
} from '../api/reportes'
import { LoadingModal } from '../components/LoadingModal'
import { useToast } from '../context/ToastContext'
import './Reporteria.css'

const ESTADO_LABEL: Record<string, string> = {
  INITIATED: 'Iniciado',
  CONFIRMED: 'Confirmado',
  PREPARING: 'En preparación',
  IN_PREPARATION: 'En preparación',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
}

export function Reporteria() {
  const { user, isLoggedIn } = useAuth()
  const { formatOrder, selectedCode } = useCurrency()
  const toast = useToast()
  const range = defaultReportDateRange()
  const [from, setFrom] = useState(range.from)
  const [to, setTo] = useState(range.to)
  const [loading, setLoading] = useState(false)
  const [masVendidos, setMasVendidos] = useState<MasVendidoRow[]>([])
  const [ventasDia, setVentasDia] = useState<VentaDiariaRow[]>([])
  const [porEstado, setPorEstado] = useState<PedidoEstadoRow[]>([])

  const canView =
    isLoggedIn && user && (user.roles?.includes('ADMIN') || user.roles?.includes('EMPLOYEE'))

  async function loadAll() {
    if (!user) return
    setLoading(true)
    try {
      const [mv, vd, pe] = await Promise.all([
        getMasVendidos(user.userId, from, to, 30),
        getVentasDiarias(user.userId, from, to),
        getPedidosPorEstado(user.userId, from, to),
      ])
      setMasVendidos(mv)
      setVentasDia(vd)
      setPorEstado(pe)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Error al cargar reportes')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (canView) void loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps -- recarga explícita con botón
  }, [canView, user?.userId])

  if (!isLoggedIn || !user) {
    return (
      <div className="reporteria-page">
        <p>Inicia sesión para continuar.</p>
      </div>
    )
  }

  if (!canView) {
    return (
      <div className="reporteria-page">
        <p>No tienes permiso para ver reportería (Admin o Empleado).</p>
      </div>
    )
  }

  return (
    <div className="reporteria-page">
      <LoadingModal open={loading} message="Generando reportes…" />
      <header className="reporteria-header">
        <h1>Reportería</h1>
        <p className="reporteria-sub">
          Tres vistas sobre pedidos locales: más vendidos, ventas por día y distribución por estado (pedidos cancelados
          excluidos de ventas e importes).
        </p>
        <div className="reporteria-filters">
          <label>
            Desde
            <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </label>
          <label>
            Hasta
            <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </label>
          <button type="button" className="btn btn-primary" onClick={() => void loadAll()}>
            Actualizar
          </button>
        </div>
      </header>

      <section className="reporteria-section">
        <h2>Repuestos más vendidos (catálogo local)</h2>
        <p className="reporteria-hint">Unidades vendidas en el rango de fechas; solo líneas <code>LOCAL</code>.</p>
        <div className="reporteria-table-wrap">
          <table className="reporteria-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Código</th>
                <th>Descripción</th>
                <th>Unidades</th>
                <th>Importe</th>
              </tr>
            </thead>
            <tbody>
              {masVendidos.length === 0 ? (
                <tr>
                  <td colSpan={5} className="reporteria-empty">
                    Sin datos en el período.
                  </td>
                </tr>
              ) : (
                masVendidos.map((r, i) => (
                  <tr key={r.partId}>
                    <td>{i + 1}</td>
                    <td>
                      <Link to={`/producto/${r.partId}`}>{r.partNumber ?? '—'}</Link>
                    </td>
                    <td>{r.partTitle ?? '—'}</td>
                    <td>{r.totalQty}</td>
                    <td>{formatOrder(Number(r.totalImporte), selectedCode)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="reporteria-section">
        <h2>Ventas por día</h2>
        <p className="reporteria-hint">Número de pedidos no cancelados e importe total del pedido por día.</p>
        <div className="reporteria-table-wrap">
          <table className="reporteria-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Pedidos</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              {ventasDia.length === 0 ? (
                <tr>
                  <td colSpan={3} className="reporteria-empty">
                    Sin datos.
                  </td>
                </tr>
              ) : (
                ventasDia.map((r) => (
                  <tr key={r.fecha}>
                    <td>{new Date(r.fecha).toLocaleDateString('es', { dateStyle: 'medium' })}</td>
                    <td>{r.pedidoCount}</td>
                    <td>{formatOrder(Number(r.totalImporte), selectedCode)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="reporteria-section">
        <h2>Pedidos por estado actual</h2>
        <p className="reporteria-hint">
          Pedidos creados en el período, agrupados según su último estado en historial.
        </p>
        <div className="reporteria-table-wrap">
          <table className="reporteria-table">
            <thead>
              <tr>
                <th>Estado</th>
                <th>Cantidad</th>
                <th>Suma importes</th>
              </tr>
            </thead>
            <tbody>
              {porEstado.length === 0 ? (
                <tr>
                  <td colSpan={3} className="reporteria-empty">
                    Sin datos.
                  </td>
                </tr>
              ) : (
                porEstado.map((r) => (
                  <tr key={r.estado}>
                    <td>{ESTADO_LABEL[r.estado] ?? r.estado}</td>
                    <td>{r.cantidadPedidos}</td>
                    <td>{formatOrder(Number(r.totalImporte), selectedCode)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
