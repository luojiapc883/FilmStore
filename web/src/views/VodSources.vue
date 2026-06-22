<template>
  <div class="vod-sources">
    <div class="page-header">
      <h2>点播源管理</h2>
      <div>
        <el-button @click="showBatchDialog = true" :icon="Upload">批量导入</el-button>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增点播源</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.type === 'spider' ? '爬虫' : row.type === 'json' ? 'JSON' : 'API' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="url" label="地址" min-width="300" show-overflow-tooltip />
        <el-table-column prop="group_name" label="分组" width="120" />
        <el-table-column label="默认" width="60">
          <template #default="{ row }">
            <el-tag v-if="row.is_default" type="success" size="small">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.is_active ? 'success' : 'info'" size="small">
              {{ row.is_active ? '启用' : '停用' }}
            </el-tag>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑点播源' : '新增点播源'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="spider">爬虫</el-radio>
            <el-radio value="json">JSON</el-radio>
            <el-radio value="api">API</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="地址" prop="url">
          <el-input v-model="form.url" placeholder="接口地址URL" />
        </el-form-item>
        <el-form-item label="爬虫KEY" v-if="form.type === 'spider'">
          <el-input v-model="form.spider_key" placeholder="爬虫类名/KEY" />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model="form.group_name" placeholder="默认分组" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort_order" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="默认源">
          <el-switch v-model="form.is_default" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="showBatchDialog" title="批量导入点播源" width="700px">
      <el-alert title="每行一个源，格式: 名称###类型###地址###分组" type="info" :closable="false" class="batch-hint" />
      <el-alert title="类型: spider/json/api; 分组可选; 分隔符 ###" type="warning" :closable="false" class="batch-hint" />
      <el-input
        v-model="batchText"
        type="textarea"
        :rows="12"
        placeholder="示例:&#10;天空资源###spider###http://sky.com/api.php###默认分组&#10;快播资源###spider###http://kuaibo.com/api.php###默认分组"
      />
      <template #footer>
        <el-button @click="showBatchDialog = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="handleBatchImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(100)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const showBatchDialog = ref(false)
const batchText = ref('')
const batchLoading = ref(false)
const formRef = ref(null)

const defaultForm = {
  name: '',
  type: 'spider',
  url: '',
  spider_key: '',
  group_name: '默认分组',
  sort_order: 0,
  is_default: false,
  _id: null
}
const form = reactive({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'blur' }],
  url: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/sources/vod', { params: { page: page.value, size: size.value } })
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
      name: row.name,
      type: row.type,
      url: row.url,
      spider_key: row.spider_key || '',
      group_name: row.group_name || '默认分组',
      sort_order: row.sort_order || 0,
      is_default: !!row.is_default,
      _id: row.id
    })
  } else {
    Object.assign(form, defaultForm)
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    let res
    if (isEdit.value) {
      res = await request.put(`/sources/vod/${form._id}`, form)
    } else {
      res = await request.post('/sources/vod', form)
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

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除点播源「${row.name}」？`, '确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await request.delete(`/sources/vod/${row.id}`)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      fetchList()
    }
  }).catch(() => {})
}

const handleBatchImport = async () => {
  if (!batchText.value.trim()) {
    ElMessage.warning('请输入源数据')
    return
  }

  batchLoading.value = true
  try {
    const sources = batchText.value.split('\n')
      .map(line => line.trim())
      .filter(line => line)
      .map(line => {
        const parts = line.split('###')
        return {
          name: parts[0],
          type: parts[1] || 'spider',
          url: parts[2],
          group_name: parts[3] || '默认分组',
          spider_key: parts[4] || '',
          sort_order: 0,
          is_default: false
        }
      })
      .filter(s => s.name && s.url)

    if (sources.length === 0) {
      ElMessage.warning('未识别到有效数据，格式: 名称###类型###地址###分组')
      return
    }

    const res = await request.post('/sources/vod/batch', { sources })
    if (res.code === 0) {
      ElMessage.success(res.message)
      showBatchDialog.value = false
      batchText.value = ''
      fetchList()
    }
  } finally {
    batchLoading.value = false
  }
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

.batch-hint {
  margin-bottom: 12px;
}
</style>
