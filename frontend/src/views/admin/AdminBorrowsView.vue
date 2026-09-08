<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BorrowStatusTag from '@/components/BorrowStatusTag.vue'
import ContentState from '@/components/ContentState.vue'
import PageIntro from '@/components/PageIntro.vue'
import { adminApi } from '@/api/admin'
import { borrowApi } from '@/api/borrow'
import { formatDateTime } from '@/utils/format'
import type { BorrowRecord, BorrowStatus } from '@/types'

const records = ref<BorrowRecord[]>([]); const total = ref(0); const loading = ref(true); const error = ref(false); const returningId = ref<number | null>(null)
const query = reactive({ status: undefined as BorrowStatus | undefined, bookTitle: '', page: 1, size: 10 })
async function load() { loading.value = true; error.value = false; try { const page = await adminApi.borrows(query); records.value = page.list; total.value = page.total } catch { error.value = true } finally { loading.value = false } }
function search() { query.page = 1; load() }
function reset() { Object.assign(query, { status: undefined, bookTitle: '', page: 1 }); load() }
async function returnBook(record: BorrowRecord) { try { await ElMessageBox.confirm(`确认代 ${record.username} 归还《${record.bookTitle}》？`, '管理员代还', { confirmButtonText: '确认归还', cancelButtonText: '取消' }) } catch { return }; returningId.value = record.recordId; try { await borrowApi.returnBook(record.recordId); ElMessage.success('借阅记录已归还'); await load() } catch { /* The shared HTTP client already presents a user-facing error. */ } finally { returningId.value = null } }
onMounted(load)
</script>

<template><section><PageIntro eyebrow="Borrowing operations" title="借阅管理" description="查看全体借阅记录，并在需要时为读者代还；借期与状态不可人工修改。" /><form class="filter-bar" @submit.prevent="search"><div class="filter-field filter-field--wide"><label>图书名称</label><el-input v-model="query.bookTitle" clearable placeholder="模糊查询" /></div><div class="filter-field"><label>状态</label><el-select v-model="query.status" clearable placeholder="全部状态"><el-option label="借出中" value="BORROWED" /><el-option label="已归还" value="RETURNED" /></el-select></div><div class="filter-actions"><el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></div></form><ContentState :loading="loading" :error="error" :empty="!records.length" empty-text="没有匹配的借阅记录" @retry="load"><div class="data-section"><el-table :data="records"><el-table-column label="借阅人" min-width="130"><template #default="{ row }"><b>{{ row.username }}</b><br><small class="text-muted">用户 #{{ row.userId }}</small></template></el-table-column><el-table-column label="图书" prop="bookTitle" min-width="190" /><el-table-column label="借阅时间" min-width="150"><template #default="{ row }">{{ formatDateTime(row.borrowTime) }}</template></el-table-column><el-table-column label="应还时间" min-width="150"><template #default="{ row }">{{ formatDateTime(row.dueTime) }}</template></el-table-column><el-table-column label="归还时间" min-width="150"><template #default="{ row }">{{ formatDateTime(row.returnTime) }}</template></el-table-column><el-table-column label="状态" width="90"><template #default="{ row }"><BorrowStatusTag :record="row" /></template></el-table-column><el-table-column label="操作" width="140" align="right"><template #default="{ row }"><div class="row-actions"><button @click="$router.push(`/borrow/${row.recordId}`)">详情</button><button v-if="row.status === 'BORROWED'" :disabled="returningId === row.recordId" @click="returnBook(row)">代还</button></div></template></el-table-column></el-table></div><div class="pagination-row"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div></ContentState></section></template>
