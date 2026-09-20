<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      <template slot="title">
        导入 tools/export_legacy.py 产出的 legacy_export.jsonl。建议先「试跑」（只匹配不写库）确认命中率，再正式导入。重导幂等：已导入的影片只刷新资源与题材标签。
      </template>
    </el-alert>

    <el-card shadow="never" style="margin-bottom: 12px">
      <el-form inline>
        <el-form-item label="导出文件">
          <el-upload :show-file-list="false" accept=".jsonl,.json,.txt" :http-request="doUpload">
            <el-button size="small" icon="el-icon-upload2" :loading="uploading">选择并上传</el-button>
          </el-upload>
          <span v-if="filePath" style="margin-left: 8px; color: #67c23a">已上传（{{ fileSize }}）</span>
        </el-form-item>
        <el-form-item label="处理行数">
          <el-input-number v-model="maxRows" :min="0" :max="100000" size="small" />
          <span class="form-tip">0 为全部</span>
        </el-form-item>
        <el-form-item label="海报前缀">
          <el-input v-model="posterBase" size="small" style="width: 260px" placeholder="https://image.yingwans.com" />
        </el-form-item>
        <el-form-item>
          <el-button size="small" type="warning" icon="el-icon-view" :disabled="!filePath || running" @click="start(true)">试跑</el-button>
          <el-button size="small" type="danger" icon="el-icon-video-play" :disabled="!filePath || running" @click="start(false)">正式导入</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="task" shadow="never" style="margin-bottom: 12px">
      <div slot="header">
        <span>{{ task.dryRun ? '试跑' : '正式导入' }}进度</span>
        <el-tag v-if="task.running" type="warning" size="small" style="margin-left: 8px">进行中</el-tag>
        <el-tag v-else type="success" size="small" style="margin-left: 8px">已完成</el-tag>
        <span style="margin-left: 12px; color: #909399; font-size: 12px">耗时 {{ formatElapsed(task.elapsedMs) }}</span>
      </div>
      <el-progress :percentage="percent" :stroke-width="16" :text-inside="true" />
      <el-row :gutter="12" style="margin-top: 12px; text-align: center">
        <el-col :span="4"><div class="stat-num">{{ task.done }}/{{ task.total }}</div><div class="stat-label">已处理</div></el-col>
        <el-col :span="4"><div class="stat-num" style="color:#909399">{{ task.reused }}</div><div class="stat-label">复用(已有)</div></el-col>
        <el-col :span="4"><div class="stat-num" style="color:#67c23a">{{ task.matchedImdb }}</div><div class="stat-label">IMDB命中</div></el-col>
        <el-col :span="4"><div class="stat-num" style="color:#409eff">{{ task.matchedTitle }}</div><div class="stat-label">标题命中</div></el-col>
        <el-col :span="4"><div class="stat-num" style="color:#e6a23c">{{ task.created }}</div><div class="stat-label">{{ task.dryRun ? '将新建legacy' : '新建legacy' }}</div></el-col>
        <el-col :span="4"><div class="stat-num" style="color:#f56c6c">{{ task.failed }}</div><div class="stat-label">失败</div></el-col>
      </el-row>
    </el-card>

    <el-card v-if="task && task.failures && task.failures.length" shadow="never" style="margin-bottom: 12px">
      <div slot="header"><span>失败清单（{{ task.failures.length }}）</span></div>
      <el-table :data="task.failures" size="mini" max-height="300">
        <el-table-column prop="aid" label="旧站aid" width="90" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="reason" label="原因" width="220" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card v-if="task && task.unmatched && task.unmatched.length" shadow="never">
      <div slot="header"><span>TMDB 未命中（{{ task.unmatched.length }}，{{ task.dryRun ? '正式导入时将' : '已' }}按旧站数据建 legacy 片）</span></div>
      <el-table :data="task.unmatched" size="mini" max-height="300">
        <el-table-column prop="aid" label="旧站aid" width="90" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="reason" label="原因" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { uploadLegacyFile, startLegacyImport, legacyImportProgress } from '@/api/kemovie/legacy'

export default {
  name: 'LegacyImport',
  data() {
    return {
      uploading: false,
      filePath: '',
      fileSize: '',
      maxRows: 0,
      posterBase: 'https://image.yingwans.com',
      taskId: '',
      task: null,
      timer: null
    }
  },
  computed: {
    running() { return this.task && this.task.running },
    percent() {
      if (!this.task || !this.task.total) return 0
      return Math.min(100, Math.round(this.task.done * 100 / this.task.total))
    }
  },
  beforeDestroy() { this.clearTimer() },
  methods: {
    doUpload(req) {
      this.uploading = true
      const fd = new FormData()
      fd.append('file', req.file)
      uploadLegacyFile(fd).then(res => {
        this.filePath = res.data.filePath
        this.fileSize = (res.data.size / 1024 / 1024).toFixed(1) + ' MB'
        this.$message.success('上传完成')
      }).finally(() => { this.uploading = false })
    },
    start(dryRun) {
      const tip = dryRun ? '试跑只匹配不写库，确认开始？' : '正式导入将写入影片与资源数据，确认开始？'
      this.$confirm(tip, '提示', { type: 'warning' }).then(() => {
        startLegacyImport({ filePath: this.filePath, dryRun, maxRows: this.maxRows, posterBase: this.posterBase }).then(res => {
          this.taskId = res.data.taskId
          this.task = null
          this.poll()
        })
      }).catch(() => {})
    },
    poll() {
      this.clearTimer()
      const tick = () => {
        legacyImportProgress(this.taskId).then(res => {
          this.task = res.data
          if (this.task && this.task.running) {
            this.timer = setTimeout(tick, 2000)
          } else {
            this.$message.success(this.task.dryRun ? '试跑完成' : '导入完成')
          }
        })
      }
      tick()
    },
    clearTimer() { if (this.timer) { clearTimeout(this.timer); this.timer = null } },
    formatElapsed(ms) {
      if (!ms) return '0s'
      const s = Math.round(ms / 1000)
      return s >= 60 ? Math.floor(s / 60) + 'm' + (s % 60) + 's' : s + 's'
    }
  }
}
</script>

<style scoped>
  .form-tip { margin-left: 6px; color: #909399; font-size: 12px; }
  .stat-num { font-size: 22px; font-weight: 600; }
  .stat-label { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
