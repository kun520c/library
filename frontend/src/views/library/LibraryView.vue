<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import BookCover from '@/components/BookCover.vue'
import ContentState from '@/components/ContentState.vue'
import PageIntro from '@/components/PageIntro.vue'
import { booksApi } from '@/api/books'
import { borrowApi } from '@/api/borrow'
import { categoriesApi } from '@/api/categories'
import type { Book, Category } from '@/types'

const books = ref<Book[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref(false)
const borrowingId = ref<number | null>(null)
const query = reactive({ title: '', author: '', isbn: '', categoryId: undefined as number | undefined, page: 1, size: 12 })

function categoryName(id: number | null) {
  return categories.value.find((item) => item.id === id)?.name || '未分类'
}

async function load() {
  loading.value = true
  error.value = false
  try {
    const [bookPage, categoryList] = await Promise.all([booksApi.list(query), categoriesApi.list()])
    books.value = bookPage.list
    total.value = bookPage.total
    categories.value = categoryList
  } catch { error.value = true } finally { loading.value = false }
}

function search() { query.page = 1; load() }
function reset() { Object.assign(query, { title: '', author: '', isbn: '', categoryId: undefined, page: 1 }); load() }
async function borrow(book: Book) {
  try { await ElMessageBox.confirm(`确认借阅《${book.title}》？借期为 30 天。`, '确认借阅', { confirmButtonText: '确认借阅', cancelButtonText: '再看看', type: 'info' }) }
  catch { return }
  borrowingId.value = book.id
  try { await borrowApi.borrow(book.id); ElMessage.success('借阅成功'); await load() }
  catch { /* The shared HTTP client already presents a user-facing error. */ }
  finally { borrowingId.value = null }
}
onMounted(load)
</script>

<template>
  <section>
    <PageIntro eyebrow="Library catalogue" title="书库" description="按书名、作者或分类查找馆藏；每一本封面都由馆藏信息稳定生成。" />
    <form class="filter-bar" @submit.prevent="search">
      <div class="filter-field filter-field--wide"><label>书名</label><el-input v-model="query.title" clearable placeholder="搜索书名" :prefix-icon="Search" /></div>
      <div class="filter-field"><label>作者</label><el-input v-model="query.author" clearable placeholder="作者姓名" /></div>
      <div class="filter-field"><label>ISBN</label><el-input v-model="query.isbn" clearable placeholder="精确查询" /></div>
      <div class="filter-field"><label>分类</label><el-select v-model="query.categoryId" clearable placeholder="全部分类"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></div>
      <div class="filter-actions"><el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></div>
    </form>
    <div class="section-heading"><h3>馆藏目录</h3><span>共 {{ total }} 本</span></div>
    <ContentState :loading="loading" :error="error" :empty="!books.length" empty-text="没有匹配的图书" @retry="load">
      <div class="book-grid">
        <article v-for="book in books" :key="book.id" class="book-item">
          <RouterLink :to="`/library/${book.id}`"><BookCover :title="book.title" :author="book.author" :book-id="book.id" :category-id="book.categoryId" /></RouterLink>
          <div class="book-item__info">
            <span class="book-item__category">{{ categoryName(book.categoryId) }}</span>
            <RouterLink :to="`/library/${book.id}`"><h3>{{ book.title }}</h3></RouterLink>
            <p class="book-item__author">{{ book.author }}</p><span class="book-item__isbn">ISBN {{ book.isbn }}</span>
            <div class="book-item__footer"><span class="stock-copy" :class="{ out: book.stock === 0 }">{{ book.stock > 0 ? `可借 · ${book.stock} 本` : '暂无库存' }}</span><button class="text-action" :disabled="book.stock === 0 || borrowingId === book.id" @click="borrow(book)">{{ borrowingId === book.id ? '处理中…' : '借阅' }}</button></div>
          </div>
        </article>
      </div>
      <div class="pagination-row"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[12, 24, 48]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </ContentState>
  </section>
</template>
