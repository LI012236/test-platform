<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <template #header>
        <div style="text-align:center;font-weight:600;font-size:16px">接口测试平台</div>
      </template>

      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-input v-model="loginForm.username" placeholder="用户名" size="large" style="margin-bottom:12px" />
          <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password
                    style="margin-bottom:16px" @keyup.enter="doLogin" />
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">登录</el-button>
          <div class="text-muted" style="text-align:center;margin-top:12px">管理员：admin / admin123</div>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-input v-model="regForm.username" placeholder="用户名（3-20 位字母数字）" size="large" style="margin-bottom:12px" />
          <el-input v-model="regForm.password" type="password" placeholder="密码（至少 6 位）" size="large" show-password
                    style="margin-bottom:12px" />
          <el-input v-model="regForm.nickname" placeholder="昵称（可选）" size="large" style="margin-bottom:16px" />
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doRegister">注册并登录</el-button>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api'

const router = useRouter()
const tab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', nickname: '' })

function save(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user))
  ElMessage.success('欢迎，' + (data.user.nickname || data.user.username))
  router.push('/projects')
}

async function doLogin() {
  if (!loginForm.username || !loginForm.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try { save(await login(loginForm)) } finally { loading.value = false }
}

async function doRegister() {
  loading.value = true
  try { save(await register(regForm)) } finally { loading.value = false }
}
</script>
