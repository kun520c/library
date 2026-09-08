<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import BookCover from '@/components/BookCover.vue'
import ContentState from '@/components/ContentState.vue'
import { booksApi } from '@/api/books'
import { borrowApi } from '@/api/borrow'
import { categoriesApi } from '@/api/categories'
import { formatPrice } from '@/utils/format'
import type { Book, Category } from '@/types'

const route = useRoute()
const book = ref<Book | null>(null)
const categories = ref<Category[]>([])
const loading = ref(true)
const error = ref(false)
const borrowing = ref(false)
const id = computed(() => Number(route.params.id))
const category = computed(() => categories.value.find((item) => item.id === book.value?.categoryId)?.name || '未分类')

async function load() {
  loading.value = true; error.value = false
  try { [book.value, categories.value] = await Promise.all([booksApi.detail(id.value), categoriesApi.list()]) }
  catch { error.value = true } finally { loading.value = false }
}
async function borrow() {
  if (!book.value) return
  try { await ElMessageBox.confirm(`确认借阅《${book.value.title}》？借期为 30 天。`, '确认借阅', { confirmButtonText: '确认借阅', cancelButtonText: '取消' }) }
  catch { return }
  borrowing.value = true
  try { await borrowApi.borrow(book.value.id); ElMessage.success('借阅成功'); await load() }
  catch { /* The shared HTTP client already presents a user-facing error. */ }
  finally { borrowing.value = false }
}
onMounted(load)
</script>

<template>
  <section>
    <el-button text :icon="ArrowLeft" @click="$router.back()">返回书库</el-button>
    <ContentState :loading="loading" :error="error" :empty="!book" empty-text="图书不存在" @retry="load">
      <article v-if="book" class="book-detail">
        <div class="book-detail__cover"><BookCover :title="book.title" :author="book.author" :book-id="book.id" :category-id="book.categoryId" size="large" /></div>
        <div class="book-detail__content"><span class="book-detail__category">{{ category }}</span><h2>{{ book.title }}</h2><p class="book-detail__author">{{ book.author }}</p>
          <dl class="metadata-list"><div><dt>馆藏编号</dt><dd class="mono">L{{ String(book.id).padStart(4, '0') }}</dd></div><div><dt>ISBN</dt><dd class="mono">{{ book.isbn }}</dd></div><div><dt>价格</dt><dd>{{ formatPrice(book.price) }}</dd></div><div><dt>分类</dt><dd>{{ category }}</dd></div></dl>
          <div class="borrow-callout"><div><strong>{{ book.stock > 0 ? `当前可借 ${book.stock} 本` : '当前暂无库存' }}</strong><p>借期 30 天；到期状态由系统实时判断。</p></div><el-button type="primary" :disabled="book.stock === 0" :loading="borrowing" @click="borrow">借阅此书</el-button></div>
        </div>
      </article>
    </ContentState>
  </section>
</template>
