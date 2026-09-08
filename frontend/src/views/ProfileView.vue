<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageIntro from '@/components/PageIntro.vue'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const dialogOpen = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ username: '' })
const rules: FormRules = { username: [{ required: true, message: '请输入用户名' }, { max: 50, message: '用户名最多 50 个字符' }] }
function openEdit() { form.username = auth.currentUser?.username || ''; dialogOpen.value = true }
async function save() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try { await authApi.updateProfile(form.username.trim()); await auth.refreshUser(); dialogOpen.value = false; ElMessage.success('用户名已更新') }
  catch { /* The shared HTTP client already presents a user-facing error. */ }
  finally { saving.value = false }
}
</script>

<template>
  <section><PageIntro eyebrow="Reader profile" title="个人资料" description="账号和角色由系统维护；你可以更新借阅记录中展示的用户名。" />
    <div class="profile-sheet"><div class="profile-row"><span>用户名</span><strong>{{ auth.currentUser?.username }}</strong><el-button text @click="openEdit">修改</el-button></div><div class="profile-row"><span>账号</span><strong class="mono">{{ auth.currentUser?.account }}</strong><span /></div><div class="profile-row"><span>角色</span><strong>{{ auth.currentUser?.role === 'ADMIN' ? '管理员' : '普通读者' }}</strong><span /></div></div>
    <el-dialog v-model="dialogOpen" title="修改用户名" width="420px"><p class="dialog-note">修改会立即写入资料；当前 JWT 中的旧用户名不会主动刷新。</p><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="新用户名" prop="username"><el-input v-model="form.username" maxlength="50" show-word-limit @keyup.enter="save" /></el-form-item></el-form><template #footer><el-button @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog>
  </section>
</template>
