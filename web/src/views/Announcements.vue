<template>
  <div class="announcements">
    <div class="page-header">
      <h2>公告管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建公告</el-button>
    </div>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.is_pinned" type="warning" size="small">置顶</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.is_active ? 'success' : 'info'" size="small">
              {{ row.is_active ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间范围" width="320">
          <template #default="{ row }">
            <span class="time-range">
              {{ row.start_at ? formatDate(row.start_at) : '不限' }}
              ~ {{ row.end_at ? formatDate(row.end_at) : '不限' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column :formatter="(r) => formatDate(r.created_at)" label="创建时间" width="160" />
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新建公告'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="text">纯文本</el-radio>
            <el-radio value="rich">富文本</el-radio>
            <el-radio value="link">链接</el-radio>
            <el-radio value="image">图片</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="链接" v-if="form.type === 'link'">
          <el-input v-model="form.link_url" placeholder="https://" />
        </el-form-item>
        <el-form-item label="图片" v-if="form.type === 'image'">
          <el-input v-model="form.image_url" placeholder="图片URL" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发布时间">
              <el-date-picker
                v-model="form.start_at"
                type="datetime"
                placeholder="立即发布"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                :clearable="true"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过期时间">
              <el-date-picker
                v-model="form.end_at"
                type="datetime"
                placeholder="永不过期"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                :clearable="true"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="置顶">
          <el-switch v-model="form.is_pinned" />
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
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate } from '@/utils/format'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

const defaultForm = {
  title: '',
  content: '',
  type: 'text',
  link_url: '',
  image_url: '',
  is_pinned: false,
  is_active: true,
  start_at: null,
  end_at: null
}

const form = reactive({ ...defaultForm })

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const typeLabel = (t) => ({ text: '文本', rich: '富文本', link: '链接', image: '图片' }[t] || t)
const typeTag = (t) => ({ text: 'info', rich: 'primary', link: 'warning', image: 'success' }[t] || 'info')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/announcements', { params: { page: page.value, size: size.value } })
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
      title: row.title,
      content: row.content,
      type: row.type,
      link_url: row.link_url || '',
      image_url: row.image_url || '',
      is_pinned: !!row.is_pinned,
      is_active: !!row.is_active,
      start_at: row.start_at || null,
      end_at: row.end_at || null,
      _id: row.id
    })
  } else {
    Object.assign(form, defaultForm)
    form._id = null
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
      res = await request.put(`/announcements/${form._id}`, form)
    } else {
      res = await request.post('/announcements', form)
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
  ElMessageBox.confirm(`确定删除公告「${row.title}」？`, '确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await request.delete(`/announcements/${row.id}`)
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

.time-range {
  font-size: 12px;
  color: #909399;
}
</style>
