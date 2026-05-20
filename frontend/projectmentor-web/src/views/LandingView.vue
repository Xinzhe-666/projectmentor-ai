<template>
  <div class="landing-page">
    <nav class="landing-nav">
      <RouterLink class="landing-brand" to="/">
        <span>PM</span>
        <strong>ProjectMentor AI</strong>
      </RouterLink>
      <div class="nav-actions">
        <el-button text @click="router.push('/login')">登录</el-button>
        <el-button type="primary" :icon="ArrowRight" @click="goStart">立即开始</el-button>
      </div>
    </nav>

    <section class="landing-hero">
      <img class="hero-image" src="@/assets/landing-hero.png" alt="ProjectMentor AI 项目审计工作台" />
      <div class="hero-overlay" />
      <div class="hero-content">
        <p class="eyebrow">AI Project Audit</p>
        <h1>让你的 GitHub 项目经得起面试官深挖</h1>
        <p class="hero-subtitle">
          AI 自动审计 README、代码结构、技术栈和面试风险，识别项目夸大与 AI 幻觉。
        </p>
        <div class="hero-actions">
          <el-button size="large" type="primary" :icon="ArrowRight" @click="goStart">立即开始</el-button>
          <el-button size="large" @click="router.push('/login')">登录</el-button>
        </div>
      </div>
    </section>

    <section class="feature-section">
      <div class="feature-card" v-for="feature in features" :key="feature.title">
        <el-icon :size="26">
          <component :is="feature.icon" />
        </el-icon>
        <h3>{{ feature.title }}</h3>
        <p>{{ feature.description }}</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowRight, ChatLineRound, Coin, DocumentChecked, MagicStick, Search, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const features = [
  {
    title: '项目真实性检测',
    description: '从文件结构、技术栈和证据链判断项目是否站得住。',
    icon: Search
  },
  {
    title: 'README 夸大检测',
    description: '识别高并发、微服务、RAG 等描述是否缺少证据。',
    icon: DocumentChecked
  },
  {
    title: 'AI 幻觉审计',
    description: '检查 AI 生成描述里的过度鼓励和简历风险。',
    icon: Warning
  },
  {
    title: '简历描述优化',
    description: '把项目亮点改写成更可信、更面试友好的表述。',
    icon: MagicStick
  },
  {
    title: '面试深挖',
    description: '围绕项目细节持续追问，提前暴露薄弱点。',
    icon: ChatLineRound
  },
  {
    title: '额度系统',
    description: '清晰记录 AI 审计与深挖能力的使用成本。',
    icon: Coin
  }
]

function goStart() {
  router.push(userStore.isLoggedIn ? '/dashboard' : '/register')
}
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  background: #f6f8fb;
}

.landing-nav {
  position: fixed;
  z-index: 10;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px clamp(18px, 5vw, 72px);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(223, 230, 240, 0.72);
}

.landing-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.landing-brand span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: #162033;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.nav-actions {
  display: flex;
  gap: 8px;
}

.landing-hero {
  position: relative;
  min-height: 82vh;
  padding: 150px clamp(18px, 5vw, 72px) 92px;
  overflow: hidden;
}

.hero-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(246, 248, 251, 0.98) 0%, rgba(246, 248, 251, 0.86) 36%, rgba(246, 248, 251, 0.2) 72%),
    linear-gradient(180deg, rgba(246, 248, 251, 0.2), rgba(246, 248, 251, 0.96));
}

.hero-content {
  position: relative;
  max-width: 680px;
}

.hero-content h1 {
  margin: 12px 0 18px;
  max-width: 640px;
  color: #111827;
  font-size: clamp(42px, 6vw, 76px);
  line-height: 1.04;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 610px;
  margin: 0;
  color: #475467;
  font-size: 20px;
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.feature-section {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  max-width: 1180px;
  margin: -28px auto 0;
  padding: 0 clamp(18px, 5vw, 72px) 70px;
}

.feature-card {
  min-height: 172px;
  padding: 24px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 40px rgba(28, 43, 68, 0.08);
}

.feature-card :deep(.el-icon) {
  color: #1f6feb;
}

.feature-card h3 {
  margin: 16px 0 8px;
  font-size: 18px;
}

.feature-card p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

@media (max-width: 860px) {
  .landing-hero {
    min-height: 78vh;
  }

  .hero-overlay {
    background: rgba(246, 248, 251, 0.84);
  }

  .feature-section {
    grid-template-columns: 1fr;
    margin-top: 0;
  }
}
</style>
