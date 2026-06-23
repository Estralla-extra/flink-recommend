 <script setup lang="ts">
 import { ref, onMounted, onUnmounted, computed } from 'vue'
 import { useRoute, useRouter } from 'vue-router'
 import { useMetricsStore } from '@/stores/metrics'
 import StatusDot from './common/StatusDot.vue'
 import Icon from './common/Icon.vue'
import { logoutApi } from '@/api'
import { fetchUserProfile } from '@/api'
import type { UserProfile } from '@/types'

const route = useRoute()
 const router = useRouter()
 const metricsStore = useMetricsStore()
 
 const now = ref('')
const menuOpen = ref(false)
const menuRef = ref<HTMLElement | null>(null)
const profile = ref<UserProfile | null>(null)
let timer: ReturnType<typeof setInterval>
 
 const currentPath = computed(() => route.path)
 
 const navItems = [
   { path: '/dashboard', label: '监控大屏', icon: 'chart' },
   { path: '/recommend', label: '用户推荐', icon: 'user' },
   { path: '/item-recommend', label: '商品推荐', icon: 'cart' },
   { path: '/analytics', label: '深度分析', icon: 'activity' },
 ]
 
 const user = computed(() => {
   try {
     const raw = localStorage.getItem('user')
     return raw ? JSON.parse(raw) : null
   } catch { return null }
 })
 
 function toggleMenu() {
   menuOpen.value = !menuOpen.value
 }
 
 function logout() {
   menuOpen.value = false
   logoutApi().catch(() => {})
   localStorage.removeItem('token')
   localStorage.removeItem('user')
   router.push('/login')
 }
 
 function handleClickOutside(e: MouseEvent) {
   if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
     menuOpen.value = false
   }
 }
 
onMounted(() => {
  // fetch user profile for display
  try {
    const token = localStorage.getItem('token')
    if (token) fetchUserProfile().then(d => profile.value = d).catch(() => {})
  } catch { /* ignore */ }

  now.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
   timer = setInterval(() => {
     now.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
   }, 1000)
   document.addEventListener('click', handleClickOutside)
 })
 
 onUnmounted(() => {
   clearInterval(timer)
   document.removeEventListener('click', handleClickOutside)
 })
 </script>
 
 <template>
   <header class="header-bar">
     <div class="header-left">
       <div class="logo-icon">
         <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
           <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
         </svg>
       </div>
       <h1 class="title">实时推荐与流计算监控大屏</h1>
       <nav class="nav-items">
         <router-link
           v-for="item in navItems"
           :key="item.path"
           :to="item.path"
           :class="['nav-item', { active: currentPath === item.path }]"
         >
           <Icon :name="item.icon" :size="13" /> {{ item.label }}
         </router-link>
       </nav>
     </div>
     <div class="header-right">
       <div class="status-item">
         <StatusDot :status="metricsStore.lastStatus === 200 ? 'online' : 'offline'" />
         <span>{{ metricsStore.lastStatus === 200 ? '系统运行中' : '服务异常' }}</span>
       </div>
 
       <div v-if="user" class="user-area" ref="menuRef">
         <span class="user-label">{{ user.name || user.username }}</span>
         <button class="user-btn" @click="toggleMenu">
           <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
             <circle cx="12" cy="8" r="4" />
             <path d="M4 20c0-4 3.5-7 8-7s8 3 8 7" />
           </svg>
         </button>
 
         <Transition name="drop">
           <div v-if="menuOpen" class="user-dropdown">
             <div class="drop-header">
               <div class="drop-avatar">
                 <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2">
                   <circle cx="12" cy="8" r="4" />
                   <path d="M4 20c0-4 3.5-7 8-7s8 3 8 7" />
                 </svg>
               </div>
              <div class="drop-info">
                <div class="drop-name">{{ user.name }}</div>
              </div>
             </div>
             <div class="drop-divider" />
           <div class="drop-meta">
              <div class="meta-row">
                <span class="meta-label">账号</span>
                <span class="meta-value">{{ user.username }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">用户ID</span>
                <span class="meta-value">{{ profile?.id || '-' }}</span>
              </div>
              <div class="meta-row" v-if="profile?.createdAt">
                <span class="meta-label">注册时间</span>
                <span class="meta-value">{{ profile.createdAt.substring(0, 10) }}</span>
              </div>
            </div>
            <div class="drop-divider" />
             <button class="drop-logout" @click="logout">
               <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                 <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                 <polyline points="16 17 21 12 16 7" />
                 <line x1="21" y1="12" x2="9" y2="12" />
               </svg>
               退出登录
             </button>
           </div>
         </Transition>
       </div>
 
       <span class="time">{{ now }}</span>
     </div>
   </header>
 </template>
 
 <style scoped>
 .header-bar {
   height: 56px;
   display: flex;
   align-items: center;
   justify-content: space-between;
   padding: 0 24px;
   background: rgba(10, 15, 23, 0.9);
   border-bottom: 1px solid var(--border-color);
   backdrop-filter: blur(12px);
   flex-shrink: 0;
   z-index: 10;
 }
 .header-left { display: flex; align-items: center; gap: 14px; }
 .logo-icon { color: var(--accent); filter: drop-shadow(0 0 8px var(--accent-glow)); }
 
 .nav-items {
   display: flex;
   gap: 2px;
   background: rgba(56,189,248,0.05);
   border-radius: 8px;
   padding: 2px;
 }
 .nav-item {
   display: flex;
   align-items: center;
   gap: 4px;
   color: var(--text-muted);
   padding: 4px 14px;
   border-radius: 6px;
   cursor: pointer;
   font-size: 12px;
   font-weight: 600;
   transition: all 0.2s;
   text-decoration: none;
 }
 .nav-item:hover { color: var(--text-secondary); }
   .nav-item.active {
     background: rgba(56,189,248,0.15);
     color: var(--accent);
     position: relative;
   }
   .nav-item.active::after {
     content: '';
     position: absolute;
     bottom: -2px;
     left: 50%;
     transform: translateX(-50%);
     width: 60%;
     height: 2px;
     background: var(--accent);
     border-radius: 1px;
     box-shadow: 0 0 6px var(--accent-glow);
   }
 
 .title {
   font-size: 20px;
   font-weight: 700;
   background: var(--gradient-blue);
   -webkit-background-clip: text;
   -webkit-text-fill-color: transparent;
   background-clip: text;
   letter-spacing: 2px;
   white-space: nowrap;
 }
 .header-right { display: flex; align-items: center; gap: 16px; font-size: 14px; color: var(--text-secondary); }
 .status-item { display: flex; align-items: center; }
 .time { font-variant-numeric: tabular-nums; color: var(--accent); font-weight: 500; min-width: 80px; text-align: right; }
 
 /* ==== User avatar + dropdown ==== */
 .user-area {
   display: flex;
   align-items: center;
   gap: 8px;
   position: relative;
 }
 
 .user-label {
   color: var(--text-secondary);
   font-size: 13px;
   font-weight: 500;
 }
 
 .user-btn {
   width: 32px;
   height: 32px;
   border-radius: 50%;
   background: linear-gradient(135deg, rgba(56,189,248,0.12), rgba(139,92,246,0.12));
   border: 1.5px solid rgba(56,189,248,0.25);
   color: var(--accent);
   cursor: pointer;
   display: flex;
   align-items: center;
   justify-content: center;
   transition: all 0.15s;
   padding: 0;
 }
 .user-btn:hover {
   background: linear-gradient(135deg, rgba(56,189,248,0.25), rgba(139,92,246,0.25));
   border-color: rgba(56,189,248,0.5);
 }
 
 .user-dropdown {
   position: absolute;
   top: calc(100% + 8px);
   right: 0;
   width: 220px;
   background: rgb(12, 18, 30);
   border: 1px solid var(--border-color);
   border-radius: 10px;
   box-shadow: 0 8px 30px rgba(0,0,0,0.5);
   overflow: hidden;
   z-index: 100;
 }
 
 .drop-header {
   display: flex;
   align-items: center;
   gap: 12px;
   padding: 16px;
 }
 
 .drop-avatar {
   width: 44px;
   height: 44px;
   border-radius: 50%;
   background: linear-gradient(135deg, rgba(56,189,248,0.12), rgba(139,92,246,0.12));
   border: 2px solid rgba(56,189,248,0.2);
   display: flex;
   align-items: center;
   justify-content: center;
   color: var(--accent);
   flex-shrink: 0;
 }
 
 .drop-info {
   display: flex;
   flex-direction: column;
   gap: 2px;
   min-width: 0;
 }
 .drop-name {
   font-size: 14px;
   font-weight: 700;
   color: var(--text-primary);
 }
 .drop-account {
   font-size: 12px;
   color: var(--text-muted);
 }
 
.drop-divider {
  height: 1px;
  background: var(--border-color);
  margin: 0;
}

.drop-meta {
  padding: 8px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.meta-label {
  color: var(--text-muted);
  font-weight: 500;
}

.meta-value {
  color: var(--text-secondary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.drop-logout {
   display: flex;
   align-items: center;
   gap: 8px;
   width: 100%;
   padding: 12px 16px;
   background: none;
   border: none;
   color: #f87171;
   font-size: 13px;
   font-weight: 600;
   cursor: pointer;
   transition: background 0.15s;
 }
 .drop-logout:hover {
   background: rgba(248,113,113,0.08);
 }
 
 /* Dropdown transition */
 .drop-enter-active {
   transition: opacity 0.15s ease, transform 0.15s ease;
 }
 .drop-leave-active {
   transition: opacity 0.1s ease;
 }
 .drop-enter-from {
   opacity: 0;
   transform: translateY(-4px);
 }
 .drop-leave-to {
   opacity: 0;
 }
 </style>
