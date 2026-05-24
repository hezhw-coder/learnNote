import { ref } from 'vue';

export function useAsyncState() {
  const loading = ref(false);

  async function run<T>(handler: () => Promise<T>) {
    loading.value = true;
    try {
      return await handler();
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
    run,
  };
}
