<template>
  <div class="landing-page">
    <nav class="landing-nav">
      <RouterLink class="landing-brand" to="/">
        <span>PM</span>
        <strong>ProjectMentor AI</strong>
      </RouterLink>
      <div class="nav-actions">
        <el-button text :icon="Message" @click="feedbackVisible = true">反馈建议</el-button>
        <el-button text :icon="Coffee" @click="donateVisible = true">请作者喝咖啡</el-button>
        <el-button text @click="router.push('/login')">登录</el-button>
        <el-button type="primary" :icon="ArrowRight" @click="goStart">立即开始</el-button>
      </div>
    </nav>

    <section class="landing-hero">
      <img class="hero-image" src="@/assets/landing-hero.png" alt="ProjectMentor AI 项目审计工作台" />
      <div class="hero-overlay" />
      <div class="hero-content">
        <p class="eyebrow">项目真实性审计 · 简历优化 · 面试深挖</p>
        <h1>ProjectMentor AI</h1>
        <p class="hero-subtitle">
          面向计算机学生、后端实习候选人和 AI 辅助项目开发者的项目真实性审计、简历优化与面试深挖工具。
        </p>
        <div class="hero-actions">
          <el-button size="large" type="primary" :icon="ArrowRight" @click="goStart">立即开始</el-button>
          <el-button size="large" :icon="Message" @click="feedbackVisible = true">反馈建议</el-button>
          <el-button size="large" :icon="Coffee" @click="donateVisible = true">请作者喝咖啡</el-button>
        </div>

        <el-alert
          class="trial-alert"
          title="试用提示"
          type="warning"
          show-icon
          :closable="false"
          description="请勿上传真实商业机密、真实密钥或公司内部代码。ProjectMentor AI 会基于你提供的 README 和项目文件生成辅助分析，结论仅供学习、项目复盘和面试准备参考。"
        />
      </div>
    </section>

    <main class="landing-content">
      <section class="section-block product-position">
        <div class="section-heading">
          <p class="eyebrow">What it helps</p>
          <h2>把项目讲清楚，而不是把项目包装满</h2>
          <p>
            ProjectMentor AI 会把 README、简历描述和项目文件放到同一张证据表里，帮你判断哪些内容能写、哪些表述需要降级、哪些细节需要补课。
          </p>
        </div>
        <div class="position-list">
          <div v-for="point in positionPoints" :key="point" class="position-item">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>{{ point }}</span>
          </div>
        </div>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <p class="eyebrow">For whom</p>
          <h2>适合这些场景</h2>
        </div>
        <div class="card-grid audience-grid">
          <article v-for="audience in audiences" :key="audience.title" class="info-card">
            <el-icon :size="24">
              <component :is="audience.icon" />
            </el-icon>
            <h3>{{ audience.title }}</h3>
            <p>{{ audience.description }}</p>
          </article>
        </div>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <p class="eyebrow">Workflow</p>
          <h2>核心流程</h2>
        </div>
        <div class="flow-grid">
          <article v-for="(step, index) in workflow" :key="step.title" class="flow-card">
            <span class="flow-index">{{ index + 1 }}</span>
            <el-icon :size="24">
              <component :is="step.icon" />
            </el-icon>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </article>
        </div>
      </section>

      <section class="section-block capability-section">
        <div class="section-heading">
          <p class="eyebrow">Trial capabilities</p>
          <h2>当前试用能力</h2>
        </div>
        <div class="capability-grid">
          <div v-for="capability in capabilities" :key="capability" class="capability-pill">
            <el-icon><Check /></el-icon>
            <span>{{ capability }}</span>
          </div>
        </div>
      </section>

      <section class="section-block boundary-section">
        <div class="section-heading">
          <p class="eyebrow">Trial boundary</p>
          <h2>试用版边界</h2>
          <p>它适合做学习复盘和面试准备辅助，不适合替代你对代码与项目事实的最终判断。</p>
        </div>
        <div class="boundary-list">
          <div v-for="boundary in boundaries" :key="boundary" class="boundary-item">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ boundary }}</span>
          </div>
        </div>
      </section>
    </main>

    <DonateDialog v-model="donateVisible" />
    <FeedbackDialog v-model="feedbackVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  ChatDotRound,
  Check,
  CircleCheckFilled,
  Coffee,
  DocumentChecked,
  EditPen,
  Files,
  FolderAdd,
  MagicStick,
  Message,
  Search,
  UploadFilled,
  UserFilled,
  Warning,
  WarningFilled
} from '@element-plus/icons-vue'

import DonateDialog from '@/components/DonateDialog.vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const donateVisible = ref(false)
const feedbackVisible = ref(false)

const positionPoints = [
  '帮你判断项目能不能写进简历',
  '帮你发现 README / 简历中的过度包装',
  '帮你基于代码证据准备面试追问',
  '帮你识别 AI 生成内容里的虚假夸大',
  '帮你把 AI 生成的项目逐步讲成自己真正理解的项目'
]

const audiences = [
  {
    title: '准备后端实习的学生',
    description: '提前看清项目亮点、风险点和可能被追问的实现细节。',
    icon: UserFilled
  },
  {
    title: '使用 AI 做项目但担心不会讲的人',
    description: '把生成出来的功能拆回证据链，补齐自己真正理解的部分。',
    icon: MagicStick
  },
  {
    title: '想优化 README / 简历项目描述的人',
    description: '把夸大的表达改成更可信、更可追问的项目说明。',
    icon: EditPen
  },
  {
    title: '想提前模拟项目深挖面试的人',
    description: '围绕技术选型、实现路径和项目边界持续追问。',
    icon: ChatDotRound
  },
  {
    title: '想检查项目证据不足的人',
    description: '识别硬编码密钥、夸大表述、缺少文件或配置支撑的风险。',
    icon: Warning
  }
]

const workflow = [
  {
    title: '创建项目',
    description: '录入项目名称、技术栈和基础描述。',
    icon: FolderAdd
  },
  {
    title: '粘贴 README 或上传 ZIP',
    description: '补充项目材料，让系统提取可验证的文本证据。',
    icon: UploadFilled
  },
  {
    title: '生成审计报告',
    description: '查看评分、风险、证据链和简历写法建议。',
    icon: DocumentChecked
  },
  {
    title: '复盘证据链与追问',
    description: '围绕简历描述、项目事实和面试问题继续打磨。',
    icon: Search
  }
]

const capabilities = [
  '项目审计',
  'ZIP 上传与证据解析',
  'AI 增强报告',
  'AI 幻觉检测',
  '模拟面试',
  '打印 / 浏览器另存为 PDF',
  '只读分享报告',
  '项目证据链展示',
  '简历描述建议'
]

const boundaries = [
  '本工具不是自动保证简历真实的工具',
  'AI 结论仅供学习和面试准备参考',
  '不建议上传商业机密、真实公司代码、真实密钥',
  '大 ZIP 上传可能较慢',
  '项目仍处于试用迭代阶段',
  '审计结果不代表绝对正确，需要用户结合代码自行判断'
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
  gap: 18px;
  padding: 16px clamp(18px, 5vw, 72px);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(223, 230, 240, 0.72);
}

.landing-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.landing-brand span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: #162033;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
}

.nav-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.landing-hero {
  position: relative;
  min-height: 84vh;
  padding: 148px clamp(18px, 5vw, 72px) 96px;
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
    linear-gradient(90deg, rgba(246, 248, 251, 0.98) 0%, rgba(246, 248, 251, 0.9) 40%, rgba(246, 248, 251, 0.28) 74%),
    linear-gradient(180deg, rgba(246, 248, 251, 0.16), rgba(246, 248, 251, 0.98));
}

.hero-content {
  position: relative;
  max-width: 760px;
}

.hero-content h1 {
  margin: 12px 0 18px;
  max-width: 680px;
  color: #111827;
  font-size: clamp(42px, 6vw, 72px);
  line-height: 1.04;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 720px;
  margin: 0;
  color: #475467;
  font-size: 20px;
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 30px;
  flex-wrap: wrap;
}

.trial-alert {
  max-width: 720px;
  margin-top: 24px;
  border-radius: 8px;
}

.landing-content {
  max-width: 1180px;
  margin: -30px auto 0;
  padding: 0 clamp(18px, 5vw, 72px) 72px;
  position: relative;
}

.section-block {
  margin-top: 20px;
  padding: 26px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 40px rgba(28, 43, 68, 0.08);
}

.product-position {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(320px, 1.05fr);
  gap: 28px;
  align-items: center;
}

.section-heading h2 {
  margin: 8px 0 10px;
  color: #111827;
  font-size: 30px;
  line-height: 1.25;
  letter-spacing: 0;
}

.section-heading p:last-child {
  margin: 0;
  color: #667085;
  line-height: 1.8;
}

.position-list {
  display: grid;
  gap: 12px;
}

.position-item,
.boundary-item,
.capability-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.position-item {
  padding: 12px 14px;
  border-radius: 8px;
  background: #f4f8ff;
  color: #344054;
}

.position-item :deep(.el-icon),
.capability-pill :deep(.el-icon) {
  flex: 0 0 auto;
  color: #14b8a6;
}

.card-grid {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.audience-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.info-card,
.flow-card {
  min-width: 0;
  min-height: 178px;
  padding: 18px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background: #fbfdff;
}

.info-card :deep(.el-icon),
.flow-card :deep(.el-icon) {
  color: #1f6feb;
}

.info-card h3,
.flow-card h3 {
  margin: 14px 0 8px;
  color: #111827;
  font-size: 17px;
  line-height: 1.35;
}

.info-card p,
.flow-card p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.flow-card {
  position: relative;
}

.flow-index {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  margin-bottom: 12px;
  border-radius: 8px;
  background: #162033;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.capability-pill {
  min-height: 46px;
  padding: 11px 14px;
  border: 1px solid rgba(20, 184, 166, 0.22);
  border-radius: 8px;
  background: #f0fbf9;
  color: #344054;
}

.boundary-section {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(320px, 1.1fr);
  gap: 28px;
}

.boundary-list {
  display: grid;
  gap: 12px;
}

.boundary-item {
  align-items: flex-start;
  padding: 12px 14px;
  border: 1px solid rgba(245, 158, 11, 0.24);
  border-radius: 8px;
  background: #fffbeb;
  color: #344054;
  line-height: 1.65;
}

.boundary-item :deep(.el-icon) {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #f59e0b;
}

@media (max-width: 980px) {
  .product-position,
  .boundary-section {
    grid-template-columns: 1fr;
  }

  .audience-grid,
  .flow-grid,
  .capability-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .landing-nav {
    position: static;
    align-items: flex-start;
    flex-direction: column;
  }

  .nav-actions {
    justify-content: flex-start;
  }

  .landing-hero {
    min-height: auto;
    padding-top: 64px;
  }

  .hero-overlay {
    background: rgba(246, 248, 251, 0.88);
  }

  .landing-content {
    margin-top: 0;
  }

  .section-block {
    padding: 20px;
  }

  .audience-grid,
  .flow-grid,
  .capability-grid {
    grid-template-columns: 1fr;
  }
}
</style>
