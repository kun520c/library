<script setup lang="ts">
import type { BorrowRecord } from '@/types'

defineProps<{ record: BorrowRecord }>()
</script>

<template>
  <span v-if="record.overdue" class="status-label status-label--overdue">已逾期</span>
  <span v-else-if="record.status === 'BORROWED'" class="status-label status-label--borrowed">借出中</span>
  <span v-else class="status-label status-label--returned">已归还</span>
</template>
