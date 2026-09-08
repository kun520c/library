<script setup lang="ts">
import { Warning } from '@element-plus/icons-vue'

withDefaults(defineProps<{ loading?: boolean; error?: boolean; empty?: boolean; emptyText?: string }>(), {
  loading: false, error: false, empty: false, emptyText: '暂无数据',
})
defineEmits<{ retry: [] }>()
</script>

<template>
  <div v-if="loading" class="skeleton-lines" aria-label="正在加载">
    <span v-for="line in 5" :key="line" :style="{ width: `${92 - line * 7}%` }" />
  </div>
  <div v-else-if="error" class="content-state">
    <el-icon><Warning /></el-icon><strong>无法连接到服务器</strong><p>数据暂时没有取回来，请稍后重试。</p>
    <el-button @click="$emit('retry')">重新加载</el-button>
  </div>
  <div v-else-if="empty" class="content-state content-state--empty"><strong>{{ emptyText }}</strong><p>调整筛选条件后再试试。</p></div>
  <slot v-else />
</template>
