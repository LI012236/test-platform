<template>
  <div class="app">
    <header class="header">
      <div class="logo" @click="$router.push('/projects')">接口测试平台</div>
      <nav class="nav" v-if="token">
        <router-link to="/projects">项目</router-link>
      </nav>
      <div class="user" v-if="token">
        <span>{{ nickname }}</span>
        <el-button link type="primary" @click="logout">退出</el-button>
      </div>
      <div class="user" v-else>
        <router-link to="/login" style="color:#c9d6ea">登录 / 注册</router-link>
      </div>
    </header>
    <main class="main">
      <router-view :key="$route.fullPath" />
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const tick = ref(0)
watch(() => route.fullPath, () => { tick.value++ })

const token = computed(() => (tick.value, localStorage.getItem('token')))
const user = computed(() => { tick.value; return JSON.parse(localStorage.getItem('user') || 'null') })
const nickname = computed(() => user.value?.nickname || user.value?.username || '')

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  tick.value++
  router.push('/login')
}
</script>
