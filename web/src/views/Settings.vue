<template>
  <div class="settings">
    <h2 class="page-title">系统设置</h2>

    <el-card shadow="never">
      <el-form ref="formRef" :model="form" label-width="140px">
        <el-divider content-position="left">基本设置</el-divider>

        <el-form-item label="站点标题">
          <el-input v-model="form.site_title" placeholder="影视仓管理后台" />
        </el-form-item>
        <el-form-item label="APP名称">
          <el-input v-model="form.app_name" placeholder="影视仓" />
        </el-form-item>
        <el-form-item label="APP全局通知">
          <el-input
            v-model="form.app_notice"
            type="textarea"
            :rows="3"
            placeholder="留空则不显示通知"
          />
        </el-form-item>

        <el-divider content-position="left">客户端设置</el-divider>

        <el-form-item label="服务器地址">
          <el-input v-model="form.server_addr" placeholder="留空使用当前地址，如 http://192.168.1.100:3000">
            <template #append>
              <el-button :loading="testLoading" @click="handleTestConnection">测试连接</el-button>
            </template>
          </el-input>
          <span class="form-hint">Android 客户端将通过此地址连接后台，留空则自动使用当前访问地址</span>
        </el-form-item>

        <el-form-item label="缓存时间(秒)">
          <el-input-number v-model="form.client_config_cache_time" :min="0" :max="86400" style="width: 200px" />
          <span class="form-hint">客户端拉取配置的缓存时长，默认300秒</span>
        </el-form-item>
        <el-form-item label="接口检测间隔(秒)">
          <el-input-number v-model="form.source_check_interval" :min="60" :max="86400" style="width: 200px" />
        </el-form-item>

        <el-divider />

        <el-form-item>
          <el-button type="primary" :loading="saveLoading" @click="handleSave">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="backup-card">
      <template #header>
        <span>数据维护</span>
      </template>
      <el-button type="warning" :icon="Download" :loading="backupLoading" @click="handleBackup">
        备份数据库
      </el-button>
      <span class="form-hint" style="margin-left: 12px">
        备份文件保存在 data/backup 目录，自动保留最近30天
      </span>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const formRef = ref(null)
const saveLoading = ref(false)
const backupLoading = ref(false)
const testLoading = ref(false)

const form = reactive({
  site_title: '',
  app_name: '',
  app_notice: '',
  server_addr: '',
  client_config_cache_time: 300,
  source_check_interval: 3600
})

const fetchConfig = async () => {
  try {
    const res = await request.get('/config')
    if (res.code === 0) {
      const data = res.data
      form.site_title = data.site_title || ''
      form.app_name = data.app_name || '影视仓'
      form.app_notice = data.app_notice || ''
      form.server_addr = data.server_addr || ''
      form.client_config_cache_time = parseInt(data.client_config_cache_time) || 300
      form.source_check_interval = parseInt(data.source_check_interval) || 3600
    }
  } catch (err) {
    console.error(err)
  }
}

const handleSave = async () => {
  saveLoading.value = true
  try {
    const res = await request.put('/config', {
      configs: {
        site_title: form.site_title,
        app_name: form.app_name,
        app_notice: form.app_notice,
        server_addr: form.server_addr,
        client_config_cache_time: String(form.client_config_cache_time),
        source_check_interval: String(form.source_check_interval)
      }
    })
    if (res.code === 0) {
      ElMessage.success('配置保存成功')
    }
  } finally {
    saveLoading.value = false
  }
}

const handleBackup = async () => {
  backupLoading.value = true
  try {
    const res = await request.post('/config/backup')
    if (res.code === 0) {
      ElMessage.success('数据库备份成功')
    }
  } finally {
    backupLoading.value = false
  }
}

const handleTestConnection = async () => {
  let addr = form.server_addr.trim()
  if (!addr) addr = window.location.origin
  if (addr.endsWith('/')) addr = addr.slice(0, -1)

  testLoading.value = true
  try {
    const res = await fetch(addr + '/api/health', { signal: AbortSignal.timeout(5000) })
    const json = await res.json()
    if (json.code === 0) {
      ElMessage.success('✅ 连接成功！')
    } else {
      ElMessage.warning('⚠️ 服务返回异常：' + (json.message || '未知'))
    }
  } catch (e) {
    ElMessage.error('❌ 连接失败：' + e.message)
  } finally {
    testLoading.value = false
  }
}

onMounted(fetchConfig)
</script>

<style scoped>
.page-title {
  margin-bottom: 20px;
}

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.backup-card {
  margin-top: 20px;
}
</style>
