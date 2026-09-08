<script setup lang="ts">
defineProps<{ eyebrow?: string; title: string; description: string }>()
</script>

<template>
  <header class="page-intro">
    <div><p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p><h2>{{ title }}</h2><p>{{ description }}</p></div>
    <div v-if="$slots.actions" class="page-actions"><slot name="actions" /></div>
  </header>
</template>
