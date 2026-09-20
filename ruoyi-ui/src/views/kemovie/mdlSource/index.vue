<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
      <el-form-item label="来源类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" placeholder="全部" clearable style="width: 140px" @change="handleQuery">
          <el-option label="MyDramaList" value="mdl" />
          <el-option label="JustWatch" value="justwatch" />
          <el-option label="Letterboxd" value="letterboxd" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入源名称" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['kemovie:mdl:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-video-play" size="mini" @click="handleRunAll" v-hasPermi="['kemovie:mdl:run']">全部抓取</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="sourceId" width="60" />
      <el-table-column label="来源" align="center" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.sourceType === 'mdl' ? '' : 'warning'" size="mini">
            {{ sourceTypeLabel(scope.row.sourceType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="平台 / List ID" align="center" prop="listId">
        <template slot-scope="scope">
          <span>{{ scope.row.sourceType === 'justwatch' ? (scope.row.listId || '全国热门') : scope.row.listId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="国家/地区" align="center" width="90">
        <template slot-scope="scope">
          <span>{{ scope.row.sourceType === 'letterboxd' ? '-' : scope.row.regionTag }}</span>
        </template>
      </el-table-column>
      <el-table-column label="影片类型" align="center" width="90">
        <template slot-scope="scope">{{ objectTypeLabel(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="页数" align="center" prop="maxPage" width="60" />
      <el-table-column label="目标片单" align="center" prop="targetCollectionId" width="80" />
      <el-table-column label="启用" align="center" width="60">
        <template slot-scope="scope">
          <el-tag :type="scope.row.enabled === '0' ? 'success' : 'info'" size="mini">{{ scope.row.enabled === '0' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上次状态" align="center" prop="lastStatus" :show-overflow-tooltip="true" min-width="180" />
      <el-table-column label="上次运行" align="center" prop="lastRunAt" width="150" />
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-video-play" :loading="scope.row._running" @click="handleRun(scope.row)" v-hasPermi="['kemovie:mdl:run']">抓取</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['kemovie:mdl:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['kemovie:mdl:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="来源类型" prop="sourceType">
          <el-radio-group v-model="form.sourceType" @change="handleTypeChange">
            <el-radio-button label="mdl">MyDramaList</el-radio-button>
            <el-radio-button label="justwatch">JustWatch</el-radio-button>
            <el-radio-button label="letterboxd">Letterboxd</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="源名称" prop="name"><el-input v-model="form.name" placeholder="如：Netflix 美国新片" /></el-form-item>

        <!-- MDL -->
        <template v-if="form.sourceType === 'mdl'">
          <el-form-item label="List ID" prop="listId">
            <el-input v-model="form.listId" placeholder="可填 ID(如 1R88rJN3) 或完整链接，URL 自动生成" />
          </el-form-item>
          <el-form-item label="地区标签" prop="regionTag"><el-input v-model="form.regionTag" placeholder="如 Asia / Europe" /></el-form-item>
        </template>

        <!-- JustWatch -->
        <template v-else-if="form.sourceType === 'justwatch'">
          <el-form-item label="国家/地区" prop="regionTag">
            <el-select v-model="form.regionTag" filterable allow-create default-first-option placeholder="US" style="width: 100%" @change="loadProviders">
              <el-option v-for="c in countries" :key="c.code" :label="c.code + ' · ' + c.name" :value="c.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="播放平台" prop="listId">
            <el-select v-model="form.listId" filterable allow-create default-first-option clearable
              :placeholder="providersLoading ? '平台加载中…' : '留空=全国热门；可选择或粘贴平台链接/slug'"
              style="width: 100%">
              <el-option :value="''" label="全国热门（不限平台）" />
              <el-option v-for="p in providerOptions" :key="p.packageId"
                :label="p.clearName + '（' + p.shortName + '）'" :value="p.slug" />
            </el-select>
          </el-form-item>
          <el-form-item label="影片类型" prop="objectType">
            <el-radio-group v-model="form.objectType">
              <el-radio label="">全部</el-radio>
              <el-radio label="movie">电影</el-radio>
              <el-radio label="tv">剧集</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- Letterboxd -->
        <template v-if="form.sourceType === 'letterboxd'">
          <el-form-item label="片单链接" prop="listId">
            <el-input v-model="form.listId" placeholder="https://letterboxd.com/{用户}/list/{片单}/" />
          </el-form-item>
        </template>

        <el-form-item label="抓取页数" prop="maxPage">
          <el-input-number v-model="form.maxPage" :min="1" :max="50" />
          <span v-if="form.sourceType === 'justwatch'" class="jw-tip">每页 {{ pageSize }} 条</span>
          <span v-if="form.sourceType === 'letterboxd'" class="jw-tip">每页约 100 部</span>
        </el-form-item>
        <el-form-item label="目标片单ID" prop="targetCollectionId"><el-input-number v-model="form.targetCollectionId" :min="1" placeholder="留空自动创建" /></el-form-item>
        <el-form-item label="是否启用" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMdlSource, getMdlSource, addMdlSource, updateMdlSource, delMdlSource, runMdlSource, runAllMdlSource, listJustWatchPackages } from '@/api/kemovie/admin'

export default {
  name: 'MdlSource',
  data() {
    return {
      loading: true,
      total: 0,
      list: [],
      title: '',
      open: false,
      pageSize: 100,
      providersLoading: false,
      providerOptions: [],
      providerCache: {},
      queryParams: { pageNum: 1, pageSize: 10, sourceType: null, name: null },
      form: {},
      countries: [
        { code: 'US', name: '美国' }, { code: 'CA', name: '加拿大' }, { code: 'GB', name: '英国' }, { code: 'IE', name: '爱尔兰' },
        { code: 'AU', name: '澳大利亚' }, { code: 'NZ', name: '新西兰' }, { code: 'HK', name: '中国香港' }, { code: 'TW', name: '中国台湾' },
        { code: 'SG', name: '新加坡' }, { code: 'MY', name: '马来西亚' }, { code: 'TH', name: '泰国' }, { code: 'ID', name: '印尼' },
        { code: 'PH', name: '菲律宾' }, { code: 'IN', name: '印度' }, { code: 'JP', name: '日本' }, { code: 'KR', name: '韩国' },
        { code: 'DE', name: '德国' }, { code: 'FR', name: '法国' }, { code: 'ES', name: '西班牙' }, { code: 'IT', name: '意大利' },
        { code: 'PT', name: '葡萄牙' }, { code: 'NL', name: '荷兰' }, { code: 'SE', name: '瑞典' }, { code: 'NO', name: '挪威' },
        { code: 'DK', name: '丹麦' }, { code: 'FI', name: '芬兰' }, { code: 'PL', name: '波兰' }, { code: 'TR', name: '土耳其' },
        { code: 'BR', name: '巴西' }, { code: 'MX', name: '墨西哥' }, { code: 'AR', name: '阿根廷' }, { code: 'ZA', name: '南非' },
        { code: 'AE', name: '阿联酋' }, { code: 'SA', name: '沙特' }
      ],
      rules: {
        sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
        name: [{ required: true, message: '请输入源名称', trigger: 'blur' }],
        listId: [{ validator: (rule, value, cb) => {
          if (this.form.sourceType === 'mdl' && !(value && value.trim())) return cb(new Error('请输入 MDL List ID'))
          if (this.form.sourceType === 'letterboxd' && !(value && value.trim())) return cb(new Error('请输入 Letterboxd 片单链接'))
          cb()
        }, trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listMdlSource(this.queryParams).then(res => {
        this.list = (res.rows || []).map(r => ({ ...r, _running: false }))
        this.total = res.total
        this.loading = false
      })
    },
    objectTypeLabel(row) {
      if (row.sourceType !== 'justwatch') return '-'
      if (row.objectType === 'movie') return '电影'
      if (row.objectType === 'tv') return '剧集'
      return '全部'
    },
    sourceTypeLabel(type) {
      if (type === 'justwatch') return 'JustWatch'
      if (type === 'letterboxd') return 'Letterboxd'
      return 'MDL'
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, sourceType: null, name: null }; this.getList() },
    reset() {
      this.form = { sourceId: null, sourceType: 'mdl', name: null, listId: null, regionTag: null, objectType: '', maxPage: 1, targetCollectionId: null, enabled: '0', remark: null }
      this.providerOptions = []
      this.$refs.form && this.$refs.form.clearValidate()
    },
    cancel() { this.open = false; this.reset() },
    handleAdd() { this.reset(); this.open = true; this.title = '新增抓取源' },
    handleUpdate(row) {
      this.reset()
      getMdlSource(row.sourceId).then(res => {
        this.form = Object.assign({ sourceType: 'mdl', objectType: '' }, res.data)
        this.open = true
        this.title = '修改抓取源'
        if (this.form.sourceType === 'justwatch') { this.loadProviders() }
      })
    },
    handleTypeChange(type) {
      this.form.listId = null
      this.form.regionTag = type === 'justwatch' ? 'US' : null
      this.form.objectType = type === 'justwatch' ? '' : null
      this.$refs.form && this.$refs.form.clearValidate()
      if (type === 'justwatch') { this.loadProviders() }
    },
    loadProviders() {
      const country = this.form.regionTag || 'US'
      if (this.providerCache[country]) { this.providerOptions = this.providerCache[country]; return }
      this.providersLoading = true
      listJustWatchPackages(country).then(res => {
        const opts = (res.data || []).filter(p => p.slug && p.shortName)
        this.providerCache[country] = opts
        this.providerOptions = opts
      }).finally(() => { this.providersLoading = false })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const payload = Object.assign({}, this.form)
        if (payload.sourceType === 'justwatch') {
          payload.regionTag = payload.regionTag || 'US'
          if (!payload.objectType) { payload.objectType = null }
        }
        if (payload.sourceType === 'letterboxd') {
          payload.regionTag = 'global'
          payload.objectType = null
        }
        const fn = payload.sourceId ? updateMdlSource : addMdlSource
        fn(payload).then(() => { this.$modal.msgSuccess('保存成功'); this.open = false; this.getList() })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除源「' + row.name + '」？').then(() => delMdlSource(row.sourceId)).then(() => {
        this.getList(); this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleRun(row) {
      row._running = true
      this.$modal.msgInfo('抓取任务已启动，可能耗时较长…')
      runMdlSource(row.sourceId).then(res => {
        this.$modal.msgSuccess((res.msg || '抓取完成'))
        this.getList()
      }).finally(() => { row._running = false })
    },
    handleRunAll() {
      this.$modal.confirm('确认运行全部启用的抓取源（MDL + JustWatch）？可能耗时较长。').then(() => runAllMdlSource()).then(res => {
        this.$modal.msgSuccess(res.msg || '抓取完成'); this.getList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.jw-tip { margin-left: 10px; color: #909399; font-size: 12px; }
</style>
