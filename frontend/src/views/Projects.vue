<template>
  <div>
    <div class="toolbar">
      <h2 class="page-title" style="flex:1;margin:0">我的项目</h2>
      <el-button type="primary" @click="openCreate">新建项目</el-button>
    </div>

    <div v-loading="loading">
      <div class="project-grid" v-if="list.length">
        <div class="project-card" v-for="p in list" :key="p.id" @click="$router.push(`/projects/${p.id}`)">
          <div class="project-name">{{ p.name }}</div>
          <div class="text-muted">{{ p.description || '暂无描述' }}</div>
          <div class="text-muted" style="margin-top:8px">创建于 {{ p.created_at }}</div>
        </div>
      </div>
      <div class="empty" v-else-if="!loading">还没有项目，点右上角新建一个</div>
    </div>

    <el-dialog v-model="dialogVisible" title="新建项目" width="460px">
      <el-form label-width="70px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：order-mall" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="被测系统说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createProject, listProjects } from '../api'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ name: '', description: '' })

async function load() {
  loading.value = true
  try {
    const data = await listProjects()
    list.value = data.list
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.name = ''
  form.description = ''
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请填写项目名称')
  saving.value = true
  try {
    await createProject({ ...form })
    ElMessage.success('已创建')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
