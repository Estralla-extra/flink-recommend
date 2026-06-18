import { ref, onMounted, onUnmounted } from 'vue'

export function usePolling<T>(
  fetcher: () => Promise<T>,
  intervalMs: number,
  onError?: (err: unknown) => void
) {
  const data = ref<T | null>(null)
  const loading = ref(true)
  const error = ref<string | null>(null)
  let timer: ReturnType<typeof setInterval> | null = null

  async function execute() {
    try {
      error.value = null
      data.value = await fetcher()
    } catch (err) {
      error.value = String(err)
      onError?.(err)
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    execute()
    timer = setInterval(execute, intervalMs)
  })

  onUnmounted(() => {
    if (timer) clearInterval(timer)
  })

  return { data, loading, error, refresh: execute }
}
