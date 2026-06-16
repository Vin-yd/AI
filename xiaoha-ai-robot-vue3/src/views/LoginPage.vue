<template>
  <div class="login-page">
    <!-- 光晕背景 -->
    <div class="glow-orb blue"></div>
    <div class="glow-orb purple"></div>

    <!-- 装饰环 -->
    <div class="deco-ring r1"></div>
    <div class="deco-ring r2"></div>

    <!-- 扫描线 -->
    <div class="scan-line"></div>

    <!-- 四角呼吸灯 -->
    <div class="corner-deco tl"><span v-for="i in 3" :key="i" class="corner-dot" :style="{ animationDelay: (i-1)*0.5+'s' }"></span></div>
    <div class="corner-deco tr"><span v-for="i in 3" :key="i" class="corner-dot" :style="{ animationDelay: (i-1)*0.5+'s' }"></span></div>
    <div class="corner-deco bl"><span v-for="i in 3" :key="i" class="corner-dot" :style="{ animationDelay: (i-1)*0.5+'s' }"></span></div>
    <div class="corner-deco br"><span v-for="i in 3" :key="i" class="corner-dot" :style="{ animationDelay: (i-1)*0.5+'s' }"></span></div>

    <!-- 粒子画布 -->
    <canvas ref="canvasRef" class="particle-canvas"></canvas>

    <!-- 登录卡片 -->
    <div class="login-card">
      <span class="version-tag">v2.0.1</span>

      <div class="card-header">
        <div class="logo-ring" @click="onLogoClick">⚡</div>
        <h1>Vin-AI Robot</h1>
        <p>开启智能对话之旅</p>
        <div class="status-bar">
          <span class="status-dot"></span>
          <span class="status-text">SYSTEM ONLINE</span>
        </div>
      </div>

      <div class="input-group">
        <label class="input-label">手机号</label>
        <input
          class="input-field"
          type="tel"
          placeholder="请输入手机号"
          maxlength="11"
          v-model="phone"
          @keyup.enter="focusCode"
        />
      </div>

      <div class="input-group">
        <label class="input-label">验证码</label>
        <div class="code-row">
          <input
            ref="codeInputRef"
            class="input-field"
            type="text"
            placeholder="输入 5 位验证码"
            maxlength="5"
            v-model="code"
            @keyup.enter="handleLogin"
          />
          <button class="send-code-btn" :disabled="countdown > 0" @click="sendCode">
            {{ countdown > 0 ? countdown + 's 后重试' : '获取验证码' }}
          </button>
        </div>
      </div>

      <button class="login-btn" @click="handleLogin" :disabled="loading">
        {{ loading ? '登录中...' : '登 录 / 注 册' }}
      </button>

      <div class="divider"><span>AI 智能助手</span></div>

      <p class="footer-text">
        登录即表示同意 <a href="#">服务协议</a> 和 <a href="#">隐私政策</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/userStore'
import { sendSmsCode } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const phone = ref('')
const code = ref('')
const countdown = ref(0)
const loading = ref(false)
const codeInputRef = ref(null)
const canvasRef = ref(null)

// ========== 验证码逻辑 ==========
let countdownTimer = null
async function sendCode() {
  if (countdown.value > 0) return
  if (!phone.value || phone.value.length < 11) {
    message.warning('请输入正确的手机号')
    return
  }
  try {
    const res = await sendSmsCode(phone.value)
    if (res.data.success) {
      message.success('验证码已发送')
      countdown.value = 60
      countdownTimer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(countdownTimer)
        }
      }, 1000)
    } else {
      message.error(res.data.message || '发送失败')
    }
  } catch {
    message.error('发送失败，请稍后重试')
  }
}

// ========== 登录逻辑 ==========
async function handleLogin() {
  if (!phone.value || !code.value) {
    message.warning('请填写手机号和验证码')
    return
  }
  loading.value = true
  try {
    await userStore.login(phone.value, code.value)
    message.success('登录成功')
    router.push('/')
  } catch (e) {
    message.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function focusCode() {
  codeInputRef.value?.focus()
}

// Logo 点击爆发
function onLogoClick() {
  // 视觉反馈由 CSS :active 处理
}

// ========== 粒子系统 ==========
let animationId, w, h
let particles = []
let burstParticles = []
let mouseX = -1000, mouseY = -1000
let clickX = 0, clickY = 0, clickTime = 0

class Particle {
  constructor() {
    this.reset()
    this.y = Math.random() * h
  }
  reset() {
    this.x = Math.random() * w
    this.y = -10
    this.baseSize = 1 + Math.random() * 2
    this.size = this.baseSize
    this.baseSpeed = 0.3 + Math.random() * 0.8
    this.speed = this.baseSpeed
    this.opacity = 0.2 + Math.random() * 0.5
    this.wave = Math.random() * Math.PI * 2
    this.glowRadius = 0
  }
  update() {
    const dx = this.x - mouseX
    const dy = this.y - mouseY
    const dist = Math.sqrt(dx * dx + dy * dy)
    const influenceRadius = 150
    this.glowRadius = 0

    if (dist < influenceRadius) {
      const force = 1 - dist / influenceRadius
      this.x += (dx / dist) * force * 1.2
      this.y += (dy / dist) * force * 1.2
      this.size = this.baseSize * (1 + force * 2.5)
      this.opacity = Math.min(1, 0.2 + force * 0.8)
      this.speed = this.baseSpeed * (1 + force * 3)
      this.glowRadius = force * 8
    } else {
      this.size += (this.baseSize - this.size) * 0.05
      this.speed += (this.baseSpeed - this.speed) * 0.05
    }

    this.y += this.speed
    this.x += Math.sin(this.wave + this.y * 0.01) * 0.3

    // 点击冲击波
    if (clickTime > 0) {
      const timeSince = performance.now() - clickTime
      if (timeSince < 800) {
        const cdx = this.x - clickX
        const cdy = this.y - clickY
        const cdist = Math.sqrt(cdx * cdx + cdy * cdy)
        const waveRadius = timeSince * 0.5
        const band = 30
        if (Math.abs(cdist - waveRadius) < band) {
          const intensity = 1 - Math.abs(cdist - waveRadius) / band
          this.x += (cdx / cdist) * intensity * 15
          this.y += (cdy / cdist) * intensity * 15
          this.opacity = Math.min(1, this.opacity + intensity * 0.6)
          this.size = this.baseSize * (1 + intensity * 4)
          this.glowRadius = intensity * 12
        }
      }
    }

    if (this.y > h + 10) { this.reset(); this.y = -10 }
    if (this.x < -10) this.x = w + 10
    if (this.x > w + 10) this.x = -10
  }
  draw(ctx) {
    if (this.glowRadius > 0.5) {
      const glow = ctx.createRadialGradient(this.x, this.y, 0, this.x, this.y, this.glowRadius)
      glow.addColorStop(0, `rgba(139, 146, 255, ${this.opacity * 0.6})`)
      glow.addColorStop(1, 'rgba(139, 146, 255, 0)')
      ctx.fillStyle = glow
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.glowRadius, 0, Math.PI * 2)
      ctx.fill()
    }
    ctx.beginPath()
    ctx.fillStyle = `rgba(139, 146, 255, ${this.opacity})`
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fill()
  }
}

class BurstParticle {
  constructor(x, y) {
    this.x = x; this.y = y
    const angle = Math.random() * Math.PI * 2
    const speed = 2 + Math.random() * 6
    this.vx = Math.cos(angle) * speed
    this.vy = Math.sin(angle) * speed
    this.size = 1 + Math.random() * 2.5
    this.opacity = 1
    this.life = 0
    this.maxLife = 0.5 + Math.random() * 1
  }
  update(dt) {
    this.x += this.vx; this.y += this.vy
    this.vx *= 0.96; this.vy *= 0.96
    this.life += dt
    this.opacity = Math.max(0, 1 - this.life / this.maxLife)
  }
  draw(ctx) {
    ctx.beginPath()
    ctx.fillStyle = `rgba(180, 180, 255, ${this.opacity})`
    ctx.arc(this.x, this.y, this.size * this.opacity, 0, Math.PI * 2)
    ctx.fill()
  }
  get dead() { return this.life >= this.maxLife }
}

function drawLines(ctx) {
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 120) {
        const lineOpacity = 0.08 * (1 - dist / 120)
        const midX = (particles[i].x + particles[j].x) / 2
        const midY = (particles[i].y + particles[j].y) / 2
        const mouseDist = Math.sqrt((midX - mouseX) ** 2 + (midY - mouseY) ** 2)
        const brightness = mouseDist < 180 ? 1 + (1 - mouseDist / 180) * 2 : 1
        ctx.strokeStyle = `rgba(139, 146, 255, ${lineOpacity * brightness})`
        ctx.lineWidth = 0.5
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.stroke()
      }
    }
  }
}

function onMouseMove(e) {
  mouseX = e.clientX
  mouseY = e.clientY
}
function onMouseLeave() {
  mouseX = -1000
  mouseY = -1000
}
function onClick(e) {
  clickX = e.clientX
  clickY = e.clientY
  clickTime = performance.now()
  for (let i = 0; i < 20; i++) {
    burstParticles.push(new BurstParticle(clickX, clickY))
  }
}

function animate(timestamp) {
  const dt = (lastTime ? (timestamp - lastTime) / 1000 : 0.016)
  lastTime = timestamp

  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, w, h)

  burstParticles.forEach(p => p.update(dt))
  burstParticles = burstParticles.filter(p => !p.dead)
  particles.forEach(p => p.update())
  drawLines(ctx)
  burstParticles.forEach(p => p.draw(ctx))
  particles.forEach(p => p.draw(ctx))

  animationId = requestAnimationFrame(animate)
}
let lastTime = 0

onMounted(() => {
  const canvas = canvasRef.value
  w = canvas.width = window.innerWidth
  h = canvas.height = window.innerHeight
  for (let i = 0; i < 100; i++) particles.push(new Particle())

  window.addEventListener('resize', () => {
    w = canvas.width = window.innerWidth
    h = canvas.height = window.innerHeight
  })
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseleave', onMouseLeave)
  window.addEventListener('click', onClick)

  animationId = requestAnimationFrame(animate)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseleave', onMouseLeave)
  window.removeEventListener('click', onClick)
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.login-page {
  position: fixed; inset: 0;
  display: flex; align-items: center; justify-content: center;
  background: #0a0a0f;
  overflow: hidden;
}

/* 光晕 */
.glow-orb {
  position: fixed; border-radius: 50%;
  filter: blur(120px); opacity: 0.3; z-index: 0; pointer-events: none;
}
.glow-orb.blue {
  width: 400px; height: 400px;
  background: #4d6bfe;
  top: -100px; right: -100px;
  animation: orbDrift1 8s ease-in-out infinite;
}
.glow-orb.purple {
  width: 300px; height: 300px;
  background: #8b5cf6;
  bottom: -80px; left: -80px;
  animation: orbDrift2 10s ease-in-out infinite;
}
@keyframes orbDrift1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-60px, 40px); }
}
@keyframes orbDrift2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(50px, -30px); }
}

/* 装饰环 */
.deco-ring {
  position: fixed; border-radius: 50%; z-index: 0; pointer-events: none;
}
.deco-ring.r1 {
  width: 600px; height: 600px; top: 50%; left: 50%; margin-left: -300px; margin-top: -300px;
  border: 1px solid rgba(77,107,254,0.08);
  animation: ringSpin 30s linear infinite;
}
.deco-ring.r2 {
  width: 800px; height: 800px; top: 50%; left: 50%; margin-left: -400px; margin-top: -400px;
  border: 1px dashed rgba(139,92,246,0.05);
  animation: ringSpin 45s linear infinite reverse;
}
@keyframes ringSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 扫描线 */
.scan-line {
  position: fixed; top: 0; left: 0; width: 100%; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(77,107,254,0.15), transparent);
  z-index: 1; pointer-events: none;
  animation: scanDown 8s linear infinite;
}
@keyframes scanDown {
  0% { top: -2px; }
  100% { top: 100vh; }
}

/* 四角呼吸灯 */
.corner-deco { position: fixed; z-index: 1; pointer-events: none; }
.corner-deco.tl { top: 24px; left: 24px; }
.corner-deco.tr { top: 24px; right: 24px; }
.corner-deco.bl { bottom: 24px; left: 24px; }
.corner-deco.br { bottom: 24px; right: 24px; }
.corner-dot {
  display: inline-block; width: 4px; height: 4px;
  background: #4d6bfe; border-radius: 50%; margin: 8px;
  animation: dotBlink 2s ease-in-out infinite;
  opacity: 0.3;
}
@keyframes dotBlink {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

/* 粒子画布 */
.particle-canvas {
  position: fixed; top: 0; left: 0;
  width: 100%; height: 100%; z-index: 0;
}

/* 卡片 */
.login-card {
  position: relative; z-index: 2;
  width: 420px; max-width: 90vw;
  background: rgba(20,20,35,0.85);
  backdrop-filter: blur(30px);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 24px;
  padding: 44px 36px;
  transition: border-color 0.5s;
  animation: cardIn 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.login-card:hover { border-color: rgba(77,107,254,0.2); }
@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.version-tag {
  position: absolute; top: 16px; right: 20px;
  font-size: 10px; color: #475569;
  letter-spacing: 1.5px; font-family: 'Courier New', monospace;
}

.card-header { text-align: center; margin-bottom: 32px; }
.logo-ring {
  width: 64px; height: 64px; border-radius: 20px;
  background: linear-gradient(135deg, #4d6bfe, #8b5cf6);
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 32px; margin-bottom: 16px;
  box-shadow: 0 0 40px rgba(77,107,254,0.3);
  animation: logoPulse 3s ease-in-out infinite;
  cursor: pointer; transition: transform 0.3s;
}
.logo-ring:hover { transform: scale(1.1); }
.logo-ring:active { animation: logoBurst 0.3s ease-out; }
@keyframes logoBurst {
  0% { transform: scale(1); box-shadow: 0 0 40px rgba(77,107,254,0.3); }
  50% { transform: scale(1.3); box-shadow: 0 0 80px rgba(139,92,246,0.8); }
  100% { transform: scale(1); box-shadow: 0 0 40px rgba(77,107,254,0.3); }
}
@keyframes logoPulse {
  0%, 100% { box-shadow: 0 0 30px rgba(77,107,254,0.3); }
  50% { box-shadow: 0 0 60px rgba(77,107,254,0.5); }
}
.card-header h1 { font-size: 24px; font-weight: 700; color: #f1f5f9; margin-top: 4px; }
.card-header p { font-size: 14px; color: #94a3b8; margin-top: 4px; }

.status-bar { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 8px; }
.status-dot {
  width: 6px; height: 6px; background: #22c55e; border-radius: 50%;
  animation: statusPulse 2s ease-in-out infinite;
}
@keyframes statusPulse {
  0%, 100% { opacity: 1; box-shadow: 0 0 6px #22c55e; }
  50% { opacity: 0.4; box-shadow: 0 0 2px #22c55e; }
}
.status-text { font-size: 11px; color: #64748b; letter-spacing: 1px; }

.input-group { margin-bottom: 18px; }
.input-label {
  display: block; font-size: 12px; font-weight: 500;
  color: #94a3b8; letter-spacing: 0.5px;
  margin-bottom: 8px; text-transform: uppercase;
}
.input-field {
  width: 100%; padding: 14px 16px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12px;
  font-size: 15px; color: #f1f5f9;
  outline: none; transition: all 0.3s;
  font-family: inherit;
}
.input-field::placeholder { color: #475569; }
.input-field:focus {
  border-color: #4d6bfe;
  background: rgba(77,107,254,0.06);
  box-shadow: 0 0 0 3px rgba(77,107,254,0.15), 0 0 30px rgba(77,107,254,0.08);
}

.code-row { display: flex; gap: 10px; }
.code-row .input-field { flex: 1; }
.send-code-btn {
  padding: 0 18px;
  border: 1px solid rgba(77,107,254,0.4);
  background: rgba(77,107,254,0.1);
  color: #8fa8ff; border-radius: 12px;
  font-size: 13px; font-weight: 500;
  cursor: pointer; white-space: nowrap;
  transition: all 0.3s; font-family: inherit;
  min-width: 105px; position: relative; overflow: hidden;
}
.send-code-btn::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(135deg, transparent 40%, rgba(255,255,255,0.08) 50%, transparent 60%);
  transform: translateX(-100%); transition: transform 0.6s;
}
.send-code-btn:hover::after { transform: translateX(100%); }
.send-code-btn:hover {
  background: rgba(77,107,254,0.18);
  border-color: rgba(77,107,254,0.6);
  color: #b4c2ff;
}
.send-code-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.login-btn {
  width: 100%; padding: 15px;
  border: none;
  background: linear-gradient(135deg, #4d6bfe, #8b5cf6);
  color: white; border-radius: 12px;
  font-size: 16px; font-weight: 600;
  cursor: pointer; margin-top: 8px;
  transition: all 0.3s; font-family: inherit;
  letter-spacing: 2px;
  position: relative; overflow: hidden;
}
.login-btn::after {
  content: ''; position: absolute;
  top: 0; left: -100%; width: 100%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  transition: left 0.5s;
}
.login-btn:hover::after { left: 100%; }
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(77,107,254,0.4);
}
.login-btn:active { transform: translateY(0) scale(0.98); }
.login-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.divider {
  display: flex; align-items: center;
  margin: 24px 0; gap: 12px;
}
.divider::before, .divider::after {
  content: ''; flex: 1; height: 1px;
  background: rgba(255,255,255,0.06);
}
.divider span { font-size: 12px; color: #475569; letter-spacing: 1px; }

.footer-text { text-align: center; margin-top: 20px; font-size: 12px; color: #475569; }
.footer-text a { color: #8fa8ff; text-decoration: none; transition: color 0.2s; }
.footer-text a:hover { color: #b4c2ff; }

/* 移动端适配 */
@media (max-width: 480px) {
  .login-card { padding: 32px 24px; border-radius: 20px; }
  .card-header h1 { font-size: 20px; }
  .code-row { flex-direction: column; }
  .send-code-btn { padding: 12px; min-width: auto; text-align: center; }
}
</style>
