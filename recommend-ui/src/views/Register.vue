<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')
const password = ref('')
const confirm = ref('')
const loading = ref(false)
const error = ref('')

async function register() {
  if (!username.value.trim() || !password.value.trim()) return
  if (password.value !== confirm.value) {
    error.value = '两次密码不一致'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value }),
    })
    if (!res.ok) throw new Error('register failed')
    await router.push('/login')
  } catch {
    error.value = '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
          </svg>
        </div>
        <h1>创建账号</h1>
        <p class="auth-sub">注册一个新账号使用系统</p>
      </div>
      <form class="auth-form" @submit.prevent="register">
        <div class="field">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="输入用户名" :disabled="loading" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="输入密码" :disabled="loading" />
        </div>
        <div class="field">
          <label>确认密码</label>
          <input v-model="confirm" type="password" placeholder="再次输入密码" :disabled="loading" />
        </div>
        <p v-if="error" class="auth-error">{{ error }}</p>
        <button type="submit" class="auth-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注 册' }}
        </button>
      </form>
      <p class="auth-footer">
        已有账号？<router-link to="/login">登录</router-link>
      </p>
    </div>
  </div>
</template>
<style scoped>
.auth-page {
  height: 100%; display: flex; align-items: center; justify-content: center;
  background: var(--bg-primary);
  background-image: radial-gradient(ellipse at 30% 50%, rgba(56,189,248,0.06) 0%, transparent 50%), radial-gradient(ellipse at 70% 50%, rgba(139,92,246,0.04) 0%, transparent 50%);
}
.auth-card {
  width: 400px; background: rgba(16,22,36,0.8); border: 1px solid var(--border-color);
  border-radius: 16px; padding: 40px; display: flex; flex-direction: column; align-items: center; gap: 24px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.4);
}
.auth-header { text-align: center; display: flex; flex-direction: column; align-items: center; gap: 8px; }
.auth-logo { color: var(--accent); filter: drop-shadow(0 0 12px var(--accent-glow)); }
.auth-header h1 { font-size: 22px; font-weight: 700; background: var(--gradient-blue); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.auth-sub { font-size: 12px; color: var(--text-muted); }
.auth-form { width: 100%; display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; color: var(--text-secondary); font-weight: 500; }
.field input { background: rgba(30,41,59,0.6); border: 1px solid var(--border-color); border-radius: 8px; padding: 10px 14px; color: var(--text-primary); font-size: 14px; outline: none; transition: border-color 0.2s; }
.field input:focus { border-color: var(--accent); box-shadow: 0 0 0 2px rgba(56,189,248,0.15); }
.auth-error { font-size: 13px; color: var(--danger); text-align: center; }
.auth-btn { width: 100%; padding: 12px; background: linear-gradient(135deg, #0ea5e9, #38bdf8); border: none; border-radius: 8px; color: white; font-size: 15px; font-weight: 600; cursor: pointer; }
.auth-btn:hover { opacity: 0.9; }
.auth-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.auth-footer { font-size: 13px; color: var(--text-muted); }
.auth-footer a { color: var(--accent); text-decoration: none; font-weight: 600; }
</style>
