<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import PageIntro from '@/components/PageIntro.vue'
import ContentState from '@/components/ContentState.vue'
import BorrowStatusTag from '@/components/BorrowStatusTag.vue'
import { booksApi } from '@/api/books'
import { categoriesApi } from '@/api/categories'
import { adminApi } from '@/api/admin'
import { formatDateTime } from '@/utils/format'
import type { BorrowRecord } from '@/types'

const totals = reactive({ books: 0, users: 0, borrowed: 0, returned: 0, categories: 0 })
const recent = ref<BorrowRecord[]>([])
const loading = ref(true)
const error = ref(false)
async function load() {
  loading.value = true; error.value = false
  try {
    const [books, users, borrowed, returned, categories, recentPage] = await Promise.all([
      booksApi.list({ page: 1, size: 1 }), adminApi.users({ page: 1, size: 1 }),
      adminApi.borrows({ status: 'BORROWED', page: 1, size: 1 }), adminApi.borrows({ status: 'RETURNED', page: 1, size: 1 }),
      categoriesApi.list(), adminApi.borrows({ page: 1, size: 5 }),
    ])
    Object.assign(totals, { books: books.total, users: users.total, borrowed: borrowed.total, returned: returned.total, categories: categories.length })
    recent.value = recentPage.list
  } catch { error.value = true } finally { loading.value = false }
}
onMounted(load)
</script>

<template><section><PageIntro eyebrow="Administration" title="管理概览" description="只汇总后端能够实时提供的数据，不推测趋势或增长率。" />
  <ContentState :loading="loading" :error="error" @retry="load"><div class="summary-strip"><div class="summary-item"><span>有效图书</span><strong>{{ totals.books }}</strong></div><div class="summary-item"><span>注册用户</span><strong>{{ totals.users }}</strong></div><div class="summary-item"><span>借出中</span><strong>{{ totals.borrowed }}</strong></div><div class="summary-item"><span>已归还记录</span><strong>{{ totals.returned }}</strong></div></div>
    <div class="overview-grid"><section><div class="section-heading"><h3>最近借阅</h3><RouterLink class="text-action" to="/admin/borrows">查看全部</RouterLink></div><div class="data-section"><el-table :data="recent"><el-table-column label="借阅人" prop="username" min-width="100" /><el-table-column label="图书" prop="bookTitle" min-width="170" /><el-table-column label="借阅时间" min-width="150"><template #default="{ row }">{{ formatDateTime(row.borrowTime) }}</template></el-table-column><el-table-column label="状态" width="90"><template #default="{ row }"><BorrowStatusTag :record="row" /></template></el-table-column></el-table></div></section>
      <aside><div class="section-heading"><h3>管理入口</h3><span>{{ totals.categories }} 个分类</span></div><div class="quick-links"><RouterLink to="/admin/books"><span>维护图书与库存</span><el-icon><ArrowRight /></el-icon></RouterLink><RouterLink to="/admin/categories"><span>整理分类目录</span><el-icon><ArrowRight /></el-icon></RouterLink><RouterLink to="/admin/users"><span>查询注册用户</span><el-icon><ArrowRight /></el-icon></RouterLink><RouterLink to="/admin/borrows"><span>处理借阅记录</span><el-icon><ArrowRight /></el-icon></RouterLink></div></aside></div>
  </ContentState></section></template>
