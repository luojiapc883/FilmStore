<template>
  <div class="dashboard">
    <h2 class="page-title">控制台</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: stat.bg }">
            <el-icon :size="28" :color="stat.color">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-value">{{ stat.value }}</p>
            <p class="stat-label">{{ stat.label }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="recent-card">
      <template #header>
        <span>最近操作</span>
      </template>
      <el-timeline v-if="recentLogs.length > 0">
        <el-timeline-item
          v-for="log in recentLogs"
          :key="log.id"
          :timestamp="formatDate(log.created_at)"
          placement="top"
        >
          <p>{{ log.detail }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无操作记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Bell, Upload, VideoCamera, Monitor, Brush } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { formatDate } from '@/utils/format'

const stats = ref([])
const recentLogs = ref([])

const fetchStats = async () => {
  try {
    const res = await request.get('/config/stats')
    if (res.code === 0) {
      stats.value = [
        { label: '公告数', value: res.data.announcementCount, icon: Bell, color: '#e6a23c', bg: '#fdf6ec' },
        { label: '活动公告', value: res.data.activeAnnouncementCount, icon: Bell, color: '#67c23a', bg: '#f0f9eb' },
        { label: '点播源', value: res.data.activeVodSourceCount + '/' + res.data.vodSourceCount, icon: VideoCamera, color: '#409eff', bg: '#ecf5ff' },
        { label: '直播源', value: res.data.activeLiveSourceCount + '/' + res.data.liveSourceCount, icon: Monitor, color: '#909399', bg: '#f4f4f5' },
      ]
      recentLogs.value = res.data.recentLogs || []
    }
  } catch (err) {
    console.error(err)
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.page-title {
  margin-bottom: 20px;
  font-size: 22px;
  color: #303133;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 16px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
