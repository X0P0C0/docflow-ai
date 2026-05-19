type StorageLike = Pick<Storage, 'getItem' | 'setItem' | 'removeItem' | 'clear'>

function createMemoryStorage(): StorageLike {
  const store = new Map<string, string>()
  return {
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null
    },
    setItem(key: string, value: string) {
      store.set(key, value)
    },
    removeItem(key: string) {
      store.delete(key)
    },
    clear() {
      store.clear()
    },
  }
}

const memoryLocalStorage = createMemoryStorage()
const memorySessionStorage = createMemoryStorage()

function resolveStorage(kind: 'localStorage' | 'sessionStorage') {
  if (typeof window !== 'undefined' && window[kind]) {
    return window[kind]
  }
  return kind === 'localStorage' ? memoryLocalStorage : memorySessionStorage
}

export function getSafeLocalStorage() {
  return resolveStorage('localStorage')
}

export function getSafeSessionStorage() {
  return resolveStorage('sessionStorage')
}
