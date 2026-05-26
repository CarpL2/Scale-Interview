<template>
  <div class="login-page" v-if="!currentUser.userId">
    <div class="login-shell">
      <div class="login-brand">
        <div class="login-logo">
          <i class="fa-solid fa-bolt"></i>
        </div>
        <div>
          <h1>Scale Interview</h1>
          <p>鱼鳞面试官</p>
        </div>
      </div>
      <div class="login-panel">
        <div class="login-title">登录工作台</div>
        <el-input v-model="authForm.username" placeholder="用户名" size="large" @keyup.enter="submitAuth"></el-input>
        <el-input v-model="authForm.password" placeholder="密码" type="password" show-password size="large" @keyup.enter="submitAuth"></el-input>
        <el-button type="primary" class="login-submit" :loading="isAuthLoading" @click="submitAuth">
          登录
        </el-button>
        <button class="register-link" @click="openAuthDialog('register')">注册新账号</button>
      </div>
    </div>

    <el-dialog v-model="authDialogVisible" title="注册" width="420px" custom-class="glass-dialog">
      <div class="auth-form">
        <el-input v-model="authForm.username" placeholder="用户名" clearable></el-input>
        <el-input v-model="authForm.password" placeholder="密码" type="password" show-password></el-input>
        <el-button type="primary" class="auth-submit" :loading="isAuthLoading" @click="submitAuth">
          注册
        </el-button>
      </div>
    </el-dialog>
  </div>

  <div class="app-layout" v-else>
    <!-- 左侧菜单栏 -->
    <div class="sidebar">
      <div class="logo-section">
        <div class="logo-circle">
          <i class="fa-solid fa-user-astronaut"></i>
        </div>
        <span class="logo-text">Scale Interview</span>
        <span class="logo-sub">鱼鳞面试官工作台</span>
      </div>

      <div class="user-panel">
        <div class="user-meta">
          <span class="user-name">{{ currentUser.username || '未登录' }}</span>
          <span class="user-id">ID: {{ currentUser.userId || '-' }}</span>
        </div>
        <div class="user-actions">
          <el-button size="small" link type="primary" @click="logout">退出</el-button>
          <el-button size="small" link type="primary" @click="openAuthDialog('register')">注册</el-button>
        </div>
      </div>
      
      <div class="menu-section">
        <el-divider>模式切换</el-divider>
        <div class="mode-toggle">
          <div :class="['mode-item', currentMode === 'interview' ? 'active' : '']" @click="setMode('interview')">
            <i class="fa-solid fa-microphone-lines"></i> 模拟面试
          </div>
          <div :class="['mode-item', currentMode === 'tutor' ? 'active' : '']" @click="setMode('tutor')">
            <i class="fa-solid fa-graduation-cap"></i> 知识导师
          </div>
        </div>

        <el-divider>我的资料</el-divider>
        <el-upload
          class="upload-demo"
          :action="`/api/api/profile/jd/upload?userId=${currentUser.userId}&sessionId=${sessionId}`"
          :on-success="onUploadSuccess"
          :on-error="onUploadError"
          :show-file-list="false"
          :disabled="!currentUser.userId"
        >
          <el-button class="sidebar-btn"><i class="fa-solid fa-file-lines"></i> 上传岗位 JD</el-button>
        </el-upload>
        <el-button class="sidebar-btn" @click="openMaterialsDialog">
          <i class="fa-solid fa-eye"></i> 查看 JD / 简历
        </el-button>
        <el-upload
          class="upload-demo"
          :action="`/api/api/profile/resume/upload?userId=${currentUser.userId}&sessionId=${sessionId}`"
          :on-success="onUploadSuccess"
          :on-error="onUploadError"
          :show-file-list="false"
          :disabled="!currentUser.userId"
        >
          <el-button class="sidebar-btn"><i class="fa-solid fa-user-tie"></i> 上传个人简历</el-button>
        </el-upload>

        <el-divider>面试配置</el-divider>
        <el-select v-model="selectedStyle" placeholder="面试风格" @change="onStyleChange" class="style-select">
          <el-option label="专业标准面" value="PROFESSIONAL"></el-option>
          <el-option label="严厉压力面" value="STERN"></el-option>
          <el-option label="鼓励引导面" value="ENCOURAGING"></el-option>
        </el-select>

        <el-button class="sidebar-btn danger-btn" @click="clearKnowledge">
          <i class="fa-solid fa-trash-can"></i> 清空面试题库
        </el-button>

        <el-upload
          class="upload-demo"
          :action="`/api/api/knowledge/upload?userId=${currentUser.userId}`"
          :on-success="onKnowledgeUploadSuccess"
          :on-error="onUploadError"
          :show-file-list="false"
        >
          <el-button class="sidebar-btn warning-btn"><i class="fa-solid fa-database"></i> 喂给 AI 题库</el-button>
        </el-upload>
        <el-button class="sidebar-btn" @click="openKnowledgeDialog">
          <i class="fa-solid fa-magnifying-glass"></i> 查看题库内容
        </el-button>
      </div>

      <div class="bottom-actions">
        <el-button class="sidebar-btn primary-btn" @click="newChat">
          <i class="fa-solid fa-rotate-right"></i> 新的一局
        </el-button>
      </div>
    </div>

    <!-- 中间聊天区 -->
    <div class="chat-room">
      <div class="chat-header">
        <div class="status-indicator">
          <span class="status-dot"></span>
          <span class="status-text">{{ currentMode === 'interview' ? '鱼鳞面试官在线' : 'Tutor 导师在线' }}</span>
        </div>
        <div class="header-actions">
          <el-button size="small" type="success" plain @click="generateReport" :loading="isGeneratingReport">
            <i class="fa-solid fa-flag-checkered"></i> 结束并生成报告
          </el-button>
        </div>
      </div>

      <div class="message-list" ref="messaggListRef">
        <div
          v-for="(message, index) in displayMessages"
          :key="index"
          :class="['message-wrapper', message.isUser ? 'user-wrapper' : 'bot-wrapper']"
        >
          <div class="avatar" v-if="!message.isUser">
            <i class="fa-solid fa-robot"></i>
          </div>
          <div :class="['message-bubble', message.isUser ? 'user-bubble' : 'bot-bubble']">
            <span class="markdown-body" v-html="renderMarkdown(message.content)"></span>
            <div class="loading-wave" v-if="message.isThinking || message.isTyping">
              <span></span><span></span><span></span>
            </div>
          </div>
          <div class="avatar user-avatar" v-if="message.isUser">
            <i class="fa-solid fa-user"></i>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-glass">
          <el-input 
            v-model="inputMessage" 
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 5 }"
            placeholder="与面试官对话... (Enter 发送，Shift+Enter 换行)" 
            @keydown.enter="handleInputEnter"
            :disabled="isSending"
          ></el-input>
          <button
            :class="['voice-btn', isListening ? 'listening' : '']"
            @click="toggleSpeechInput"
            :disabled="isSending"
            title="语音输入"
          >
            <i class="fa-solid fa-microphone"></i>
          </button>
          <button class="send-btn" @click="sendMessage" :disabled="isSending || !inputMessage.trim()">
            <i class="fa-solid fa-paper-plane"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- 右侧情报面板 -->
    <div class="dashboard">
      <div class="dashboard-header">
        <h3>情报面板</h3>
      </div>
      
      <div class="dashboard-content">
        <!-- Agent 决策摘要 -->
        <div class="panel-card decision-card" v-if="currentMode === 'interview'">
          <div class="card-title"><i class="fa-solid fa-route"></i> Agent 决策摘要</div>
          <div v-if="agentDecision && agentDecision.turnIndex">
            <div class="decision-row">
              <span>当前阶段</span>
              <strong>{{ displayStageLabel(agentDecision.currentStage) }}</strong>
            </div>
            <div class="decision-row">
              <span>工具状态</span>
              <strong>{{ toolStatusLabel(agentDecision.toolStatus) }}</strong>
            </div>
            <div class="decision-reason">
              {{ agentDecision.decisionSummary || '暂无决策摘要' }}
            </div>
          </div>
          <div v-else class="empty-tip">等待首轮决策</div>
        </div>

        <!-- 雷达图展示区 -->
        <div class="panel-card chart-card">
          <div class="card-title"><i class="fa-solid fa-chart-pie"></i> 能力画像</div>
          <div class="chart-container">
            <v-chart class="radar-chart" :option="chartOption" autoresize />
          </div>
          <el-button size="small" type="warning" plain class="full-width-btn" @click="enrichProfile" :loading="isEnriching" v-if="currentMode === 'tutor'">
            <i class="fa-solid fa-bolt"></i> 更新能力画像
          </el-button>
        </div>

        <div class="panel-card metrics-card">
          <div class="card-title" style="display: flex; justify-content: space-between;">
            <span><i class="fa-solid fa-gauge-high"></i> 模型调用</span>
            <el-button size="small" link type="primary" @click="fetchModelMetrics">刷新</el-button>
          </div>
          <div class="metric-list" v-if="modelMetrics.length > 0">
            <div class="metric-item" v-for="metric in modelMetrics.slice(0, 5)" :key="metric.id">
              <div class="metric-top">
                <span>{{ metric.agentName }}</span>
                <strong :class="metric.success ? 'metric-ok' : 'metric-fail'">{{ metric.success ? '成功' : '失败' }}</strong>
              </div>
              <div class="metric-bottom">
                <span>{{ metric.modelName }}</span>
                <span>{{ metric.costMs || 0 }}ms</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-tip">暂无模型调用记录</div>
        </div>

        <!-- 历史会话 -->
        <div class="panel-card history-card">
          <div class="card-title" style="display: flex; justify-content: space-between;">
            <span><i class="fa-solid fa-clock-rotate-left"></i> 历史评测</span>
            <el-button size="small" link type="primary" @click="openHistoryDrawer">全部</el-button>
          </div>
          <div class="history-list" v-if="historyRecords.length > 0">
            <div class="history-item" v-for="record in historyRecords.slice(0, 3)" :key="record.id" @click="viewHistoryReport(record)">
              <div class="hi-score">{{ parseReport(record.evaluationReport)?.technicalScore || 0 }}分</div>
              <div class="hi-info">
                <div class="hi-date">{{ formatDate(record.createTime).split(' ')[0] }}</div>
                <div class="hi-desc">{{ (parseReport(record.evaluationReport)?.overallSummary || '暂无评价').substring(0, 15) }}...</div>
              </div>
            </div>
          </div>
          <div v-else class="empty-tip">暂无评测记录</div>
        </div>
      </div>
    </div>

    <!-- 报告弹窗 -->
    <el-dialog v-model="reportDialogVisible" title="🌟 面试评估报告" width="65%" custom-class="glass-dialog">
      <div v-if="reportData" class="report-content">
        <div class="report-actions">
          <el-button size="small" type="primary" plain @click="exportReportPdf">
            <i class="fa-solid fa-file-pdf"></i> 导出 PDF
          </el-button>
        </div>

        <div class="score-board">
          <div class="score-item">
            <div class="score-label">技术得分</div>
            <div class="score-value success">{{ reportData.technicalScore }}<span class="score-max">/100</span></div>
          </div>
          <div class="score-item">
            <div class="score-label">沟通得分</div>
            <div class="score-value warning">{{ reportData.communicationScore }}<span class="score-max">/100</span></div>
          </div>
        </div>
        
        <div class="report-section">
          <h4><i class="fa-solid fa-quote-left"></i> 总体评价</h4>
          <p>{{ reportData.overallSummary }}</p>
        </div>

        <div class="report-section">
          <h4><i class="fa-solid fa-lightbulb"></i> 改进建议</h4>
          <ul>
            <li v-for="(sug, i) in reportData.suggestions" :key="i">{{ sug }}</li>
          </ul>
        </div>

        <div class="report-section" v-if="reportData.weakKnowledgePoints && reportData.weakKnowledgePoints.length > 0">
          <h4><i class="fa-solid fa-triangle-exclamation"></i> 薄弱知识点复盘</h4>
          <div class="weakness-item" v-for="(kp, i) in reportData.weakKnowledgePoints" :key="i">
            <div class="w-question">Q: {{ kp.question }}</div>
            <div class="w-answer"><strong>正确解答：</strong><span v-html="renderMarkdown(kp.correctAnswer)"></span></div>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="historyDrawerVisible" title="历史面试记录" size="400px" custom-class="glass-drawer">
      <el-timeline style="padding: 20px 10px;">
        <el-timeline-item
          v-for="record in historyRecords"
          :key="record.id"
          :timestamp="formatDate(record.createTime)"
          placement="top"
        >
          <el-card class="history-timeline-card" shadow="hover" @click="viewHistoryReport(record)">
            <h4>得分：{{ parseReport(record.evaluationReport)?.technicalScore || 0 }} 分</h4>
            <p>{{ (parseReport(record.evaluationReport)?.overallSummary || '暂无评价').substring(0, 40) }}...</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>

    <el-dialog v-model="materialsDialogVisible" title="已上传 JD / 简历" width="70%" custom-class="glass-dialog">
      <div class="materials-dialog">
        <el-tabs v-model="materialsActiveTab">
          <el-tab-pane :label="`岗位 JD (${uploadedMaterials.jdLength || 0}字)`" name="jd">
            <pre class="material-preview">{{ uploadedMaterials.jdContent || '还没有上传岗位 JD' }}</pre>
          </el-tab-pane>
          <el-tab-pane :label="`个人简历 (${uploadedMaterials.resumeLength || 0}字)`" name="resume">
            <pre class="material-preview">{{ uploadedMaterials.resumeContent || '还没有上传个人简历' }}</pre>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <el-dialog v-model="knowledgeDialogVisible" title="AI 题库内容" width="70%" custom-class="glass-dialog">
      <div class="knowledge-dialog">
        <div class="knowledge-summary">
          <span>索引：{{ knowledgeStatus.indexName || '-' }}</span>
          <span>片段数：{{ knowledgeStatus.documentCount || 0 }}</span>
        </div>
        <div v-if="knowledgeStatus.samples && knowledgeStatus.samples.length > 0" class="knowledge-samples">
          <div class="knowledge-sample" v-for="(sample, index) in knowledgeStatus.samples" :key="index">
            <div class="sample-title">片段 {{ index + 1 }}</div>
            <pre>{{ sample }}</pre>
          </div>
        </div>
        <div v-else class="empty-tip">暂无题库内容，或 Elasticsearch 当前不可用</div>
      </div>
    </el-dialog>

    <el-dialog v-model="authDialogVisible" :title="authMode === 'login' ? '登录' : '注册'" width="420px" custom-class="glass-dialog">
      <div class="auth-form">
        <el-input v-model="authForm.username" placeholder="用户名" clearable></el-input>
        <el-input v-model="authForm.password" placeholder="密码" type="password" show-password></el-input>
        <el-button type="primary" class="auth-submit" :loading="isAuthLoading" @click="submitAuth">
          {{ authMode === 'login' ? '登录' : '注册' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch, computed, nextTick } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { v4 as uuidv4 } from 'uuid'
import { use } from 'echarts/core'
import { RadarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

use([TitleComponent, TooltipComponent, LegendComponent, RadarChart, CanvasRenderer])

const sessionId = ref(uuidv4())
const messaggListRef = ref()
const isSending = ref(false)
const inputMessage = ref('')
const interviewMessages = ref([])
const tutorMessages = ref([])
const currentMode = ref('interview')
const agentDecision = ref(null)
const currentUser = ref({})
const authDialogVisible = ref(false)
const authMode = ref('login')
const isAuthLoading = ref(false)
const authForm = ref({ username: '', password: '' })
const isListening = ref(false)
let speechRecognition = null
const isSpeechSupported = typeof window !== 'undefined'
  && !!(window.SpeechRecognition || window.webkitSpeechRecognition)

const speechErrorMessage = (errorCode) => {
  const messageMap = {
    'not-allowed': '麦克风权限被拒绝，请在浏览器地址栏打开麦克风权限',
    'service-not-allowed': '当前浏览器禁用了语音识别服务',
    'audio-capture': '没有检测到可用麦克风设备',
    'network': '语音识别服务连接失败，请检查网络或浏览器服务状态',
    'no-speech': '没有识别到语音，请靠近麦克风后重试',
    'aborted': '语音识别已取消'
  }
  return messageMap[errorCode] || `语音识别失败: ${errorCode || 'unknown'}`
}

// 雷达图配置
const chartOption = ref({
  tooltip: { trigger: 'item' },
  radar: {
    indicator: [
      { name: 'Java基础', max: 100 },
      { name: '框架原理', max: 100 },
      { name: '架构设计', max: 100 },
      { name: '算法数据结构', max: 100 },
      { name: '沟通表达', max: 100 }
    ],
    splitNumber: 4,
    axisName: { color: '#94a3b8' },
    splitLine: { lineStyle: { color: ['rgba(255, 255, 255, 0.1)'] } },
    splitArea: { show: false },
    axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } }
  },
  series: [{
    name: '能力画像',
    type: 'radar',
    data: [{
      value: [60, 60, 60, 60, 60],
      name: '当前预估',
      itemStyle: { color: '#60a5fa' },
      areaStyle: { color: 'rgba(96, 165, 250, 0.3)' }
    }]
  }]
})

const displayMessages = computed(() => {
  return currentMode.value === 'interview' ? interviewMessages.value : tutorMessages.value
})

const setMode = (mode) => {
  currentMode.value = mode
  nextTick(() => scrollToBottom())
}

const isEnriching = ref(false)
const enrichProfile = async () => {
  isEnriching.value = true
  try {
    const res = await axios.post(`/api/api/tutor/enrich-profile?userId=${currentUser.value.userId}&sessionId=${sessionId.value}`)
    ElMessage.success(res.data)
    fetchModelMetrics()
    // 模拟雷达图变化
    chartOption.value.series[0].data[0].value = [85, 70, 65, 80, 75]
  } catch(e) {
    ElMessage.error('画像更新失败')
  } finally {
    isEnriching.value = false
  }
}

const reportDialogVisible = ref(false)
const historyDrawerVisible = ref(false)
const materialsDialogVisible = ref(false)
const knowledgeDialogVisible = ref(false)
const historyRecords = ref([])
const materialsActiveTab = ref('jd')
const uploadedMaterials = ref({})
const knowledgeStatus = ref({})
const modelMetrics = ref([])

const fetchHistory = async () => {
  if (!currentUser.value.userId) return
  try {
    const res = await axios.get(`/api/api/interview/history?userId=${currentUser.value.userId}`)
    historyRecords.value = res.data
    if (res.data && res.data.length > 0) {
      const lastRep = parseReport(res.data[0].evaluationReport)
      if (lastRep && lastRep.technicalScore) {
        chartOption.value.series[0].data[0].value = [
          lastRep.technicalScore, 
          lastRep.technicalScore - 5, 
          lastRep.technicalScore - 10, 
          lastRep.technicalScore + 5, 
          lastRep.communicationScore || 60
        ]
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchAgentDecision = async () => {
  if (!currentUser.value.userId) return
  try {
    const res = await axios.get(`/api/api/interview/trace/latest?userId=${currentUser.value.userId}&sessionId=${sessionId.value}`)
    agentDecision.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const fetchModelMetrics = async () => {
  if (!currentUser.value.userId) return
  try {
    const res = await axios.get(`/api/api/model-metrics/recent?userId=${currentUser.value.userId}&limit=20`)
    modelMetrics.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const stageLabel = (stage) => {
  const map = {
    ICE_BREAKING: '开场确认',
    BASIC_TECH: '基础技术',
    DEEP_DIVE: '项目深挖',
    WRAP_UP: '总结收尾'
  }
  return map[stage] || stage || '未知阶段'
}

const displayStageLabel = (stage) => {
  const map = {
    ICE_BREAKING: '开场确认',
    BASIC_TECH: '项目延展',
    DEEP_DIVE: '基础八股',
    WRAP_UP: '总结收尾'
  }
  return map[stage] || stage || '未知阶段'
}

const toolStatusLabel = (status) => {
  const map = {
    PLANNER_OK: 'Planner 正常',
    PLANNER_EXCEPTION_FALLBACK: 'Planner 异常兜底',
    PLANNER_PARSE_FALLBACK: '解析兜底',
    PLANNER_INVALID_STAGE_FALLBACK: '阶段兜底',
    PLANNER_EARLY_WRAP_UP_FALLBACK: '过早收尾兜底',
    PLANNER_EMPTY_INSTRUCTION_FALLBACK: '空指令兜底'
  }
  return map[status] || status || '暂无'
}

const openHistoryDrawer = async () => {
  historyDrawerVisible.value = true
  await fetchHistory()
}

const parseReport = (reportStr) => {
  if (!reportStr) return {}
  try {
    return JSON.parse(reportStr)
  } catch (e) {
    return {}
  }
}

const viewHistoryReport = (record) => {
  reportData.value = parseReport(record.evaluationReport)
  reportDialogVisible.value = true
}

const formatDate = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toLocaleString()
}
const isGeneratingReport = ref(false)
const reportData = ref(null)

const selectedStyle = ref('PROFESSIONAL')

const onStyleChange = async (val) => {
  try {
    await axios.post(`/api/api/profile/style/update?userId=${currentUser.value.userId}&sessionId=${sessionId.value}&style=${val}`)
    ElMessage.success('风格切换成功！请重新开始一局。')
  } catch (e) {
    ElMessage.error('切换失败')
  }
}

onMounted(() => {
  watch(displayMessages, () => scrollToBottom(), { deep: true })
  hello()
  initUser()
})

const scrollToBottom = () => {
  if (messaggListRef.value) {
    messaggListRef.value.scrollTop = messaggListRef.value.scrollHeight
  }
}

const hello = () => {
  interviewMessages.value.push({
    isUser: false,
    content: '你好！我是鱼鳞面试官 Scale。请问你准备好开始面试了吗？你可以先上传你的 **简历** 和 **JD**。',
    isTyping: false,
    isThinking: false
  })
  tutorMessages.value.push({
    isUser: false,
    content: '你好！我是你的专属 AI 知识导师。有任何技术不懂的地方，随时问我！',
    isTyping: false,
    isThinking: false
  })
}

const onUploadSuccess = (res) => {
  ElMessage.success(res || '上传成功！')
  fetchUploadedMaterials()
}

const onKnowledgeUploadSuccess = (res) => {
  ElMessage.success(res || '知识库上传成功！')
  fetchKnowledgeStatus()
}

const clearKnowledge = async () => {
  try {
    const res = await axios.post(`/api/api/knowledge/clear?userId=${currentUser.value.userId}`)
    ElMessage.success(res.data)
  } catch (e) {
    ElMessage.error('清空失败')
  }
}

const onUploadError = () => {
  ElMessage.error('网络错误或后端异常')
}

const fetchUploadedMaterials = async () => {
  if (!currentUser.value.userId) return
  try {
    const res = await axios.get(`/api/api/profile/materials?userId=${currentUser.value.userId}&sessionId=${sessionId.value}`)
    uploadedMaterials.value = res.data || {}
  } catch (e) {
    ElMessage.error('读取上传资料失败')
  }
}

const openMaterialsDialog = async () => {
  await fetchUploadedMaterials()
  materialsDialogVisible.value = true
}

const fetchKnowledgeStatus = async () => {
  try {
    const res = await axios.get(`/api/api/knowledge/status?userId=${currentUser.value.userId}`)
    knowledgeStatus.value = res.data || {}
  } catch (e) {
    ElMessage.error('读取题库状态失败')
  }
}

const openKnowledgeDialog = async () => {
  await fetchKnowledgeStatus()
  knowledgeDialogVisible.value = true
}

const generateReport = async () => {
  if (interviewMessages.value.length <= 1) {
    ElMessage.warning('目前还没有面试记录哦')
    return
  }
  isGeneratingReport.value = true
  try {
    const res = await axios.post(`/api/api/interview/report?userId=${currentUser.value.userId}&sessionId=${sessionId.value}`)
    reportData.value = res.data
    reportDialogVisible.value = true
    fetchHistory()
    fetchModelMetrics()
  } catch (e) {
    ElMessage.error('生成报告失败')
  } finally {
    isGeneratingReport.value = false
  }
}

const exportReportPdf = () => {
  if (!reportData.value) {
    ElMessage.warning('暂无可导出的报告')
    return
  }

  const report = reportData.value
  const suggestions = Array.isArray(report.suggestions) ? report.suggestions : []
  const weakPoints = Array.isArray(report.weakKnowledgePoints) ? report.weakKnowledgePoints : []
  const generatedAt = new Date().toLocaleString()

  const html = `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>鱼鳞面试官评估报告</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 32px;
      color: #1e293b;
      background: #f8fafc;
      font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
      line-height: 1.7;
    }
    .page {
      max-width: 860px;
      margin: 0 auto;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 16px;
      padding: 32px;
    }
    .header {
      display: flex;
      justify-content: space-between;
      gap: 24px;
      border-bottom: 2px solid #e2e8f0;
      padding-bottom: 18px;
      margin-bottom: 24px;
    }
    h1 { margin: 0; font-size: 28px; color: #0f172a; }
    .meta { color: #64748b; font-size: 13px; text-align: right; }
    .scores {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-bottom: 28px;
    }
    .score {
      background: #f1f5f9;
      border-radius: 12px;
      padding: 20px;
      text-align: center;
    }
    .score-label { color: #475569; font-size: 14px; }
    .score-value { margin-top: 8px; font-size: 40px; font-weight: 800; }
    .technical { color: #059669; }
    .communication { color: #d97706; }
    section { margin-top: 26px; }
    h2 {
      margin: 0 0 12px 0;
      font-size: 18px;
      color: #0f172a;
      border-left: 4px solid #3b82f6;
      padding-left: 10px;
    }
    ul { padding-left: 22px; }
    li { margin-bottom: 8px; }
    .weak {
      margin-top: 14px;
      padding: 16px;
      border-left: 4px solid #f59e0b;
      background: #f8fafc;
      border-radius: 8px;
      break-inside: avoid;
    }
    .question { font-weight: 700; margin-bottom: 8px; }
    .answer { color: #334155; white-space: pre-wrap; }
    @media print {
      body { padding: 0; background: #fff; }
      .page { border: none; border-radius: 0; }
    }
  </style>
</head>
<body>
  <main class="page">
    <div class="header">
      <div>
        <h1>鱼鳞面试官评估报告</h1>
        <div>Scale Interview</div>
      </div>
      <div class="meta">
        <div>用户：${escapeHtml(currentUser.value.username || '-')} / ID ${escapeHtml(String(currentUser.value.userId || '-'))}</div>
        <div>导出时间：${escapeHtml(generatedAt)}</div>
      </div>
    </div>

    <div class="scores">
      <div class="score">
        <div class="score-label">技术得分</div>
        <div class="score-value technical">${escapeHtml(String(report.technicalScore ?? 0))}<span style="font-size:16px;color:#64748b;"> /100</span></div>
      </div>
      <div class="score">
        <div class="score-label">沟通得分</div>
        <div class="score-value communication">${escapeHtml(String(report.communicationScore ?? 0))}<span style="font-size:16px;color:#64748b;"> /100</span></div>
      </div>
    </div>

    <section>
      <h2>总体评价</h2>
      <p>${escapeHtml(report.overallSummary || '暂无评价')}</p>
    </section>

    <section>
      <h2>改进建议</h2>
      <ul>${suggestions.map(item => `<li>${escapeHtml(item)}</li>`).join('') || '<li>暂无建议</li>'}</ul>
    </section>

    <section>
      <h2>薄弱知识点复盘</h2>
      ${weakPoints.map(point => `
        <div class="weak">
          <div class="question">Q: ${escapeHtml(point.question || '未记录问题')}</div>
          <div class="answer"><strong>正确解答：</strong>${escapeHtml(point.correctAnswer || '暂无参考解答')}</div>
        </div>
      `).join('') || '<p>暂无薄弱知识点。</p>'}
    </section>
  </main>
  <script>
    window.onload = () => {
      window.focus();
      window.print();
    };
  <\/script>
</body>
</html>`

  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.error('浏览器阻止了弹窗，请允许弹窗后重试')
    return
  }
  printWindow.document.open()
  printWindow.document.write(html)
  printWindow.document.close()
}

const escapeHtml = (value) => {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const sendMessage = () => {
  if (inputMessage.value.trim()) {
    sendRequest(inputMessage.value.trim())
    inputMessage.value = ''
  }
}

const handleInputEnter = (event) => {
  if (event.shiftKey) {
    return
  }
  event.preventDefault()
  sendMessage()
}

const toggleSpeechInput = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessage.warning('当前浏览器不支持语音识别，建议使用 Chrome')
    return
  }

  if (isListening.value && speechRecognition) {
    speechRecognition.stop()
    return
  }

  speechRecognition = new SpeechRecognition()
  speechRecognition.lang = 'zh-CN'
  speechRecognition.continuous = false
  speechRecognition.interimResults = true

  let finalText = inputMessage.value ? inputMessage.value.trimEnd() : ''
  if (finalText && !finalText.endsWith('\n')) {
    finalText += ' '
  }

  speechRecognition.onstart = () => {
    isListening.value = true
  }

  speechRecognition.onresult = (event) => {
    let interimText = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const text = event.results[i][0].transcript
      if (event.results[i].isFinal) {
        finalText += text
      } else {
        interimText += text
      }
    }
    inputMessage.value = finalText + interimText
  }

  speechRecognition.onerror = (event) => {
    const message = event.error === 'not-allowed'
      ? '麦克风权限被拒绝'
      : '语音识别失败'
    ElMessage.error(speechErrorMessage(event.error))
  }

  speechRecognition.onend = () => {
    isListening.value = false
    speechRecognition = null
  }

  speechRecognition.start()
}

const sendRequest = (message) => {
  if (!currentUser.value.userId) {
    ElMessage.warning('请先登录')
    return
  }
  isSending.value = true
  const userMsg = { isUser: true, content: message, isTyping: false, isThinking: false }
  const targetMessages = currentMode.value === 'interview' ? interviewMessages.value : tutorMessages.value;
  targetMessages.push(userMsg)

  const botMsg = { isUser: false, content: '', isTyping: true, isThinking: false }
  targetMessages.push(botMsg)
  const lastMsg = targetMessages[targetMessages.length - 1]
  scrollToBottom()

  const url = currentMode.value === 'interview' 
      ? `/api/api/interview/chat?userId=${currentUser.value.userId}&sessionId=${sessionId.value}` 
      : `/api/api/tutor/chat?userId=${currentUser.value.userId}&sessionId=${sessionId.value}`;

  axios.post(url, message, {
        headers: { 'Content-Type': 'text/plain' },
        onDownloadProgress: (e) => {
          const fullText = e.event.target.responseText
          if (fullText.includes('data:')) {
            let lines = fullText.split('\n')
            let content = ''
            let isFirstDataInEvent = true
            for (let i = 0; i < lines.length; i++) {
              let line = lines[i]
              if (line.startsWith('data:')) {
                let text = line.substring(5)
                if (text.startsWith(' ')) text = text.substring(1)
                if (!isFirstDataInEvent) content += '\n'
                content += text
                isFirstDataInEvent = false
              } else if (line.trim() === '') {
                isFirstDataInEvent = true
              }
            }
            lastMsg.content = content
          } else {
            lastMsg.content = fullText
          }
          scrollToBottom()
        },
      })
    .then(() => {
      targetMessages.at(-1).isTyping = false
      isSending.value = false
      if (currentMode.value === 'interview') {
        fetchAgentDecision()
      }
      fetchModelMetrics()
    })
    .catch(() => {
      targetMessages.at(-1).content += '\n\n**请求失败或流被中断**'
      targetMessages.at(-1).isTyping = false
      isSending.value = false
    })
}

const renderMarkdown = (text) => {
  if (!text) return ''
  return marked.parse(text, { breaks: true })
}

const newChat = () => {
  window.location.reload()
}

const initUser = async () => {
  const stored = localStorage.getItem('scale_current_user')
  if (stored) {
    try {
      currentUser.value = JSON.parse(stored)
      await fetchHistory()
      return
    } catch (e) {
      localStorage.removeItem('scale_current_user')
    }
  }
  authForm.value = { username: '', password: '' }
}

const openAuthDialog = (mode) => {
  authMode.value = mode
  authForm.value = { username: '', password: '' }
  authDialogVisible.value = true
}

const submitAuth = async () => {
  if (!authForm.value.username.trim() || !authForm.value.password.trim()) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  isAuthLoading.value = true
  try {
    const url = authMode.value === 'login' ? '/api/api/auth/login' : '/api/api/auth/register'
    const res = await axios.post(url, authForm.value)
    setCurrentUser(res.data)
    authDialogVisible.value = false
    ElMessage.success(authMode.value === 'login' ? '登录成功' : '注册成功')
  } catch (e) {
    ElMessage.error(e.response?.data || '操作失败')
  } finally {
    isAuthLoading.value = false
  }
}

const setCurrentUser = async (user) => {
  currentUser.value = user || {}
  localStorage.setItem('scale_current_user', JSON.stringify(currentUser.value))
  historyRecords.value = []
  uploadedMaterials.value = {}
  agentDecision.value = null
  modelMetrics.value = []
  await fetchHistory()
  await fetchModelMetrics()
}

const logout = () => {
  localStorage.removeItem('scale_current_user')
  currentUser.value = {}
  historyRecords.value = []
  uploadedMaterials.value = {}
  agentDecision.value = null
  modelMetrics.value = []
  authMode.value = 'login'
  authForm.value = { username: '', password: '' }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.94), rgba(30, 41, 59, 0.88)),
    radial-gradient(circle at 20% 20%, rgba(14, 165, 233, 0.35), transparent 34%),
    radial-gradient(circle at 80% 70%, rgba(16, 185, 129, 0.2), transparent 32%);
  color: #f8fafc;
}
.login-shell {
  width: min(920px, calc(100vw - 40px));
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 28px;
  align-items: stretch;
}
.login-brand,
.login-panel {
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(15, 23, 42, 0.72);
  backdrop-filter: blur(20px);
  border-radius: 18px;
  padding: 34px;
}
.login-brand {
  display: flex;
  align-items: center;
  gap: 20px;
}
.login-logo {
  width: 68px;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: linear-gradient(135deg, #0ea5e9, #10b981);
  color: #fff;
  font-size: 30px;
}
.login-brand h1 {
  margin: 0 0 8px 0;
  font-size: 34px;
  letter-spacing: 0;
}
.login-brand p {
  margin: 0;
  color: #94a3b8;
  font-size: 16px;
}
.login-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.login-title {
  color: #e2e8f0;
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 4px;
}
.login-submit {
  height: 44px;
  width: 100%;
}
.register-link {
  border: none;
  background: transparent;
  color: #93c5fd;
  cursor: pointer;
  font-size: 14px;
}
.app-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: radial-gradient(circle at top left, #1e293b 0%, #0f172a 100%);
  color: #f8fafc;
  font-family: 'Inter', -apple-system, sans-serif;
  overflow: hidden;
}

/* Sidebar */
.sidebar {
  width: 260px;
  background: rgba(15, 23, 42, 0.6);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  padding: 25px 20px;
  backdrop-filter: blur(20px);
  z-index: 10;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 25px;
}
.logo-circle {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.4);
  margin-bottom: 15px;
}
.logo-text {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #e2e8f0, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.logo-sub {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}
.user-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 18px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.06);
}
.user-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.user-name {
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 700;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.user-id {
  color: #64748b;
  font-size: 11px;
}
.user-actions {
  display: flex;
  flex-shrink: 0;
}

.menu-section {
  flex: 1;
  overflow-y: auto;
}

:deep(.el-divider__text) {
  background-color: transparent;
  color: #64748b;
  font-size: 12px;
  padding: 0 10px;
}
:deep(.el-divider) {
  border-top-color: rgba(255,255,255,0.05);
  margin: 25px 0 15px 0;
}

.mode-toggle {
  display: flex;
  background: rgba(0,0,0,0.3);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 10px;
}
.mode-item {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  color: #94a3b8;
  transition: all 0.3s ease;
}
.mode-item.active {
  background: rgba(255,255,255,0.1);
  color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.sidebar-btn {
  width: 100%;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.05);
  color: #cbd5e1;
  border-radius: 10px;
  padding: 18px 15px;
  margin-bottom: 10px;
  justify-content: flex-start;
  transition: all 0.2s;
}
.sidebar-btn:hover {
  background: rgba(255,255,255,0.08);
  border-color: rgba(255,255,255,0.15);
  color: #fff;
}
.sidebar-btn i {
  width: 20px;
  margin-right: 8px;
}
.primary-btn {
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: white;
  border: none;
  justify-content: center;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}
.primary-btn:hover { background: linear-gradient(135deg, #2563eb, #4f46e5); color: white; transform: translateY(-1px); }

.style-select { width: 100%; margin-bottom: 15px; }
:deep(.style-select .el-input__wrapper) {
  background-color: rgba(0,0,0,0.2) !important;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.05) inset !important;
}

/* Chat Room */
.chat-room {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" opacity="0.02"><circle cx="50" cy="50" r="1" fill="%23ffffff"/></svg>') repeat;
}

.chat-header {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  background: rgba(15, 23, 42, 0.3);
  backdrop-filter: blur(10px);
}
.status-indicator {
  display: flex;
  align-items: center;
}
.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  margin-right: 10px;
  box-shadow: 0 0 10px #10b981;
  animation: breathe 2s infinite ease-in-out;
}
@keyframes breathe {
  0%, 100% { opacity: 0.5; box-shadow: 0 0 5px #10b981; }
  50% { opacity: 1; box-shadow: 0 0 15px #10b981; }
}
.status-text {
  font-weight: 600;
  letter-spacing: 0.5px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 30px 40px;
  display: flex;
  flex-direction: column;
}
.message-wrapper {
  display: flex;
  margin-bottom: 25px;
  animation: slideUp 0.3s ease-out;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.user-wrapper { justify-content: flex-end; }
.bot-wrapper { justify-content: flex-start; }

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin: 0 15px;
  flex-shrink: 0;
}
.bot-wrapper .avatar {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
}
.user-avatar {
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.05);
}

.message-bubble {
  max-width: 75%;
  padding: 15px 20px;
  border-radius: 16px;
  line-height: 1.6;
  font-size: 15px;
}
.bot-bubble {
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(255,255,255,0.05);
  border-top-left-radius: 4px;
}
.user-bubble {
  background: linear-gradient(135deg, #0ea5e9, #3b82f6);
  color: white;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 15px rgba(14, 165, 233, 0.2);
}

.input-area {
  padding: 20px 40px;
  background: linear-gradient(to top, rgba(15,23,42,1) 50%, transparent);
}
.input-glass {
  display: flex;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 20px;
  padding: 8px 8px 8px 20px;
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
:deep(.input-glass .el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  padding: 0;
}
:deep(.input-glass .el-input__inner) {
  color: white !important;
  font-size: 15px;
}
:deep(.input-glass .el-textarea__inner) {
  min-height: 44px !important;
  max-height: 140px;
  padding: 10px 0;
  background: transparent !important;
  box-shadow: none !important;
  color: white !important;
  font-size: 15px;
  line-height: 24px;
  resize: none;
}
.send-btn {
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border: none;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.2s;
}
.voice-btn {
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.08);
  width: 44px;
  height: 44px;
  border-radius: 14px;
  color: #cbd5e1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.2s;
  margin-right: 8px;
}
.voice-btn:hover:not(:disabled) {
  background: rgba(255,255,255,0.14);
  color: #fff;
}
.voice-btn.listening {
  background: rgba(16, 185, 129, 0.22);
  border-color: rgba(16, 185, 129, 0.5);
  color: #34d399;
  box-shadow: 0 0 14px rgba(16, 185, 129, 0.35);
}
.voice-btn:disabled {
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.3);
  cursor: not-allowed;
}
.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 0 15px rgba(59, 130, 246, 0.5);
}
.send-btn:disabled { background: rgba(255,255,255,0.1); color: rgba(255,255,255,0.3); cursor: not-allowed; }

/* Dashboard */
.dashboard {
  width: 320px;
  background: rgba(15, 23, 42, 0.4);
  border-left: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
}
.dashboard-header {
  height: 70px;
  display: flex;
  align-items: center;
  padding: 0 25px;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.dashboard-header h3 { margin: 0; font-size: 16px; color: #e2e8f0; }

.dashboard-content {
  padding: 25px;
  flex: 1;
  overflow-y: auto;
}
.panel-card {
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 25px;
}
.decision-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 10px;
}
.decision-row strong {
  color: #e2e8f0;
  text-align: right;
}
.decision-reason {
  margin-top: 12px;
  padding: 12px;
  border-radius: 10px;
  background: rgba(0,0,0,0.2);
  color: #cbd5e1;
  font-size: 13px;
  line-height: 1.6;
}
.card-title {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 15px;
  font-weight: 600;
}
.card-title i { margin-right: 6px; color: #60a5fa; }
.chart-container {
  height: 200px;
  width: 100%;
}
.full-width-btn { width: 100%; margin-top: 10px; }

.history-list { display: flex; flex-direction: column; gap: 12px; }
.metric-list { display: flex; flex-direction: column; gap: 10px; }
.metric-item {
  background: rgba(0,0,0,0.2);
  padding: 10px;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.04);
}
.metric-top,
.metric-bottom {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}
.metric-top {
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 5px;
}
.metric-bottom {
  color: #94a3b8;
  font-size: 12px;
}
.metric-ok { color: #22c55e; }
.metric-fail { color: #f87171; }
.history-item {
  display: flex;
  align-items: center;
  background: rgba(0,0,0,0.2);
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.history-item:hover {
  background: rgba(255,255,255,0.05);
  border-color: rgba(255,255,255,0.1);
}
.hi-score {
  font-size: 18px;
  font-weight: 700;
  color: #10b981;
  margin-right: 15px;
  min-width: 45px;
}
.hi-info { flex: 1; overflow: hidden; }
.hi-date { font-size: 12px; color: #64748b; margin-bottom: 4px; }
.hi-desc { font-size: 13px; color: #cbd5e1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* Dialog & Markdown */
:deep(.glass-dialog) {
  background: #f8fafc !important;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
}
:deep(.glass-dialog .el-dialog__title) { color: #111827; font-weight: 700; }
:deep(.glass-dialog .el-dialog__body) { color: #1e293b; }
:deep(.glass-dialog .el-dialog__headerbtn .el-dialog__close) { color: #64748b; }
.score-board { display: flex; gap: 20px; margin-bottom: 30px; }
.score-item {
  flex: 1;
  background: #e2e8f0;
  padding: 20px;
  border-radius: 16px;
  text-align: center;
}
.score-label { color: #334155; font-size: 14px; margin-bottom: 10px; }
.score-value { font-size: 40px; font-weight: 800; }
.score-value.success { color: #10b981; }
.score-value.warning { color: #f59e0b; }
.score-max { font-size: 16px; color: #475569; font-weight: normal; margin-left: 5px; }

.report-section { margin-bottom: 25px; }
.report-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 18px;
}
.report-section h4 { color: #1e293b; margin-bottom: 15px; display: flex; align-items: center; gap: 8px; }
.report-section h4 i { color: #3b82f6; }
.report-section p { line-height: 1.7; color: #334155; }
.report-section ul { padding-left: 20px; color: #334155; line-height: 1.7; }
.weakness-item {
  background: #e2e8f0;
  border-left: 4px solid #f59e0b;
  padding: 15px;
  border-radius: 0 8px 8px 0;
  margin-bottom: 15px;
}
.materials-dialog,
.knowledge-dialog {
  color: #1e293b;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.auth-submit {
  width: 100%;
}
@media (max-width: 760px) {
  .login-shell {
    grid-template-columns: 1fr;
  }
}
.material-preview,
.knowledge-sample pre {
  min-height: 260px;
  max-height: 520px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 10px;
  background: #e2e8f0;
  color: #1e293b;
  line-height: 1.7;
  font-family: inherit;
  font-size: 14px;
}
.knowledge-summary {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  color: #475569;
}
.knowledge-samples {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.sample-title {
  margin-bottom: 8px;
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
}
.w-question { font-weight: 600; margin-bottom: 10px; color: #1e293b; }
.w-answer { color: #334155; line-height: 1.6; }
:deep(.glass-dialog .el-tabs__item) { color: #334155; }
:deep(.glass-dialog .el-tabs__item.is-active) { color: #2563eb; }
:deep(.glass-dialog .el-tabs__nav-wrap::after) { background-color: #cbd5e1; }

/* Markdown Styles */
:deep(.markdown-body p) { margin: 0 0 10px 0; }
:deep(.markdown-body p:last-child) { margin-bottom: 0; }
:deep(.markdown-body code) {
  background: rgba(0,0,0,0.3);
  padding: 3px 6px;
  border-radius: 6px;
  font-family: monospace;
  color: #fbbf24;
}
:deep(.markdown-body pre) {
  background: #0f172a;
  padding: 15px;
  border-radius: 12px;
  overflow-x: auto;
  border: 1px solid rgba(255,255,255,0.05);
}

.loading-wave {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}
.loading-wave span {
  width: 6px;
  height: 6px;
  background: #94a3b8;
  border-radius: 50%;
  animation: wave 1s infinite;
}
.loading-wave span:nth-child(2) { animation-delay: 0.2s; }
.loading-wave span:nth-child(3) { animation-delay: 0.4s; }
@keyframes wave {
  0%, 100% { transform: translateY(0); opacity: 0.5; }
  50% { transform: translateY(-4px); opacity: 1; }
}

::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 10px; }
::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.2); }
</style>
