<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <img v-if="false" src="" alt="logo" class="logo" />
        <span class="title" v-show="!isCollapsed">影视仓管理</span>
        <span class="title-short" v-show="isCollapsed">仓</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :router="true"
        background-color="#1a1a2e"
        text-color="#b0b0b0"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>控制台</template>
        </el-menu-item>
        <el-menu-item index="/announcements">
          <el-icon><Bell /></el-icon>
          <template #title>公告管理</template>
        </el-menu-item>
        <el-menu-item index="/app-versions">
          <el-icon><Upload /></el-icon>
          <template #title>APP更新</template>
        </el-menu-item>
        <el-sub-menu index="sources">
          <template #title>
            <el-icon><VideoCamera /></el-icon>
            <span>接口管理</span>
          </template>
          <el-menu-item index="/sources/vod">点播源管理</el-menu-item>
          <el-menu-item index="/sources/live">直播源管理</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/themes">
          <el-icon><Brush /></el-icon>
          <template #title>主题管理</template>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 主区域 -->
    <div class="main" :class="{ expanded: isCollapsed }">
      <!-- 顶部导航 -->
      <header class="header">
        <div class="header-left">
          <el-button
            :icon="Fold"
            text
            @click="isCollapsed = !isCollapsed"
          />
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="admin-name">{{ auth.admin?.nickname || auth.admin?.username }}</span>
          <el-dropdown trigger="click">
            <el-avatar size="small" icon="UserFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showPasswordDialog = true">
                  <el-icon><Lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Fold, SwitchButton, UserFilled, Lock, Bell, Upload, VideoCamera, Monitor, Brush, Setting, Document, Odometer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const isCollapsed = ref(false)
const showPasswordDialog = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')

const handleLogout = () => {
  auth.logout()
  router.push('/login')
  ElMessage.success('已退出')
}

const handleChangePassword = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  pwdLoading.value = true
  try {
    const res = await request.put('/auth/password', {
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    if (res.code === 0) {
      ElMessage.success('密码修改成功')
      showPasswordDialog.value = false
      pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    }
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
}

.sidebar {
  width: var(--sidebar-width);
  background: #1a1a2e;
  transition: width 0.3s;
  flex-shrink: 0;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}

.title-short {
  font-size: 24px;
}

.sidebar :deep(.el-menu) {
  border-right: none;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: margin-left 0.3s;
}

.header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-name {
  font-size: 14px;
  color: #606266;
}

.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>
