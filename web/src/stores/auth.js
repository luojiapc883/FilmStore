import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const admin = ref(JSON.parse(localStorage.getItem('admin') || 'null'))

  const isLoggedIn = () => !!token.value

  const setAuth = (t, a) => {
    token.value = t
    admin.value = a
    localStorage.setItem('token', t)
    localStorage.setItem('admin', JSON.stringify(a))
  }

  const checkAuth = async () => {
    if (!token.value) return false
    try {
      const res = await request.get('/auth/me')
      if (res.code === 0) {
        admin.value = res.data
        return true
      }
    } catch {
      logout()
    }
    return false
  }

  const login = async (username, password) => {
    const res = await request.post('/auth/login', { username, password })
    if (res.code === 0) {
      setAuth(res.data.token, res.data.admin)
      return true
    }
    return false
  }

  const logout = () => {
    token.value = ''
    admin.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('admin')
  }

  return { token, admin, isLoggedIn, setAuth, checkAuth, login, logout }
})
