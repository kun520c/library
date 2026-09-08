<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BookCover from '@/components/BookCover.vue'
import BookFormDialog from '@/components/BookFormDialog.vue'
import ContentState from '@/components/ContentState.vue'
import PageIntro from '@/components/PageIntro.vue'
import { booksApi } from '@/api/books'
import { categoriesApi } from '@/api/categories'
import { formatPrice } from '@/utils/format'
import type { Book, BookCreatePayload, BookUpdatePayload, Category } from '@/types'

const books = ref<Book[]>([]); const categories = ref<Category[]>([]); const total = ref(0); const loading = ref(true); const error = ref(false)
const query = reactive({ title: '', author: '', isbn: '', categoryId: undefined as number | undefined, page: 1, size: 10 })
const formOpen = ref(false); const editing = ref<Book | null>(null); const saving = ref(false)
const stockOpen = ref(false); const stockBook = ref<Book | null>(null); const delta = ref(1); const stockSaving = ref(false)
const expectedStock = computed(() => (stockBook.value?.stock || 0) + delta.value)
function categoryName(id: number | null) { return categories.value.find((item) => item.id === id)?.name || '未分类' }
async function load() { loading.value = true; error.value = false; try { const [page, list] = await Promise.all([booksApi.list(query), categoriesApi.list()]); books.value = page.list; total.value = page.total; categories.value = list } catch { error.value = true } finally { loading.value = false } }
function search() { query.page = 1; load() }
function reset() { Object.assign(query, { title: '', author: '', isbn: '', categoryId: undefined, page: 1 }); load() }
function openCreate() { editing.value = null; formOpen.value = true }
function openEdit(book: Book) { editing.value = book; formOpen.value = true }
async function saveBook(payload: BookCreatePayload | BookUpdatePayload) { saving.value = true; try { if (editing.value) await booksApi.update(editing.value.id, payload as BookUpdatePayload); else await booksApi.create(payload as BookCreatePayload); ElMessage.success(editing.value ? '图书信息已更新' : '新书已录入'); formOpen.value = false; await load() } catch { /* The shared HTTP client already presents a user-facing error. */ } finally { saving.value = false } }
function openStock(book: Book) { stockBook.value = book; delta.value = 1; stockOpen.value = true }
async function saveStock() { if (!stockBook.value || delta.value === 0 || expectedStock.value < 0) return; stockSaving.value = true; try { await booksApi.adjustStock(stockBook.value.id, delta.value); ElMessage.success('库存已按增量调整'); stockOpen.value = false; await load() } catch { /* The shared HTTP client already presents a user-facing error. */ } finally { stockSaving.value = false } }
async function remove(book: Book) { try { await ElMessageBox.confirm(`确认删除《${book.title}》？存在未归还记录时后端会拒绝删除。`, '删除图书', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }) } catch { return }; try { await booksApi.remove(book.id); ElMessage.success('图书已删除'); await load() } catch { /* The shared HTTP client already presents a user-facing error. */ } }
onMounted(load)
</script>

<template><section><PageIntro eyebrow="Catalogue operations" title="图书管理" description="维护书目基本信息；库存只通过正负增量调整，不用旧表单覆盖当前数量。"><template #actions><el-button type="primary" @click="openCreate">录入新书</el-button></template></PageIntro>
  <form class="filter-bar" @submit.prevent="search"><div class="filter-field filter-field--wide"><label>书名</label><el-input v-model="query.title" clearable placeholder="搜索书名" /></div><div class="filter-field"><label>作者</label><el-input v-model="query.author" clearable /></div><div class="filter-field"><label>ISBN</label><el-input v-model="query.isbn" clearable /></div><div class="filter-field"><label>分类</label><el-select v-model="query.categoryId" clearable placeholder="全部分类"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></div><div class="filter-actions"><el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></div></form>
  <ContentState :loading="loading" :error="error" :empty="!books.length" empty-text="没有匹配的图书" @retry="load"><div class="data-section"><el-table :data="books"><el-table-column label="图书" min-width="245"><template #default="{ row }"><div class="table-book"><BookCover :title="row.title" :author="row.author" :book-id="row.id" :category-id="row.categoryId" size="small" /><div><b>{{ row.title }}</b><small>{{ row.author }}</small></div></div></template></el-table-column><el-table-column label="分类" min-width="100"><template #default="{ row }">{{ categoryName(row.categoryId) }}</template></el-table-column><el-table-column label="ISBN" prop="isbn" min-width="145" /><el-table-column label="价格" width="100"><template #default="{ row }">{{ formatPrice(row.price) }}</template></el-table-column><el-table-column label="库存" width="80"><template #default="{ row }"><span class="stock-copy" :class="{ out: row.stock === 0 }">{{ row.stock }}</span></template></el-table-column><el-table-column label="操作" min-width="210" align="right"><template #default="{ row }"><div class="row-actions"><button @click="$router.push(`/library/${row.id}`)">查看</button><button @click="openEdit(row)">编辑</button><button @click="openStock(row)">调库存</button><button class="danger" @click="remove(row)">删除</button></div></template></el-table-column></el-table></div><div class="pagination-row"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div></ContentState>
  <BookFormDialog v-model="formOpen" :book="editing" :categories="categories" :loading="saving" @submit="saveBook" />
  <el-dialog v-model="stockOpen" title="按增量调整库存" width="430px"><p class="dialog-note">输入正数增加库存，输入负数减少库存。不会设置绝对库存值。</p><el-form label-position="top"><el-form-item label="调整数量"><el-input-number v-model="delta" :min="-2147483647" :max="2147483647" :precision="0" style="width:100%" /></el-form-item></el-form><div class="stock-preview"><div><span>当前库存</span><b>{{ stockBook?.stock }}</b></div><strong>{{ delta >= 0 ? '+' : '−' }}</strong><div><span>预计库存</span><b :style="{ color: expectedStock < 0 ? 'var(--brick)' : 'inherit' }">{{ expectedStock }}</b></div></div><template #footer><el-button @click="stockOpen = false">取消</el-button><el-button type="primary" :disabled="delta === 0 || expectedStock < 0" :loading="stockSaving" @click="saveStock">确认调整</el-button></template></el-dialog>
</section></template>
