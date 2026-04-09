import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getAranceles,
  getTarifaEnvioPorLibra,
  putArancel,
  putTarifaEnvioPorLibra,
  type ArancelPaisRow,
} from '../api/aranceles'
import { getMonedas, putMonedaTasa, type MonedaRow } from '../api/monedas'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { LoadingModal } from '../components/LoadingModal'
import './ArancelesAdmin.css'

export function ArancelesAdmin() {
  const { user, isLoggedIn } = useAuth()
  const toast = useToast()
  const [rows, setRows] = useState<ArancelPaisRow[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState<string | null>(null)
  const [drafts, setDrafts] = useState<Record<string, string>>({})
  const [envioDraft, setEnvioDraft] = useState('0')
  const [envioSaving, setEnvioSaving] = useState(false)
  const [monedasList, setMonedasList] = useState<MonedaRow[]>([])
  const [monedaDrafts, setMonedaDrafts] = useState<Record<string, string>>({})
  const [savingMoneda, setSavingMoneda] = useState<string | null>(null)

  const isAdmin = isLoggedIn && user?.roles?.includes('ADMIN')

  useEffect(() => {
    if (!isAdmin) {
      setLoading(false)
      return
    }
    ;(async () => {
      const [arRes, envRes, monRes] = await Promise.allSettled([
        getAranceles(),
        getTarifaEnvioPorLibra(),
        getMonedas(),
      ])
      if (arRes.status === 'fulfilled') {
        const list = arRes.value
        setRows(list)
        const d: Record<string, string> = {}
        for (const r of list) d[r.countryCode] = String(r.tariffPercent)
        setDrafts(d)
      } else {
        toast.error('No se pudieron cargar los aranceles')
        setRows([])
      }
      if (envRes.status === 'fulfilled') {
        setEnvioDraft(String(envRes.value.usdPerLb))
      }
      if (monRes.status === 'fulfilled') {
        const ml = monRes.value
        setMonedasList(ml)
        const md: Record<string, string> = {}
        for (const m of ml) md[m.code] = String(m.unitsPerUsd)
        setMonedaDrafts(md)
      }
      setLoading(false)
    })()
  }, [isAdmin])

  async function saveRow(countryCode: string) {
    if (!user) return
    const raw = drafts[countryCode]?.replace(',', '.').trim() ?? '0'
    const n = Number(raw)
    if (Number.isNaN(n) || n < 0 || n > 100) {
      toast.error('El arancel debe ser un número entre 0 y 100')
      return
    }
    setSaving(countryCode)
    try {
      const updated = await putArancel(countryCode, n, user.userId)
      setRows((prev) => prev.map((r) => (r.countryCode === countryCode ? updated : r)))
      setDrafts((d) => ({ ...d, [countryCode]: String(updated.tariffPercent) }))
      toast.success('Arancel actualizado')
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'No se pudo guardar')
    } finally {
      setSaving(null)
    }
  }

  async function saveEnvioTarifa() {
    if (!user) return
    const raw = envioDraft.replace(',', '.').trim() || '0'
    const n = Number(raw)
    if (Number.isNaN(n) || n < 0) {
      toast.error('La tarifa debe ser un número mayor o igual a 0')
      return
    }
    setEnvioSaving(true)
    try {
      const updated = await putTarifaEnvioPorLibra(n, user.userId)
      setEnvioDraft(String(updated.usdPerLb))
      toast.success('Tarifa de envío actualizada')
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'No se pudo guardar')
    } finally {
      setEnvioSaving(false)
    }
  }

  async function saveMonedaRow(code: string) {
    if (!user) return
    if (code === 'USD') {
      toast.error('USD siempre equivale a 1 (referencia)')
      return
    }
    const raw = monedaDrafts[code]?.replace(',', '.').trim() ?? '0'
    const n = Number(raw)
    if (Number.isNaN(n) || n <= 0) {
      toast.error('El tipo de cambio debe ser un número mayor que 0')
      return
    }
    setSavingMoneda(code)
    try {
      const updated = await putMonedaTasa(code, n, user.userId)
      setMonedasList((prev) => prev.map((m) => (m.code === code ? updated : m)))
      setMonedaDrafts((d) => ({ ...d, [code]: String(updated.unitsPerUsd) }))
      toast.success(`Tipo de cambio ${code} actualizado`)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'No se pudo guardar')
    } finally {
      setSavingMoneda(null)
    }
  }

  if (!isLoggedIn || !user) {
    return (
      <div className="aranceles-admin">
        <p>Debes iniciar sesión.</p>
        <Link to="/login">Iniciar sesión</Link>
      </div>
    )
  }

  if (!isAdmin) {
    return (
      <div className="aranceles-admin">
        <p>Solo administradores pueden editar aranceles, envío y divisas.</p>
        <Link to="/">Volver al inicio</Link>
      </div>
    )
  }

  return (
    <div className="aranceles-admin">
      <LoadingModal open={loading} message="Cargando aranceles..." />
      <header className="aranceles-header">
        <h1>Aranceles, envío y divisas</h1>
        <p className="aranceles-sub">
          Aranceles: porcentaje sobre el subtotal importado (fábrica) según país de destino. Envío: cargo global en USD
          por libra según el peso total del pedido. Divisas: cuántas unidades de cada moneda equivalen a 1 USD al cobrar
          (el catálogo sigue en USD como referencia).
        </p>
      </header>
      {!loading && (
        <section className="aranceles-envio-card" aria-labelledby="envio-tarifa-heading">
          <h2 id="envio-tarifa-heading" className="aranceles-envio-title">
            Tarifa de envío (USD / lb)
          </h2>
          <p className="aranceles-sub">
            Se multiplica por la suma de (peso en lb × cantidad) de cada línea. Si un producto no tiene peso, no suma al
            envío. En 0 no hay cargo por peso.
          </p>
          <div className="aranceles-envio-row">
            <input
              type="text"
              inputMode="decimal"
              className="aranceles-input"
              value={envioDraft}
              onChange={(e) => setEnvioDraft(e.target.value)}
              aria-label="Tarifa USD por libra"
            />
            <button
              type="button"
              className="btn btn-primary btn-sm"
              disabled={envioSaving}
              onClick={() => void saveEnvioTarifa()}
            >
              {envioSaving ? 'Guardando…' : 'Guardar tarifa'}
            </button>
          </div>
        </section>
      )}
      {!loading && monedasList.length > 0 && (
        <section className="aranceles-envio-card" aria-labelledby="monedas-heading">
          <h2 id="monedas-heading" className="aranceles-envio-title">
            Tipo de cambio (unidades por 1 USD)
          </h2>
          <p className="aranceles-sub">
            Ejemplo: si 1 USD = 7,85 GTQ, en la fila GTQ indica <code>7.85</code>. USD debe permanecer en 1.
          </p>
          <div className="aranceles-table-wrap">
            <table className="aranceles-table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nombre</th>
                  <th>Símbolo</th>
                  <th>Unidades / 1 USD</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {monedasList.map((m) => (
                  <tr key={m.code}>
                    <td>
                      <code>{m.code}</code>
                    </td>
                    <td>{m.name}</td>
                    <td>{m.symbol}</td>
                    <td>
                      <input
                        type="text"
                        inputMode="decimal"
                        className="aranceles-input moneda-input-wide"
                        disabled={m.code === 'USD'}
                        value={monedaDrafts[m.code] ?? ''}
                        onChange={(e) =>
                          setMonedaDrafts((d) => ({ ...d, [m.code]: e.target.value }))
                        }
                        aria-label={`Tipo de cambio ${m.code}`}
                      />
                    </td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        disabled={m.code === 'USD' || savingMoneda === m.code}
                        onClick={() => void saveMonedaRow(m.code)}
                      >
                        {savingMoneda === m.code ? 'Guardando…' : 'Guardar'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {!loading && (
        <div className="aranceles-table-wrap">
          <h2 className="aranceles-section-title">Aranceles por país (LATAM)</h2>
          <table className="aranceles-table">
            <thead>
              <tr>
                <th>País</th>
                <th>Código</th>
                <th>Arancel %</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.countryCode}>
                  <td>{r.countryName}</td>
                  <td>
                    <code>{r.countryCode}</code>
                  </td>
                  <td>
                    <input
                      type="text"
                      inputMode="decimal"
                      className="aranceles-input"
                      value={drafts[r.countryCode] ?? ''}
                      onChange={(e) =>
                        setDrafts((d) => ({ ...d, [r.countryCode]: e.target.value }))
                      }
                      aria-label={`Arancel % ${r.countryName}`}
                    />
                  </td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      disabled={saving === r.countryCode}
                      onClick={() => saveRow(r.countryCode)}
                    >
                      {saving === r.countryCode ? 'Guardando…' : 'Guardar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
