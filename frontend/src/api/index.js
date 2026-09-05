import request from './request'

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)

export const listProjects = () => request.get('/projects')
export const createProject = (data) => request.post('/projects', data)
export const updateProject = (id, data) => request.put(`/projects/${id}`, data)
export const deleteProject = (id) => request.delete(`/projects/${id}`)
export const getProject = (id) => request.get(`/projects/${id}`)

export const listEnvs = (pid) => request.get(`/projects/${pid}/envs`)
export const createEnv = (pid, data) => request.post(`/projects/${pid}/envs`, data)
export const deleteEnv = (id) => request.delete(`/envs/${id}`)

export const listApis = (pid) => request.get(`/projects/${pid}/apis`)
export const createApi = (pid, data) => request.post(`/projects/${pid}/apis`, data)
export const updateApi = (id, data) => request.put(`/apis/${id}`, data)
export const deleteApi = (id) => request.delete(`/apis/${id}`)

export const listCases = (pid) => request.get(`/projects/${pid}/cases`)
export const getCase = (id) => request.get(`/cases/${id}`)
export const createCase = (pid, data) => request.post(`/projects/${pid}/cases`, data)
export const deleteCase = (id) => request.delete(`/cases/${id}`)

export const runCase = (cid, data) => request.post(`/cases/${cid}/run`, data)
export const getRun = (id) => request.get(`/runs/${id}`)
export const listRuns = (pid, limit = 20) => request.get(`/projects/${pid}/runs`, { params: { limit } })

export const listSteps = (cid) => request.get(`/cases/${cid}/steps`)
export const addStep = (cid, data) => request.post(`/cases/${cid}/steps`, data)
export const updateStep = (id, data) => request.put(`/steps/${id}`, data)
export const deleteStep = (id) => request.delete(`/steps/${id}`)

export const seedDemo = (pid, baseUrl) => request.post(`/projects/${pid}/seed-demo`, { base_url: baseUrl })
