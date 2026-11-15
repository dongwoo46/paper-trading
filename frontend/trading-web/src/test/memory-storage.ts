import { vi } from "vitest"

/**
 * localStorage 를 메모리 구현으로 바꿔 끼운다.
 * Node 25+ 는 `--localstorage-file` 없이는 전역 localStorage 가 비어 있어 jsdom 의 것을 가린다.
 */
export function stubLocalStorage(): Storage {
  const data = new Map<string, string>()
  const storage: Storage = {
    get length() {
      return data.size
    },
    clear: () => data.clear(),
    getItem: (key) => data.get(key) ?? null,
    key: (index) => Array.from(data.keys())[index] ?? null,
    removeItem: (key) => {
      data.delete(key)
    },
    setItem: (key, value) => {
      data.set(key, String(value))
    },
  }
  vi.stubGlobal("localStorage", storage)
  return storage
}
