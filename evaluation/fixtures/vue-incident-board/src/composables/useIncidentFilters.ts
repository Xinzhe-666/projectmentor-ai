import { computed, ref } from 'vue'

export function useIncidentFilters() {
  const selectedSeverity = ref('all')
  const query = ref('')
  const activeFilterCount = computed(() => Number(selectedSeverity.value !== 'all') + Number(Boolean(query.value)))
  return { selectedSeverity, query, activeFilterCount }
}
