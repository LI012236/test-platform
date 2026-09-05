<template>
  <div>
    <div class="toolbar">
      <el-button link @click="$router.push('/projects')">← 返回项目</el-button>
      <h2 class="page-title" style="margin:0">{{ project?.name || '' }}</h2>
      <span class="text-muted" style="align-self:center">{{ project?.description }}</span>
      <div style="flex:1"></div>
      <el-button :loading="seeding" @click="importDemo">导入示例</el-button>
    </div>

    <el-tabs v-model="tab">
      <!-- ================= 接口管理 ================= -->
      <el-tab-pane label="接口管理" name="apis">
        <div class="toolbar">
          <el-button type="primary" @click="openApiCreate">新增接口</el-button>
          <span class="text-muted" style="align-self:center">共 {{ apis.length }} 个接口</span>
        </div>

        <el-table :data="apis" border v-loading="apiLoading">
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column label="方法" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="methodType(row.method)">{{ row.method }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路径" min-width="200" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openApiEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeApi(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ================= 用例 ================= -->
      <el-tab-pane label="测试用例" name="cases">
        <div class="toolbar">
          <el-button type="primary" @click="openCaseCreate">新建用例</el-button>
          <span class="text-muted" style="align-self:center">共 {{ cases.length }} 条用例</span>
        </div>

        <el-table :data="cases" border v-loading="caseLoading">
          <el-table-column prop="name" label="用例" min-width="180" />
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column prop="created_at" label="创建时间" width="170" />
          <el-table-column label="操作" width="260">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push(`/cases/${row.id}`)">编辑步骤</el-button>
              <el-button link type="success" @click="openRun(row)">执行</el-button>
              <el-button link type="danger" @click="removeCase(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ================= 环境 ================= -->
      <el-tab-pane label="环境" name="envs">
        <div class="toolbar">
          <el-button type="primary" @click="openEnvCreate">新增环境</el-button>
          <span class="text-muted" style="align-self:center">执行用例前至少要有一个环境</span>
        </div>

        <el-table :data="envs" border v-loading="envLoading">
          <el-table-column prop="name" label="名称" width="140" />
          <el-table-column prop="base_url" label="Base URL" min-width="260" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeEnv(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑接口 -->
    <el-dialog v-model="apiDialog" :title="apiForm.id ? '编辑接口' : '新增接口'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="apiForm.name" placeholder="登录 / 下单 / 查商品" /></el-form-item>
        <el-form-item label="方法">
          <el-select v-model="apiForm.method" style="width:140px">
            <el-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径"><el-input v-model="apiForm.path" placeholder="/api/auth/login" /></el-form-item>
        <el-form-item label="Headers">
          <el-input v-model="apiForm.headers_json" type="textarea" :rows="2" placeholder='{"Content-Type":"application/json"}' />
        </el-form-item>
        <el-form-item label="Body">
          <el-input v-model="apiForm.body" type="textarea" :rows="4" placeholder='{"username":"admin","password":"admin123"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="apiDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveApi">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建用例 -->
    <el-dialog v-model="caseDialog" title="新建用例" width="460px">
      <el-form label-width="70px">
        <el-form-item label="名称"><el-input v-model="caseForm.name" placeholder="下单主流程" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="caseForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="caseDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCase">创建</el-button>
      </template>
    </el-dialog>

    <!-- 新增环境 -->
    <el-dialog v-model="envDialog" title="新增环境" width="480px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="envForm.name" placeholder="dev / test / prod" /></el-form-item>
        <el-form-item label="Base URL"><el-input v-model="envForm.base_url" placeholder="http://127.0.0.1:18091" /></el-form-item>
        <el-form-item label="公共 Header">
          <el-input v-model="envForm.headers_json" type="textarea" :rows="2" placeholder='{"Content-Type":"application/json"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="envDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEnv">创建</el-button>
      </template>
    </el-dialog>

    <!-- 执行 -->
    <el-dialog v-model="runDialog" title="执行用例" width="720px" @closed="stopPolling">
      <div v-if="!runResult">
        <el-form label-width="80px">
          <el-form-item label="环境">
            <el-select v-model="runEnvId" placeholder="选择环境" style="width:260px">
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

        <el-collapse v-model="expandedSteps">
          <el-collapse-item v-for="d in runResult.details" :key="d.id" :name="d.id">
            <template #title>
              <el-tag size="small" :type="d.status === 'PASSED' ? 'success' : 'danger'" style="margin-right:8px">
                {{ d.status }}
              </el-tag>
              第 {{ d.sort }} 步 · {{ d.api_name || '接口' }}
              <el-tag size="small" style="margin:0 8px" :type="methodType(d.method)">{{ d.method }}</el-tag>
              {{ d.duration_ms }} ms
            </template>
            <div class="text-muted" style="margin-bottom:4px">请求</div>
            <pre class="snapshot">{{ pretty(d.request_snapshot) }}</pre>
            <div class="text-muted" style="margin-bottom:4px">响应</div>
            <pre class="snapshot">{{ d.response_snapshot }}</pre>
            <div class="text-muted" style="margin-bottom:4px">断言</div>
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
  createApi, createCase, createEnv, deleteApi, deleteCase, deleteEnv,
  getProject, getRun, listApis, listCases, listEnvs, runCase, seedDemo, updateApi
} from '../api'

const route = useRoute()
const pid = Number(route.params.id)

const tab = ref(route.query.tab || 'apis')
const project = ref(null)
const apis = ref([])
const cases = ref([])
const envs = ref([])
const apiLoading = ref(false)
const caseLoading = ref(false)
const envLoading = ref(false)

const saving = ref(false)
const apiDialog = ref(false)
const caseDialog = ref(false)
const envDialog = ref(false)

const apiForm = reactive({ id: null, name: '', method: 'GET', path: '', headers_json: '{}', body: '' })
const caseForm = reactive({ name: '', description: '' })
const envForm = reactive({ name: 'dev', base_url: '', headers_json: '{}' })

const runDialog = ref(false)
const runEnvId = ref(null)
const continueOnFail = ref(false)
const running = ref(false)
const runResult = ref(null)
const expandedSteps = ref([])
const seeding = ref(false)
let timer = null

const methodType = (m) => ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }[m] || 'info')
const statusType = (s) => ({ PASSED: 'success', FAILED: 'danger', RUNNING: 'warning', ERROR: 'danger' }[s] || 'info')
const pretty = (text) => {
  try { return JSON.stringify(JSON.parse(text), null, 2) } catch (e) { return text }
}

async function loadProject() { project.value = await getProject(pid) }
async function loadApis() {
  apiLoading.value = true
  try { apis.value = (await listApis(pid)).list } finally { apiLoading.value = false }
}
async function loadCases() {
  caseLoading.value = true
  try { cases.value = (await listCases(pid)).list } finally { caseLoading.value = false }
}
async function loadEnvs() {
  envLoading.value = true
  try {
    envs.value = (await listEnvs(pid)).list
    if (!runEnvId.value && envs.value.length) runEnvId.value = envs.value[0].id
  } finally { envLoading.value = false }
}

// ---------- 接口 ----------
function openApiCreate() {
  Object.assign(apiForm, { id: null, name: '', method: 'GET', path: '', headers_json: '{}', body: '' })
  apiDialog.value = true
}
function openApiEdit(row) {
  Object.assign(apiForm, {
    id: row.id, name: row.name, method: row.method, path: row.path,
    headers_json: row.headers_json || '{}', body: row.body || ''
  })
  apiDialog.value = true
}
async function saveApi() {
  if (!apiForm.name || !apiForm.path) return ElMessage.warning('名称和路径不能为空')
  saving.value = true
  try {
    const payload = {
      name: apiForm.name, method: apiForm.method, path: apiForm.path,
      headers_json: apiForm.headers_json || '{}', body: apiForm.body
    }
    if (apiForm.id) await updateApi(apiForm.id, payload)
    else await createApi(pid, payload)
    ElMessage.success('已保存')
    apiDialog.value = false
    loadApis()
  } finally { saving.value = false }
}
async function removeApi(row) {
  await ElMessageBox.confirm(`删除接口「${row.name}」？`, '提示', { type: 'warning' })
  await deleteApi(row.id)
  ElMessage.success('已删除')
  loadApis()
}

// ---------- 用例 ----------
function openCaseCreate() {
  caseForm.name = ''
  caseForm.description = ''
  caseDialog.value = true
}
async function saveCase() {
  if (!caseForm.name) return ElMessage.warning('请填写用例名称')
  saving.value = true
  try {
    await createCase(pid, { ...caseForm })
    ElMessage.success('已创建')
    caseDialog.value = false
    loadCases()
  } finally { saving.value = false }
}
async function removeCase(row) {
  await ElMessageBox.confirm(`删除用例「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCase(row.id)
  ElMessage.success('已删除')
  loadCases()
}

// ---------- 环境 ----------
function openEnvCreate() {
  envForm.name = 'dev'
  envForm.base_url = ''
  envForm.headers_json = '{"Content-Type":"application/json"}'
  envDialog.value = true
}
async function saveEnv() {
  if (!envForm.base_url) return ElMessage.warning('请填写 Base URL')
  saving.value = true
  try {
    await createEnv(pid, { ...envForm })
    ElMessage.success('已创建')
    envDialog.value = false
    loadEnvs()
  } finally { saving.value = false }
}
async function removeEnv(row) {
  await ElMessageBox.confirm(`删除环境「${row.name}」？`, '提示', { type: 'warning' })
  await deleteEnv(row.id)
  ElMessage.success('已删除')
  loadEnvs()
}

// ---------- 导入示例 ----------
async function importDemo() {
  try {
    await ElMessageBox.confirm(
      '将导入 order-mall 的 5 个接口和一条「登录 → 加购 → 下单 → 查订单」用例，确定？',
      '导入示例', { type: 'info' }
    )
  } catch (e) {
    return
  }
  seeding.value = true
  try {
    const r = await seedDemo(pid, 'http://127.0.0.1:18091')
    ElMessage.success(r.message)
    await Promise.all([loadApis(), loadCases(), loadEnvs()])
  } finally {
    seeding.value = false
  }
}

// ---------- 执行 ----------
const currentCaseId = ref(null)

async function openRun(row) {
  currentCaseId.value = row.id
  runResult.value = null
  expandedSteps.value = []
  if (!envs.value.length) await loadEnvs()
  if (!envs.value.length) return ElMessage.warning('请先在「环境」里添加一个环境')
  runDialog.value = true
}

async function startRun() {
  running.value = true
  try {
    const run = await runCase(currentCaseId.value, {
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
      expandedSteps.value = data.details.filter(d => d.status !== 'PASSED').map(d => d.id)
    }
  }, 600)
}

function stopPolling() {
  if (timer) { clearInterval(timer); timer = null }
}

onMounted(async () => {
  await loadProject()
  await Promise.all([loadApis(), loadCases(), loadEnvs()])

  // 支持通过 ?runId= 直接打开某次执行报告（可分享链接）
  if (route.query.runId) {
    try {
      runResult.value = await getRun(Number(route.query.runId))
      expandedSteps.value = runResult.value.details
        .filter((d) => d.status !== 'PASSED')
        .map((d) => d.id)
      runDialog.value = true
    } catch (e) {
      /* 忽略：报告不存在时正常展示项目页 */
    }
  }
})
</script>
