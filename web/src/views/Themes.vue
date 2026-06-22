<template>
  <div class="themes">
    <div class="page-header">
      <h2>主题管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建主题</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="8" v-for="theme in list" :key="theme.id">
        <el-card shadow="hover" class="theme-card">
          <div class="theme-preview" :style="previewStyle(theme)">
            <div class="preview-bar">
              <div class="preview-dot" style="background: #ff5f56"></div>
              <div class="preview-dot" style="background: #ffbd2e"></div>
              <div class="preview-dot" style="background: #27c93f"></div>
              <span class="preview-title">{{ theme.title }}</span>
            </div>
            <div class="preview-content">
              <div class="preview-item" :style="{ background: getConfig(theme).surfaceColor || '#16213e' }"></div>
              <div class="preview-item short" :style="{ background: getConfig(theme).accentColor || '#e94560' }"></div>
            </div>
          </div>
          <div class="theme-info">
            <div class="theme-name">
              <strong>{{ theme.title }}</strong>
              <el-tag v-if="theme.is_default" type="success" size="small">默认</el-tag>
            </div>
            <p class="theme-key">标识: {{ theme.name }}</p>
          </div>
          <div class="theme-actions">
            <el-button text type="primary" size="small" @click="openDialog(theme)">编辑</el-button>
            <el-button text type="success" size="small" @click="setDefault(theme)" :disabled="theme.is_default">设为默认</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(theme)" :disabled="theme.is_default">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="list.length === 0 && !loading" description="暂无主题" />

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑主题' : '新建主题'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="英文标识" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="显示名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">颜色配置</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主色调">
              <el-color-picker v-model="cfg.primaryColor" show-alpha />
              <span class="color-value">{{ cfg.primaryColor }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="强调色">
              <el-color-picker v-model="cfg.accentColor" show-alpha />
              <span class="color-value">{{ cfg.accentColor }}</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="背景色">
              <el-color-picker v-model="cfg.backgroundColor" show-alpha />
              <span class="color-value">{{ cfg.backgroundColor }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="表面色">
              <el-color-picker v-model="cfg.surfaceColor" show-alpha />
              <span class="color-value">{{ cfg.surfaceColor }}</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="文字色">
              <el-color-picker v-model="cfg.textColor" show-alpha />
              <span class="color-value">{{ cfg.textColor }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="圆角">
              <el-input-number v-model="cfg.borderRadius" :min="0" :max="24" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="暗色">
              <el-switch v-model="cfg.darkMode" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">其他配置</el-divider>

        <el-form-item label="应用名称">
          <el-input v-model="cfg.appName" placeholder="影视仓" />
        </el-form-item>
        <el-form-item label="Logo URL">
          <el-input v-model="cfg.logoUrl" placeholder="图片URL" />
        </el-form-item>
        <el-form-item label="启动图">
          <el-input v-model="cfg.startupImage" placeholder="图片URL" />
        </el-form-item>
        <el-form-item label="底部栏">
          <el-radio-group v-model="cfg.tabBarStyle">
            <el-radio value="bottom">底部</el-radio>
            <el-radio value="side">侧边</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch v-model="form.is_default" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

const defaultCfg = {
  primaryColor: '#E84C3D',
  backgroundColor: '#1a1a2e',
  surfaceColor: '#16213e',
  textColor: '#e0e0e0',
  accentColor: '#e94560',
  logoUrl: '',
  appName: '影视仓',
  startupImage: '',
  tabBarStyle: 'bottom',
  playerStyle: 'default',
  borderRadius: 8,
  darkMode: true
}

const cfg = reactive({ ...defaultCfg })

const defaultForm = {
  name: '',
  title: '',
  is_default: false,
  _id: null
}

const form = reactive({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入主题标识', trigger: 'blur' }],
  title: [{ required: true, message: '请输入主题标题', trigger: 'blur' }]
}

const getConfig = (theme) => {
  try {
    return typeof theme.config === 'object' ? theme.config : JSON.parse(theme.config)
  } catch {
    return defaultCfg
  }
}

const previewStyle = (theme) => {
  const c = getConfig(theme)
  return { background: c.backgroundColor || '#1a1a2e' }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/themes')
    if (res.code === 0) {
      list.value = res.data.list
    }
  } finally {
    loading.value = false
  }
}

const openDialog = (theme) => {
  isEdit.value = !!theme
  if (theme) {
    const c = getConfig(theme)
    Object.assign(cfg, c)
    Object.assign(form, {
      name: theme.name,
      title: theme.title,
      is_default: !!theme.is_default,
      _id: theme.id
    })
  } else {
    Object.assign(cfg, defaultCfg)
    Object.assign(form, {
      name: '',
      title: '',
      is_default: false,
      _id: null
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = {
      name: form.name,
      title: form.title,
      is_default: form.is_default,
      config: { ...cfg }
    }

    let res
    if (isEdit.value) {
      res = await request.put(`/themes/${form._id}`, data)
    } else {
      res = await request.post('/themes', data)
    }
    if (res.code === 0) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      fetchList()
    }
  } finally {
    submitLoading.value = false
  }
}

const setDefault = async (theme) => {
  await request.put(`/themes/${theme.id}`, { is_default: true })
  ElMessage.success('默认主题已更新')
  fetchList()
}

const handleDelete = (theme) => {
  ElMessageBox.confirm(`确定删除主题「${theme.title}」？`, '确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await request.delete(`/themes/${theme.id}`)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      fetchList()
    }
  }).catch(() => {})
}

onMounted(fetchList)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.theme-card {
  margin-bottom: 20px;
}

.theme-preview {
  border-radius: 8px;
  overflow: hidden;
  height: 140px;
  display: flex;
  flex-direction: column;
}

.preview-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.3);
}

.preview-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.preview-title {
  margin-left: 8px;
  font-size: 12px;
  color: #fff;
  opacity: 0.8;
}

.preview-content {
  flex: 1;
  padding: 12px;
  display: flex;
  gap: 8px;
}

.preview-item {
  flex: 1;
  border-radius: 4px;
  height: 100%;
}

.preview-item.short {
  flex: 0 0 40px;
}

.theme-info {
  padding: 12px 0;
}

.theme-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.theme-key {
  font-size: 12px;
  color: #909399;
}

.theme-actions {
  border-top: 1px solid #eee;
  padding-top: 12px;
}

.color-value {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}
</style>
