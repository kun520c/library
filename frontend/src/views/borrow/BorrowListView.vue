<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BorrowStatusTag from '@/components/BorrowStatusTag.vue'
import ContentState from '@/components/ContentState.vue'
import PageIntro from '@/components/PageIntro.vue'
import { borrowApi } from '@/api/borrow'
import { formatDateTime } from '@/utils/format'
import type { BorrowRecord, BorrowStatus } from '@/types'

const records = ref<BorrowRecord[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref(false)
const returningId = ref<number | null>(null)
const query = reactive({ status: undefined as BorrowStatus | undefined, bookTitle: '', page: 1, size: 10 })
const tabs: { label: string; value?: BorrowStatus }[] = [{ label: '全部' }, { label: '借出中', value: 'BORROWED' }, { label: '已归还', value: 'RETURNED' }]

async function load() {
  loading.value = true; error.value = false
  try { const page = await borrowApi.mine(query); records.value = page.list; total.value = page.total }
  catch { error.value = true } finally { loading.value = false }
}
function switchStatus(status?: BorrowStatus) { query.status = status; query.page = 1; load() }
function search() { query.page = 1; load() }
async function returnBook(record: BorrowRecord) {
  try { await ElMessageBox.confirm(`确认归还《${record.bookTitle}》？`, '确认还书', { confirmButtonText: '确认归还', cancelButtonText: '取消' }) }
  catch { return }
  returningId.value = record.recordId
  try { await borrowApi.returnBook(record.recordId); ElMessage.success('图书已归还'); await load() }
  catch { /* The shared HTTP client already presents a user-facing error. */ }
  finally { returningId.value = null }
}
onMounted(load)
</script>

<template>
  <section><PageIntro eyebrow="Borrowing ledger" title="我的借阅" description="借出、到期与归还时间都来自真实借阅记录；逾期状态会随时间更新。" />
    <div class="tabs-line"><button v-for="tab in tabs" :key="tab.label" :class="{ active: query.status === tab.value }" @click="switchStatus(tab.value)">{{ tab.label }}</button></div>
    <form class="filter-bar" @submit.prevent="search"><div class="filter-field filter-field--wide"><label>图书名称</label><el-input v-model="query.bookTitle" clearable placeholder="在借阅记录中搜索" /></div><div class="filter-actions"><el-button type="primary" native-type="submit">查询</el-button><el-button @click="query.bookTitle = ''; search()">清除</el-button></div></form>
    <ContentState :loading="loading" :error="error" :empty="!records.length" empty-text="暂无借阅记录" @retry="load">
      <div class="data-section"><el-table :data="records" style="width:100%"><el-table-column label="图书" min-width="210"><template #default="{ row }"><div class="table-book"><div><b>{{ row.bookTitle }}</b><small>记录 #{{ row.recordId }}</small></div></div></template></el-table-column><el-table-column label="借阅时间" min-width="150"><template #default="{ row }">{{ formatDateTime(row.borrowTime) }}</template></el-table-column><el-table-column label="应还时间" min-width="150"><template #default="{ row }">{{ formatDateTime(row.dueTime) }}</template></el-table-column><el-table-column label="归还时间" min-width="150"><template #default="{ row }">{{ formatDateTime(row.returnTime) }}</template></el-table-column><el-table-column label="状态" width="90"><template #default="{ row }"><BorrowStatusTag :record="row" /></template></el-table-column><el-table-column label="操作" width="135" align="right"><template #default="{ row }"><div class="row-actions"><button @click="$router.push(`/borrow/${row.recordId}`)">详情</button><button v-if="row.status === 'BORROWED'" :disabled="returningId === row.recordId" @click="returnBook(row)">归还</button></div></template></el-table-column></el-table></div>
      <div class="pagination-row"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </ContentState>
  </section>
</template>
