import { ref } from 'vue'

const STORAGE_KEY = 'fabrica_store_info'
const DEFAULT_INFO = {
  name: 'Fabrica',
  subtitle: 'Agencias Vehiculos',
  generalInfo: 'Catalogo principal',
}

const storeInfo = ref(loadStored())

function loadStored() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...DEFAULT_INFO }
    const v = JSON.parse(raw)
    return {
      name: (v?.name || DEFAULT_INFO.name).trim(),
      subtitle: (v?.subtitle || DEFAULT_INFO.subtitle).trim(),
      generalInfo: (v?.generalInfo || DEFAULT_INFO.generalInfo).trim(),
    }
  } catch {
    return { ...DEFAULT_INFO }
  }
}

function normalize(next) {
  return {
    name: (next?.name || '').trim() || DEFAULT_INFO.name,
    subtitle: (next?.subtitle || '').trim() || DEFAULT_INFO.subtitle,
    generalInfo: (next?.generalInfo || '').trim() || DEFAULT_INFO.generalInfo,
  }
}

export function useStoreInfo() {
  function update(next) {
    const clean = normalize(next)
    storeInfo.value = clean
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(clean))
    } catch {
      // ignore
    }
  }

  function reset() {
    storeInfo.value = { ...DEFAULT_INFO }
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // ignore
    }
  }

  return {
    storeInfo,
    updateStoreInfo: update,
    resetStoreInfo: reset,
  }
}
