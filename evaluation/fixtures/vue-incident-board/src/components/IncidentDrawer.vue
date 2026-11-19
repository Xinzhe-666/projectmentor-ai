<script setup lang="ts">
defineProps<{ incidentId: number; open: boolean }>()
defineEmits<{ close: [] }>()
</script>

<template><aside v-if="open" aria-label="Incident details"><button @click="$emit('close')">Close</button></aside></template>
