<template>
  <div class="app-versions">
    <div class="page-header">
      <h2>APP版本管理</h2>
      <el-button type="primary" :icon="Upload" @click="openDialog()">上传新版本</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="version_name" label="版本名" width="120" />
        <el-table-column prop="version_code" label="版本号" width="100" />
        <el-table-column prop="platform" label="平台" width="120">
          <template #default="{ row }">
            <el-tag :type="row.platform === 'android_tv' ? 'primary' : 'success'" size="small">
              {{ row.platform === 'android_tv' ? 'TV端' : '手机端' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="120">
          <template #default="{ row }">{{ formatSize(row.apk_size) }}</template>
        </el-table-column>
        <el-table-column prop="update_log" label="更新日志" min-width="200" show-overflow-tooltip />
        <el-table-column label="强制更新" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.force_update" type="danger" size="small">强制</el-tag>
            <el-tag v-else type="info" size="small">可选</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.is_active ? 'success' : 'info'" size="small">
              {{ row.is_active ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :formatter="(r) => formatDate(r.created_at)" label="创建时间" width="160" />
        <el-table-column label="下载链接" min-width="260">
          <template #default="{ row }">
            <a :href="downloadUrl(row.apk_url)" target="_blank" style="color: #409eff;">
              {{ downloadUrl(row.apk_url) }}
            </a>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑版本' : '上传新版本'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="版本名称" prop="version_name">
          <el-input v-model="form.version_name" placeholder="如: 1.0.0" />
        </el-form-item>
        <el-form-item label="版本号" prop="version_code">
          <el-input-number v-model="form.version_code" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-radio-group v-model="form.platform">
            <el-radio value="android_tv">TV端</el-radio>
            <el-radio value="android">手机端</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="APK文件" prop="apk">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".apk"
            :on-change="handleFileChange"
          >
            <el-button type="primary">选择APK文件</el-button>
            <template #tip>
              <span class="el-upload__tip">最大200MB，仅支持 .apk</span>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="更新日志">
          <el-input
            v-model="form.update_log"
            type="textarea"
            :rows="4"
            placeholder="更新内容描述"
          />
        </el-form-item>
        <el-form-item label="强制更新">
          <el-switch v-model="form.force_update" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.is_active" />
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
import { ref, reactive, onMounted } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate, formatSize } from '@/utils/format'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const uploadRef = ref(null)
const selectedFile = ref(null)

const defaultForm = {
  version_name: '',
  version_code: 1,
  platform: 'android_tv',
  update_log: '',
  force_update: false,
  is_active: true,
  _id: null
}

const form = reactive({ ...defaultForm })

const rules = {
  version_name: [{ required: true, message: '请输入版本名称', trigger: 'blur' }],
  version_code: [{ required: true, message: '请输入版本号', trigger: 'blur' }]
}

const downloadUrl = (path) => {
  const base = window.location.origin
  if (path && path.startsWith('http')) return path
  return base + (path || '')
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/apps', { params: { page: page.value, size: size.value } })
    if (res.code === 0) {
      list.value = res.data.list
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, {
      version_name: row.version_name,
      version_code: row.version_code,
      platform: row.platform,
      update_log: row.update_log || '',
      force_update: !!row.force_update,
      is_active: !!row.is_active,
      _id: row.id
    })
  } else {
    Object.assign(form, defaultForm)
  }
  selectedFile.value = null
  uploadRef.value?.clearFiles()
  dialogVisible.value = true
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (!selectedFile.value && !isEdit.value) {
    ElMessage.warning('请选择APK文件')
    return
  }

  submitLoading.value = true
  try {
    const fd = new FormData()
    fd.append('version_name', form.version_name)
    fd.append('version_code', form.version_code)
    fd.append('platform', form.platform)
    fd.append('update_log', form.update_log || '')
    fd.append('force_update', form.force_update ? '1' : '0')
    fd.append('is_active', form.is_active ? '1' : '0')
    if (selectedFile.value) {
      fd.append('apk', selectedFile.value)
    }

    let res
    if (isEdit.value) {
      res = await request.put(`/apps/${form._id}`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    } else {
      res = await request.post('/apps', fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    }
    if (res.code === 0) {
      ElMessage.success(isEdit.value ? '更新成功' : '上传成功')
      dialogVisible.value = false
      fetchList()
    }
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除版本 ${row.version_name}（${row.version_code}）？`, '确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await request.delete(`/apps/${row.id}`)
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

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
