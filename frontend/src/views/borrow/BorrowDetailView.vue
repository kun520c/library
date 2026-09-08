<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import BorrowStatusTag from '@/components/BorrowStatusTag.vue'
import ContentState from '@/components/ContentState.vue'
import BookCover from '@/components/BookCover.vue'
import { borrowApi } from '@/api/borrow'
import { formatDateTime } from '@/utils/format'
import type { BorrowRecord } from '@/types'

const route = useRoute(); const router = useRouter()
const record = ref<BorrowRecord | null>(null); const loading = ref(true); const error = ref(false); const returning = ref(false)
const id = computed(() => Number(route.params.id))
async function load() { loading.value = true; error.value = false; try { record.value = await borrowApi.detail(id.value) } catch { error.value = true } finally { loading.value = false } }
async function returnBook() { if (!record.value) return; try { await ElMessageBox.confirm(`确认归还《${record.value.bookTitle}》？`, '确认还书', { confirmButtonText: '确认归还', cancelButtonText: '取消' }) } catch { return }; returning.value = true; try { await borrowApi.returnBook(record.value.recordId); ElMessage.success('图书已归还'); await load() } catch { /* The shared HTTP client already presents a user-facing error. */ } finally { returning.value = false } }
onMounted(load)
</script>

<template><section class="borrow-detail"><el-button text :icon="ArrowLeft" @click="router.back()">返回借阅列表</el-button><ContentState :loading="loading" :error="error" :empty="!record" empty-text="借阅记录不存在" @retry="load"><template v-if="record"><div class="record-heading"><div><p>借阅记录 #{{ record.recordId }}</p><h2>{{ record.bookTitle }}</h2><span class="text-muted">借阅人：{{ record.username }}</span></div><BorrowStatusTag :record="record" /></div><div class="record-timeline"><div class="timeline-point"><i class="timeline-dot" /><span>借阅时间</span><b>{{ formatDateTime(record.borrowTime) }}</b></div><div class="timeline-point"><i class="timeline-dot" /><span>应还时间</span><b>{{ formatDateTime(record.dueTime) }}</b></div><div class="timeline-point" :class="{ muted: !record.returnTime }"><i class="timeline-dot" /><span>实际归还</span><b>{{ formatDateTime(record.returnTime) }}</b></div></div><div style="display:flex;gap:30px;align-items:flex-start;border-top:1px solid var(--line);padding-top:26px"><BookCover :title="record.bookTitle" :author="record.username" :book-id="record.bookId" size="small" /><dl class="metadata-list" style="flex:1"><div><dt>图书编号</dt><dd>#{{ record.bookId }}</dd></div><div><dt>记录状态</dt><dd>{{ record.overdue ? '借出中，已超过应还时间' : record.status === 'BORROWED' ? '借出中，尚未逾期' : '已经归还' }}</dd></div></dl></div><div v-if="record.status === 'BORROWED'" class="borrow-callout"><div><strong>{{ record.overdue ? '这本书已经逾期' : '这本书仍在借阅中' }}</strong><p>归还后库存会由后端事务原子恢复。</p></div><el-button type="primary" :loading="returning" @click="returnBook">确认归还</el-button></div></template></ContentState></section></template>
