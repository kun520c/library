<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Book, BookCreatePayload, BookUpdatePayload, Category } from '@/types'

const props = defineProps<{ modelValue: boolean; book?: Book | null; categories: Category[]; loading?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; submit: [payload: BookCreatePayload | BookUpdatePayload] }>()
const visible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
const formRef = ref<FormInstance>()
const form = reactive<BookCreatePayload>({ title: '', author: '', isbn: '', price: 0, stock: 0, categoryId: undefined })
const rules: FormRules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }, { max: 200, message: '最多 200 个字符' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }, { max: 100, message: '最多 100 个字符' }],
  isbn: [{ required: true, message: '请输入 ISBN', trigger: 'blur' }, { max: 32, message: '最多 32 个字符' }],
  price: [{ required: true, message: '请输入价格' }],
  stock: [{ required: true, message: '请输入初始库存' }],
}

watch(() => [props.modelValue, props.book] as const, () => {
  if (!props.modelValue) return
  Object.assign(form, props.book ? {
    title: props.book.title, author: props.book.author, isbn: props.book.isbn,
    price: props.book.price, stock: props.book.stock, categoryId: props.book.categoryId || undefined,
  } : { title: '', author: '', isbn: '', price: 0, stock: 0, categoryId: undefined })
}, { immediate: true })

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const common = { title: form.title.trim(), author: form.author.trim(), isbn: form.isbn.trim(), price: Number(form.price), categoryId: form.categoryId }
  emit('submit', props.book ? common : { ...common, stock: Number(form.stock) })
}
</script>

<template>
  <el-dialog v-model="visible" :title="book ? '编辑图书信息' : '录入新书'" width="560px" destroy-on-close>
    <p class="dialog-note">{{ book ? '库存不在此处修改，请使用“调整库存”。' : '录入书目与首次入库数量。' }}</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <div class="form-grid">
        <el-form-item label="书名" prop="title" class="span-2"><el-input v-model="form.title" maxlength="200" /></el-form-item>
        <el-form-item label="作者" prop="author"><el-input v-model="form.author" maxlength="100" /></el-form-item>
        <el-form-item label="ISBN" prop="isbn"><el-input v-model="form.isbn" maxlength="32" /></el-form-item>
        <el-form-item label="价格" prop="price"><el-input-number v-model="form.price" :min="0" :precision="2" :controls="false" /></el-form-item>
        <el-form-item v-if="!book" label="初始库存" prop="stock"><el-input-number v-model="form.stock" :min="0" :precision="0" /></el-form-item>
        <el-form-item label="分类"><el-select v-model="form.categoryId" clearable placeholder="未分类"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
      </div>
    </el-form>
    <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="loading" @click="submit">{{ book ? '保存修改' : '确认录入' }}</el-button></template>
  </el-dialog>
</template>
