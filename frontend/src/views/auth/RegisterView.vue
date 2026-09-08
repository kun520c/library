<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { authApi } from '@/api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', account: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { max: 50, message: '用户名最多 50 个字符' }],
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 32, message: '账号长度为 4–32 个字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._-]+$/, message: '仅支持字母、数字、点、下划线和连字符', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 72, message: '密码长度为 6–72 个字符' }],
}

async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  loading.value = true
  try {
    await authApi.register(form.username.trim(), form.account.trim(), form.password)
    ElMessage.success('注册成功，请登录')
    await router.replace('/login')
  } catch {
    // The shared HTTP client already presents a user-facing error.
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout eyebrow="New reader" title="登记读者账号" description="创建普通读者账号。管理员权限由系统维护者设置。">
    <el-form ref="formRef" class="auth-form" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
      <el-form-item label="用户名" prop="username"><el-input v-model="form.username" size="large" maxlength="50" autocomplete="name" placeholder="用于借阅记录展示" /></el-form-item>
      <el-form-item label="账号" prop="account"><el-input v-model="form.account" size="large" maxlength="32" autocomplete="username" placeholder="4–32 位字母或数字" /></el-form-item>
      <el-form-item label="密码" prop="password"><el-input v-model="form.password" size="large" maxlength="72" type="password" show-password autocomplete="new-password" placeholder="至少 6 个字符" /></el-form-item>
      <el-button class="auth-submit" type="primary" size="large" :loading="loading" @click="submit">完成注册</el-button>
    </el-form>
    <p class="auth-switch">已有账号？<RouterLink to="/login">返回登录</RouterLink></p>
  </AuthLayout>
</template>
