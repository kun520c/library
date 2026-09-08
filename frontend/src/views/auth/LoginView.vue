<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ account: '', password: '' })
const rules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 32, message: '账号长度为 4–32 个字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._-]+$/, message: '账号格式不正确', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { max: 72, message: '密码最多 72 个字符' }],
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  loading.value = true
  try {
    await auth.login(form.account.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    await router.replace(redirect || (auth.isAdmin ? '/admin' : '/library'))
  } catch {
    // The shared HTTP client already presents a user-facing error.
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout eyebrow="Member access" title="登录书室" description="使用你的账号进入书库，查看库存与借阅记录。">
    <el-form ref="formRef" class="auth-form" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
      <el-form-item label="账号" prop="account"><el-input v-model="form.account" size="large" maxlength="32" autocomplete="username" placeholder="4–32 位字母或数字" /></el-form-item>
      <el-form-item label="密码" prop="password"><el-input v-model="form.password" size="large" maxlength="72" type="password" show-password autocomplete="current-password" placeholder="输入密码" /></el-form-item>
      <el-button class="auth-submit" type="primary" size="large" :loading="loading" @click="submit">登录</el-button>
    </el-form>
    <p class="auth-switch">还没有账号？<RouterLink to="/register">注册读者账号</RouterLink></p>
  </AuthLayout>
</template>
