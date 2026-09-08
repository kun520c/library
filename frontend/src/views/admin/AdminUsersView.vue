<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import ContentState from '@/components/ContentState.vue'
import PageIntro from '@/components/PageIntro.vue'
import { adminApi } from '@/api/admin'
import { formatDateTime } from '@/utils/format'
import type { AdminUser } from '@/types'

const users = ref<AdminUser[]>([]); const total = ref(0); const loading = ref(true); const error = ref(false)
const query = reactive({ username: '', account: '', page: 1, size: 10 })
async function load() { loading.value = true; error.value = false; try { const page = await adminApi.users(query); users.value = page.list; total.value = page.total } catch { error.value = true } finally { loading.value = false } }
function search() { query.page = 1; load() }
function reset() { Object.assign(query, { username: '', account: '', page: 1 }); load() }
onMounted(load)
</script>

<template><section><PageIntro eyebrow="Registered readers" title="用户查询" description="只查看注册用户的公开管理信息；此处没有角色修改、封禁或删除操作。" /><form class="filter-bar" @submit.prevent="search"><div class="filter-field filter-field--wide"><label>用户名</label><el-input v-model="query.username" clearable placeholder="模糊查询" /></div><div class="filter-field filter-field--wide"><label>账号</label><el-input v-model="query.account" clearable placeholder="模糊查询" /></div><div class="filter-actions"><el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></div></form><ContentState :loading="loading" :error="error" :empty="!users.length" empty-text="没有匹配的用户" @retry="load"><div class="data-section"><el-table :data="users"><el-table-column label="ID" prop="id" width="90" /><el-table-column label="用户名" min-width="180"><template #default="{ row }"><strong class="serif">{{ row.username }}</strong></template></el-table-column><el-table-column label="账号" prop="account" min-width="180" /><el-table-column label="角色" width="110"><template #default="{ row }"><span class="status-label" :class="row.role === 'ADMIN' ? 'status-label--borrowed' : 'status-label--returned'">{{ row.role }}</span></template></el-table-column><el-table-column label="注册时间" min-width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column></el-table></div><div class="pagination-row"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div></ContentState></section></template>
