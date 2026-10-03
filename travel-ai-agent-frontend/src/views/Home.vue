<template>
  <div class="home-container">
    <div class="header">
      <h1 class="main-title">AI 智能体应用平台</h1>
      <p class="subtitle">/ 旅游规划 · 专业问答 · 一站式 AI 体验 /</p>
      <div class="cyber-line"></div>
    </div>

    <div class="apps-container">
      <div class="app-card" @click="navigateTo('/travel-master')">
        <div class="card-glow"></div>
        <div class="app-icon travel-icon">✈️</div>
        <div class="app-info">
          <div class="app-title">AI旅游管理大师</div>
          <div class="app-desc">旅游指导专家，帮你制定旅游方案</div>
        </div>
        <div class="app-button">
          <span class="btn-text">立即体验</span>
          <span class="btn-icon">→</span>
        </div>
      </div>

      <div class="app-card" @click="navigateTo('/super-agent')">
        <div class="card-glow"></div>
        <div class="app-icon robot-icon">🤖</div>
        <div class="app-info">
          <div class="app-title">AI超级智能体</div>
          <div class="app-desc">全能型AI助手，解决各类专业问题</div>
        </div>
        <div class="app-button">
          <span class="btn-text">立即体验</span>
          <span class="btn-icon">→</span>
        </div>
      </div>
    </div>

    <div class="cyber-circles">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'

// 设置页面标题和元数据
useHead({
  title: 'AI 智能体应用平台 - 首页',
  meta: [
    {
      name: 'description',
      content: 'AI 智能体应用平台提供 AI 旅游管理大师和 AI 超级智能体服务，满足您的各种 AI 对话需求'
    },
    {
      name: 'keywords',
      content: 'AI智能体,AI应用,AI旅游管理大师,AI助手,智能对话,AI超级智能体,首页'
    }
  ]
})

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
/* 白蓝清爽主题 */
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: linear-gradient(160deg, #ffffff 0%, #eef5ff 55%, #dcebff 100%);
  position: relative;
  overflow: hidden;
}

/* 标题 */
.header {
  padding: 70px 20px 50px;
  text-align: center;
  position: relative;
  z-index: 2;
}

.main-title {
  font-size: 3rem;
  font-weight: 700;
  background: linear-gradient(120deg, #1d4ed8, #3b82f6 55%, #06b6d4);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 4px;
  margin-bottom: 20px;
}

.subtitle {
  font-size: 1.1rem;
  color: #64748b;
  max-width: 600px;
  margin: 0 auto 24px;
  letter-spacing: 3px;
}

.cyber-line {
  height: 2px;
  width: 80%;
  max-width: 600px;
  margin: 0 auto;
  background: linear-gradient(90deg, transparent, #3b82f6, transparent);
  position: relative;
}

.cyber-line::before,
.cyber-line::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 8px;
  height: 8px;
  background-color: #3b82f6;
  border-radius: 50%;
  transform: translateY(-50%);
  box-shadow: 0 0 8px 2px rgba(59, 130, 246, 0.5);
}

.cyber-line::before {
  left: 20%;
}

.cyber-line::after {
  right: 20%;
}

.apps-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 50px;
  max-width: 1200px;
  margin: 60px auto;
  padding: 0 20px;
  flex: 1;
  position: relative;
  z-index: 2;
}

.app-card {
  width: 340px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow:
    0 8px 30px rgba(37, 99, 235, 0.12),
    inset 0 0 0 1px rgba(37, 99, 235, 0.08);
  padding: 30px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle at center,
    rgba(59, 130, 246, 0.1) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.5s;
  pointer-events: none;
}

.app-card:hover {
  transform: translateY(-12px) scale(1.03);
  box-shadow:
    0 15px 45px rgba(37, 99, 235, 0.25),
    inset 0 0 0 1px rgba(37, 99, 235, 0.35);
}

.app-card:hover .card-glow {
  opacity: 1;
}

.app-icon {
  font-size: 3.5rem;
  margin-bottom: 25px;
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  position: relative;
  z-index: 1;
}

.travel-icon {
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.35);
}

.robot-icon {
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  box-shadow: 0 0 20px rgba(37, 99, 235, 0.35);
}

.app-info {
  text-align: center;
  margin-bottom: 30px;
  width: 100%;
}

.app-title {
  font-size: 1.5rem;
  font-weight: bold;
  color: #1e3a8a;
  margin-bottom: 12px;
}

.app-desc {
  font-size: 1rem;
  color: #64748b;
  line-height: 1.6;
}

.app-button {
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  color: white;
  padding: 12px 28px;
  border-radius: 30px;
  font-weight: 500;
  transition: all 0.3s;
  margin-top: auto;
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(37, 99, 235, 0.3);
}

.app-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.25), transparent);
  transition: left 0.7s;
}

.app-button:hover {
  box-shadow: 0 0 18px rgba(37, 99, 235, 0.45);
  transform: scale(1.05);
}

.app-button:hover::before {
  left: 100%;
}

.btn-text {
  margin-right: 8px;
  letter-spacing: 1px;
}

.btn-icon {
  font-size: 1.2rem;
  transition: transform 0.3s;
}

.app-button:hover .btn-icon {
  transform: translateX(4px);
}

/* 背景装饰圆圈 */
.cyber-circles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.35;
  filter: blur(40px);
}

.circle-1 {
  width: 320px;
  height: 320px;
  top: -120px;
  right: -100px;
  background: linear-gradient(135deg, #93c5fd, #67e8f9);
  animation: float 15s infinite alternate;
}

.circle-2 {
  width: 500px;
  height: 500px;
  bottom: -220px;
  left: -200px;
  background: linear-gradient(135deg, #bfdbfe, #a5b4fc);
  animation: float 20s infinite alternate-reverse;
}

.circle-3 {
  width: 220px;
  height: 220px;
  top: 40%;
  right: 15%;
  background: linear-gradient(135deg, #bae6fd, #93c5fd);
  animation: float 12s infinite alternate;
}

@keyframes float {
  0% {
    transform: translate(0, 0) rotate(0deg);
  }
  100% {
    transform: translate(50px, 50px) rotate(10deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-title {
    font-size: 2.4rem;
  }

  .subtitle {
    font-size: 1rem;
  }

  .apps-container {
    gap: 30px;
    margin: 40px auto;
  }

  .app-card {
    width: 100%;
    max-width: 420px;
    padding: 25px;
  }

  .app-icon {
    font-size: 3rem;
    width: 80px;
    height: 80px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 50px 15px 40px;
  }

  .main-title {
    font-size: 2rem;
    letter-spacing: 2px;
  }

  .subtitle {
    font-size: 0.9rem;
    letter-spacing: 2px;
  }

  .apps-container {
    margin: 30px auto;
    padding: 0 15px;
  }

  .app-card {
    padding: 20px;
  }

  .app-icon {
    font-size: 2.6rem;
    margin-bottom: 20px;
    width: 70px;
    height: 70px;
  }

  .app-title {
    font-size: 1.3rem;
  }

  .app-desc {
    font-size: 0.9rem;
  }

  .circle-1, .circle-2, .circle-3 {
    opacity: 0.25;
  }
}
</style>
