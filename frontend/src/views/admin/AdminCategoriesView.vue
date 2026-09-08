<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import ContentState from '@/components/ContentState.vue'
import PageIntro from '@/components/PageIntro.vue'
import { categoriesApi } from '@/api/categories'
import { formatDateTime } from '@/utils/format'
import type { Category } from '@/types'

const categories = ref<Category[]>([]); const loading = ref(true); const error = ref(false); const dialogOpen = ref(false); const saving = ref(false); const editing = ref<Category | null>(null)
const formRef = ref<FormInstance>(); const form = reactive({ name: '' }); const rules: FormRules = { name: [{ required: true, message: '请输入分类名称' }, { max: 50, message: '分类名称最多 50 个字符' }] }
async function load() { loading.value = true; error.value = false; try { categories.value = await categoriesApi.list() } catch { error.value = true } finally { loading.value = false } }
function openCreate() { editing.value = null; form.name = ''; dialogOpen.value = true }
function openEdit(item: Category) { editing.value = item; form.name = item.name; dialogOpen.value = true }
async function save() { if (!(await formRef.value?.validate().catch(() => false))) return; saving.value = true; try { if (editing.value) await categoriesApi.update(editing.value.id, form.name.trim()); else await categoriesApi.create(form.name.trim()); ElMessage.success(editing.value ? '分类已重命名' : '分类已新增'); dialogOpen.value = false; await load() } catch { /* The shared HTTP client already presents a user-facing error. */ } finally { saving.value = false } }
async function remove(item: Category) { try { await ElMessageBox.confirm(`确认删除分类“${item.name}”？仍被有效图书使用时后端会拒绝。`, '删除分类', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }) } catch { return }; try { await categoriesApi.remove(item.id); ElMessage.success('分类已删除'); await load() } catch { /* The shared HTTP client already presents a user-facing error. */ } }
onMounted(load)
</script>

<template><section><PageIntro eyebrow="Taxonomy" title="分类管理" description="分类用于整理书库目录；被有效图书使用的分类不能删除。"><template #actions><el-button type="primary" @click="openCreate">新增分类</el-button></template></PageIntro><ContentState :loading="loading" :error="error" :empty="!categories.length" empty-text="暂无分类" @retry="load"><div class="data-section"><el-table :data="categories"><el-table-column label="ID" prop="id" width="90" /><el-table-column label="分类名称" min-width="220"><template #default="{ row }"><strong class="serif">{{ row.name }}</strong></template></el-table-column><el-table-column label="创建时间" min-width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column><el-table-column label="最近更新" min-width="180"><template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template></el-table-column><el-table-column label="操作" width="130" align="right"><template #default="{ row }"><div class="row-actions"><button @click="openEdit(row)">重命名</button><button class="danger" @click="remove(row)">删除</button></div></template></el-table-column></el-table></div></ContentState><el-dialog v-model="dialogOpen" :title="editing ? '重命名分类' : '新增分类'" width="410px"><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="分类名称" prop="name"><el-input v-model="form.name" maxlength="50" show-word-limit @keyup.enter="save" /></el-form-item></el-form><template #footer><el-button @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog></section></template>
