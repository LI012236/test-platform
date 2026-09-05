<template>
  <div>
    <div class="toolbar">
      <el-button link @click="$router.push(`/projects/${projectId}`)">← 返回项目</el-button>
      <h2 class="page-title" style="margin:0">{{ testCase?.name || '用例' }}</h2>
      <span class="text-muted" style="align-self:center">{{ testCase?.description }}</span>
      <div style="flex:1"></div>
      <el-button type="primary" @click="openAdd">添加步骤</el-button>
      <el-button type="success" :disabled="!steps.length" @click="openRun">执行用例</el-button>
    </div>

    <el-table :data="steps" border v-loading="loading">
      <el-table-column prop="sort" label="#" width="60" align="center" />
      <el-table-column label="接口" min-width="220">
        <template #default="{ row }">
          <el-tag size="small" :type="methodType(row.method)" style="margin-right:6px">{{ row.method }}</el-tag>
          {{ row.api_name }}
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" min-width="200" />
      <el-table-column label="断言" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ assertionCount(row.assertions_json) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row, $index }">
          <el-button link :disabled="$index === 0" @click="move($index, -1)">上移</el-button>
          <el-button link :disabled="$index === steps.length - 1" @click="move($index, 1)">下移</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="empty" v-if="!loading && !steps.length">
      还没有步骤，点右上角「添加步骤」开始
    </div>

    <!-- 步骤编辑 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑步骤' : '添加步骤'" width="640px">
      <el-form label-width="100px">
        <el-form-item label="接口">
          <el-select v-model="form.api_id" placeholder="选择一个接口" style="width:100%">
            <el-option v-for="a in apis" :key="a.id" :label="`${a.method} ${a.name}  ${a.path}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="覆盖 Header">
          <el-input v-model="form.override_headers" type="textarea" :rows="2"
                    placeholder='{"Authorization":"Bearer ${token}"}' />
        </el-form-item>
        <el-form-item label="覆盖 Body">
          <el-input v-model="form.override_body" type="textarea" :rows="3" placeholder="留空则用接口自带的 body" />
        </el-form-item>
        <el-form-item label="变量提取">
          <el-input v-model="form.extractors_json" type="textarea" :rows="2"
                    placeholder='[{"name":"token","path":"$.data.token"}]' />
        </el-form-item>
        <el-form-item label="断言">
          <el-input v-model="form.assertions_json" type="textarea" :rows="4"
                    placeholder='[{"type":"status","expect":200},{"type":"json","path":"$.code","expect":0}]' />
        </el-form-item>
        <div class="text-muted" style="padding-left:100px">
          断言类型：status（状态码）/ json（按路径比较）/ contain（包含文本）/ time（耗时上限 max_ms）
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 执行 -->
    <el-dialog v-model="runDialog" title="执行用例" width="720px" @closed="stopPolling">
      <div v-if="!runResult">
        <el-form label-width="80px">
          <el-form-item label="环境">
            <el-select v-model="runEnvId" placeholder="选择环境" style="width:300px">
              <el-option v-for="e in envs" :key="e.id" :label="e.name + ' · ' + e.base_url" :value="e.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="失败策略">
            <el-switch v-model="continueOnFail" active-text="失败继续跑完" inactive-text="失败即中断" />
          </el-form-item>
        </el-form>
        <el-button type="primary" :loading="running" :disabled="!runEnvId" @click="startRun">开始执行</el-button>
      </div>

      <div v-else>
        <el-descriptions :column="3" border style="margin-bottom:12px">
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(runResult.run.status)">{{ runResult.run.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="通过 / 总数">
            {{ runResult.run.passed }} / {{ runResult.run.total }}
          </el-descriptions-item>
          <el-descriptions-item label="耗时">{{ runResult.run.duration_ms }} ms</el-descriptions-item>
        </el-descriptions>

        <el-collapse v-model="expanded">
          <el-collapse-item v-for="d in runResult.details" :key="d.id" :name="d.id">
            <template #title>
              <el-tag size="small" :type="d.status === 'PASSED' ? 'success' : 'danger'" style="margin-right:8px">
                {{ d.status }}
              </el-tag>
              第 {{ d.sort }} 步 · {{ d.api_name || '接口' }}
              <el-tag size="small" style="margin:0 8px" :type="methodType(d.method)">{{ d.method }}</el-tag>
              {{ d.duration_ms }} ms
            </template>
            <div class="text-muted">请求</div>
            <pre class="snapshot">{{ pretty(d.request_snapshot) }}</pre>
            <div class="text-muted">响应</div>
            <pre class="snapshot">{{ d.response_snapshot }}</pre>
            <div class="text-muted">断言</div>
            <pre class="snapshot">{{ pretty(d.assertions_result) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addStep, deleteStep, getCase, getRun, listApis, listEnvs, listSteps, runCase, updateStep
} from '../api'

const route = useRoute()
const caseId = Number(route.params.id)

const loading = ref(false)
const saving = ref(false)
const testCase = ref(null)
const projectId = ref(null)
const steps = ref([])
const apis = ref([])
const envs = ref([])

const dialog = ref(false)
const form = reactive({
  id: null, api_id: null, override_headers: '', override_body: '',
  extractors_json: '[]', assertions_json: '[]'
})

const runDialog = ref(false)
const runEnvId = ref(null)
const continueOnFail = ref(false)
const running = ref(false)
const runResult = ref(null)
const expanded = ref([])
let timer = null

const methodType = (m) => ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }[m] || 'info')
const statusType = (s) => ({ PASSED: 'success', FAILED: 'danger', RUNNING: 'warning', ERROR: 'danger' }[s] || 'info')
const pretty = (t) => { try { return JSON.stringify(JSON.parse(t), null, 2) } catch (e) { return t } }
const assertionCount = (json) => {
  try { return JSON.parse(json || '[]').length } catch (e) { return 0 }
}

async function load() {
  loading.value = true
  try {
    const c = await getCase(caseId)
    testCase.value = c
    projectId.value = c.project_id
    const [s, a, e] = await Promise.all([listSteps(caseId), listApis(c.project_id), listEnvs(c.project_id)])
    steps.value = s.list
    apis.value = a.list
    envs.value = e.list
    if (envs.value.length) runEnvId.value = envs.value[0].id
  } finally {
    loading.value = false
  }
}

function openAdd() {
  Object.assign(form, {
    id: null, api_id: null, override_headers: '', override_body: '',
    extractors_json: '[]', assertions_json: '[{"type":"status","expect":200}]'
  })
  dialog.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, api_id: row.api_id,
    override_headers: row.override_headers || '',
    override_body: row.override_body || '',
    extractors_json: row.extractors_json || '[]',
    assertions_json: row.assertions_json || '[]'
  })
  dialog.value = true
}

async function save() {
  if (!form.api_id) return ElMessage.warning('请选择接口')
  saving.value = true
  try {
    const payload = {
      api_id: form.api_id,
      override_headers: form.override_headers,
      override_body: form.override_body,
      extractors_json: form.extractors_json,
      assertions_json: form.assertions_json
    }
    if (form.id) await updateStep(form.id, payload)
    else await addStep(caseId, payload)
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`删除第 ${row.sort} 步「${row.api_name}」？`, '提示', { type: 'warning' })
  await deleteStep(row.id)
  ElMessage.success('已删除')
  load()
}

async function move(index, delta) {
  const target = steps.value[index + delta]
  const current = steps.value[index]
  if (!target) return
  await updateStep(current.id, {
    sort: target.sort, api_id: current.api_id, override_headers: current.override_headers,
    override_body: current.override_body, extractors_json: current.extractors_json,
    assertions_json: current.assertions_json
  })
  await updateStep(target.id, {
    sort: current.sort, api_id: target.api_id, override_headers: target.override_headers,
    override_body: target.override_body, extractors_json: target.extractors_json,
    assertions_json: target.assertions_json
  })
  load()
}

function openRun() {
  runResult.value = null
  expanded.value = []
  runDialog.value = true
}

async function startRun() {
  running.value = true
  try {
    const run = await runCase(caseId, {
      env_id: runEnvId.value,
      continue_on_fail: continueOnFail.value
    })
    pollRun(run.id)
  } finally {
    running.value = false
  }
}

function pollRun(id) {
  stopPolling()
  timer = setInterval(async () => {
    const data = await getRun(id)
    runResult.value = data
    if (data.run.status !== 'RUNNING') {
      stopPolling()
      expanded.value = data.details.filter(d => d.status !== 'PASSED').map(d => d.id)
    }
  }, 600)
}

function stopPolling() {
  if (timer) { clearInterval(timer); timer = null }
}

onMounted(load)
</script>
