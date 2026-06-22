<template>
  <div class="logs">
    <div class="page-header">
      <h2>操作日志</h2>
    </div>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="admin_name" label="操作人" width="120" />
        <el-table-column prop="action" label="操作" width="160">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTag(row.action)">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="目标" width="160" />
        <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column :formatter="(r) => formatDate(r.created_at)" label="时间" width="160" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { formatDate } from '@/utils/format'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(50)
const total = ref(0)

const actionLabel = (action) => {
  const map = {
    'login': '登录',
    'change_password': '修改密码',
    'create_announcement': '创建公告',
    'update_announcement': '更新公告',
    'delete_announcement': '删除公告',
    'create_app_version': '创建版本',
    'update_app_version': '更新版本',
    'delete_app_version': '删除版本',
    'create_vod_source': '创建点播源',
    'update_vod_source': '更新点播源',
    'delete_vod_source': '删除点播源',
    'batch_import_vod': '批量导入点播源',
    'create_live_source': '创建直播源',
    'update_live_source': '更新直播源',
    'delete_live_source': '删除直播源',
    'batch_import_live': '批量导入直播源',
    'create_theme': '创建主题',
    'update_theme': '更新主题',
    'delete_theme': '删除主题',
    'update_config': '更新配置',
  }
  return map[action] || action
}

const actionTag = (action) => {
  if (action.includes('create') || action.includes('import')) return 'success'
  if (action.includes('delete')) return 'danger'
  if (action.includes('update') || action.includes('change')) return 'warning'
  return 'info'
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/config/logs', { params: { page: page.value, size: size.value } })
    if (res.code === 0) {
      list.value = res.data.list
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
