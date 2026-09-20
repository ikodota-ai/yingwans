<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入影片标题" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="mediaType">
        <el-select v-model="queryParams.mediaType" placeholder="全部" clearable style="width: 120px">
          <el-option label="电影" value="movie" />
          <el-option label="剧集" value="tv" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-download" size="mini" @click="openSync" v-hasPermi="['kemovie:media:sync']">TMDB 同步</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['kemovie:media:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="ID" align="center" prop="mediaId" width="60" />
      <el-table-column label="海报" align="center" width="70">
        <template slot-scope="scope">
          <el-image v-if="scope.row.posterPath" :src="poster(scope.row)" style="width: 40px; height: 60px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" width="70">
        <template slot-scope="scope">{{ scope.row.mediaType === 'movie' ? '电影' : '剧集' }}</template>
      </el-table-column>
      <el-table-column label="地区" align="center" prop="region" width="70" />
      <el-table-column label="上映" align="center" prop="releaseDate" width="110" />
      <el-table-column label="站内评分" align="center" prop="ratingAvg" width="80" />
      <el-table-column label="热度" align="center" prop="hotScore" width="90" />
      <el-table-column label="可见" align="center" width="70">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.visible" active-value="0" inactive-value="1" @change="toggleVisible(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['kemovie:media:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-user" :loading="scope.row._castLoading" @click="handleSyncCast(scope.row)" v-hasPermi="['kemovie:media:sync']">演职员</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['kemovie:media:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 影片详情 / 编辑 -->
    <el-dialog :title="'影片详情 · ' + (form.title || '')" :visible.sync="open" width="880px" append-to-body custom-class="media-detail-dlg">
      <!-- 海报与剧照 -->
      <div class="media-images">
        <el-image :src="poster(form)" class="mi-poster" fit="cover">
          <div slot="error" class="mi-img-fallback"><i class="el-icon-picture-outline" /></div>
        </el-image>
        <el-image :src="backdrop(form)" class="mi-backdrop" fit="cover">
          <div slot="error" class="mi-img-fallback"><i class="el-icon-picture-outline" /></div>
        </el-image>
      </div>

      <el-divider content-position="left">基础信息（可编辑）</el-divider>
      <el-form ref="form" :model="form" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标题"><el-input v-model="form.title" placeholder="中文标题" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原名"><el-input v-model="form.originalTitle" placeholder="original title" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上映日期">
              <el-date-picker v-model="form.releaseDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(分)">
              <el-input-number v-model="form.runtime" :min="0" :step="1" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" filterable allow-create default-first-option clearable placeholder="TMDB 状态" style="width:100%">
                <el-option v-for="st in statusOptions" :key="st" :label="st" :value="st" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="国家/地区"><el-input v-model="form.region" placeholder="ISO 码，逗号分隔，如 US,KR,TH" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型标签"><el-input v-model="form.genres" placeholder="逗号分隔，如 剧情,科幻" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="TMDB评分">
              <el-input-number v-model="form.voteAverage" :min="0" :max="10" :precision="1" :step="0.1" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评分人数">
              <el-input-number v-model="form.voteCount" :min="0" :step="100" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="TMDB热度">
              <el-input-number v-model="form.popularity" :min="0" :precision="2" :step="1" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="简介"><el-input v-model="form.overview" type="textarea" :rows="4" show-word-limit maxlength="2000" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否上架">
              <el-radio-group v-model="form.visible">
                <el-radio label="0">上架</el-radio>
                <el-radio label="1">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-divider content-position="left">自定义服务商（勾选表示该片在该平台可观看）</el-divider>
      <div v-if="providerOptions.length" class="provider-checks">
        <el-checkbox-group v-model="form.providerIds">
          <el-checkbox v-for="p in providerOptions" :key="p.providerId" :label="p.providerId">
            {{ p.name }}<span v-if="p.region" class="pv-region">{{ p.region }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <div v-else class="provider-empty">暂无启用的服务商，可先到「影弯管理 → 服务商管理」添加</div>

      <el-divider content-position="left">下载资源（仅后台可见，用于生成资源文档）</el-divider>
      <div class="res-area">
        <div v-if="resources.length" class="res-list">
          <div v-for="r in resources" :key="r.resourceId" class="res-item" :class="{ 'res-editing': r.resourceId === resEditingId }">
            <el-tag size="mini" effect="plain">{{ r.groupName || '资源' }}</el-tag>
            <el-tag size="mini" type="success" effect="plain">{{ platformLabel(r.platform) }}</el-tag>
            <span class="res-url" :title="r.url">{{ r.url }}</span>
            <span v-if="r.pwd" class="res-pwd">提取码:{{ r.pwd }}</span>
            <el-button size="mini" type="text" icon="el-icon-edit" class="res-del" @click="editResource(r)" />
            <el-button size="mini" type="text" icon="el-icon-delete" class="res-del" @click="removeResource(r)" />
          </div>
        </div>
        <div v-else class="provider-empty">暂无资源</div>
        <div class="res-add">
          <el-input v-model="resForm.groupName" placeholder="分组(如 第一季)" size="small" style="width:130px" />
          <el-select v-model="resForm.platform" placeholder="平台" size="small" style="width:120px">
            <el-option v-for="(l, v) in platformOptions" :key="v" :label="l" :value="v" />
          </el-select>
          <el-input v-model="resForm.url" placeholder="资源链接 https://..." size="small" style="flex:1" />
          <el-input v-model="resForm.pwd" placeholder="提取码" size="small" style="width:100px" />
          <el-button v-if="!resEditingId" type="primary" size="small" :loading="resSaving" @click="addResource">添加</el-button>
          <template v-else>
            <el-button type="primary" size="small" :loading="resSaving" @click="saveResource">保存</el-button>
            <el-button size="small" @click="cancelEditResource">取消</el-button>
          </template>
        </div>
        <div v-if="resEditingId" class="doc-tip">正在编辑资源 #{{ resEditingId }}（网盘失效直接替换链接/提取码）</div>
      </div>

      <el-divider content-position="left">资源文档（前台详情页「下载资源」展示的链接）</el-divider>
      <el-form label-width="92px">
        <el-form-item label="飞书文档">
          <div class="doc-row">
            <el-link v-if="form.feishuDocUrl" :href="form.feishuDocUrl" target="_blank" type="primary" class="doc-link">{{ form.feishuDocUrl }}</el-link>
            <span v-else class="provider-empty">未生成</span>
            <el-button size="small" type="primary" plain :loading="docGenerating" @click="genFeishuDoc">
              {{ form.feishuDocUrl ? '更新文档' : '生成文档' }}
            </el-button>
            <el-button size="small" plain :loading="docTesting" @click="testFeishu">测试连接</el-button>
          </div>
          <div class="doc-tip">根据上方资源列表自动创建/重写飞书文档（需配置飞书应用凭证）</div>
        </el-form-item>
        <el-form-item label="腾讯文档">
          <el-input v-model="form.tencentDocUrl" placeholder="手动维护腾讯文档链接，如 https://docs.qq.com/..." />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">外部标识（只读）</el-divider>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="站内ID">{{ show(form.mediaId) }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeLabel(form.mediaType) }}</el-descriptions-item>
        <el-descriptions-item label="TMDB ID">{{ show(form.tmdbId) }}</el-descriptions-item>
        <el-descriptions-item label="IMDB ID">{{ show(form.imdbId) }}</el-descriptions-item>
        <el-descriptions-item label="MDL ID">{{ show(form.mdlId) }}</el-descriptions-item>
        <el-descriptions-item label="删除标记">{{ delFlagLabel(form.delFlag) }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">站内统计（系统计算，只读）</el-divider>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="想看/点赞">{{ show(form.likeCount) }}</el-descriptions-item>
        <el-descriptions-item label="期待/订阅">{{ show(form.wishCount) }}</el-descriptions-item>
        <el-descriptions-item label="看过">{{ show(form.watchedCount) }}</el-descriptions-item>
        <el-descriptions-item label="站内评分">{{ show(form.ratingAvg) }}</el-descriptions-item>
        <el-descriptions-item label="站内评分人数">{{ show(form.ratingCount) }}</el-descriptions-item>
        <el-descriptions-item label="综合热度">{{ show(form.hotScore) }}</el-descriptions-item>
        <el-descriptions-item label="最后同步" :span="3">{{ show(form.lastSyncedAt) }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">操作记录（只读）</el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="创建人">{{ show(form.createBy) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ show(form.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新人">{{ show(form.updateBy) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ show(form.updateTime) }}</el-descriptions-item>
      </el-descriptions>

      <div slot="footer">
        <el-button type="primary" @click="submitForm">保 存</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- TMDB 同步 -->
    <el-dialog title="从 TMDB 同步影片" :visible.sync="syncOpen" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="TMDB ID"><el-input-number v-model="syncForm.tmdbId" :min="1" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="syncForm.type">
            <el-radio label="movie">电影</el-radio>
            <el-radio label="tv">剧集</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" :loading="syncing" @click="doSync">同 步</el-button>
        <el-button @click="syncOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAdminMedia, getAdminMedia, updateAdminMedia, delAdminMedia, syncTmdb, syncMediaCast, listEnabledProviders, listMediaResources, addMediaResource, delMediaResource, updateMediaResource, generateFeishuDoc, testFeishuConnection } from '@/api/kemovie/admin'

const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p'

export default {
  name: 'KmMedia',
  data() {
    return {
      loading: true, total: 0, list: [],
      ids: [], multiple: true,
      open: false, form: {},
      statusOptions: ['Released','Returning Series','Planned','In Production','Post Production','Rumored','Ended','Canceled','Pilot'],
      syncOpen: false, syncing: false, syncForm: { tmdbId: null, type: 'movie' },
      queryParams: { pageNum: 1, pageSize: 10, title: null, mediaType: null },
      providerOptions: [],
      resources: [], resSaving: false, docGenerating: false, docTesting: false, resEditingId: null,
      resForm: { groupName: null, platform: 'baidu', url: null, pwd: null },
      platformOptions: { baidu: '百度网盘', '115': '115网盘', quark: '夸克网盘', aliyun: '阿里云盘', thunder: '迅雷云盘', magnet: '磁力链接', other: '其他' }
    }
  },
  created() {
    this.getList()
    listEnabledProviders().then(res => { this.providerOptions = res.data || [] })
  },
  methods: {
    handleSyncCast(row) {
      this.$set(row, '_castLoading', true)
      syncMediaCast(row.mediaId).then(res => {
        this.$modal.msgSuccess(res.msg || '同步完成')
      }).finally(() => { this.$set(row, '_castLoading', false) })
    },
    poster(row) {
      if (row.posterLocalUrl) {
        const u = row.posterLocalUrl
        if (/^(https?:)?\/\//.test(u)) return u
        return (process.env.VUE_APP_BASE_API || '') + u
      }
      return TMDB_IMAGE_BASE + '/w92' + row.posterPath
    },
    backdrop(row) {
      if (!row) return ''
      const u = row.backdropLocalUrl
      if (u) {
        if (/^(https?:)?\/\//.test(u)) return u
        return (process.env.VUE_APP_BASE_API || '') + u
      }
      return row.backdropPath ? TMDB_IMAGE_BASE + '/w780' + row.backdropPath : ''
    },
    show(v) {
      return (v === null || v === undefined || v === '') ? '—' : v
    },
    typeLabel(t) {
      return t === 'tv' ? '剧集' : t === 'movie' ? '电影' : this.show(t)
    },
    delFlagLabel(f) {
      if (f === '0') return '正常'
      if (f === '2' || f === '1') return '已删除'
      return this.show(f)
    },
    getList() {
      this.loading = true
      listAdminMedia(this.queryParams).then(res => {
        this.list = res.rows || []; this.total = res.total; this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, title: null, mediaType: null }; this.getList() },
    handleSelectionChange(sel) { this.ids = sel.map(i => i.mediaId); this.multiple = !sel.length },
    handleUpdate(row) {
      getAdminMedia(row.mediaId).then(res => {
        this.form = res.data
        this.$set(this.form, 'providerIds', res.data.providerIds || [])
        this.open = true
        this.loadResources(row.mediaId)
      })
    },
    platformLabel(p) { return this.platformOptions[p] || p },
    loadResources(mediaId) {
      listMediaResources(mediaId).then(res => { this.resources = res.data || [] })
    },
    addResource() {
      if (!this.resForm.url) { this.$modal.msgWarning('请填写资源链接'); return }
      this.resSaving = true
      addMediaResource({ mediaId: this.form.mediaId, ...this.resForm }).then(() => {
        this.$modal.msgSuccess(this.resSuccessMsg('已添加'))
        this.resForm = { groupName: null, platform: 'baidu', url: null, pwd: null }
        this.loadResources(this.form.mediaId)
      }).finally(() => { this.resSaving = false })
    },
    resSuccessMsg(base) {
      return this.form.feishuDocUrl ? base + '，请点「更新文档」同步到飞书' : base
    },
    editResource(r) {
      this.resEditingId = r.resourceId
      this.resForm = { groupName: r.groupName, platform: r.platform, url: r.url, pwd: r.pwd }
    },
    cancelEditResource() {
      this.resEditingId = null
      this.resForm = { groupName: null, platform: 'baidu', url: null, pwd: null }
    },
    saveResource() {
      if (!this.resForm.url) { this.$modal.msgWarning('请填写资源链接'); return }
      this.resSaving = true
      updateMediaResource({ resourceId: this.resEditingId, mediaId: this.form.mediaId, ...this.resForm }).then(() => {
        this.$modal.msgSuccess(this.resSuccessMsg('已保存'))
        this.cancelEditResource()
        this.loadResources(this.form.mediaId)
      }).finally(() => { this.resSaving = false })
    },
    removeResource(r) {
      this.$modal.confirm('删除该资源？').then(() => delMediaResource(r.resourceId)).then(() => {
        if (this.resEditingId === r.resourceId) { this.cancelEditResource() }
        this.loadResources(this.form.mediaId); this.$modal.msgSuccess(this.resSuccessMsg('已删除'))
      }).catch(() => {})
    },
    genFeishuDoc() {
      this.docGenerating = true
      generateFeishuDoc(this.form.mediaId).then(res => {
        this.$modal.msgSuccess(res.msg || '已生成/更新')
        if (res.url) this.$set(this.form, 'feishuDocUrl', res.url)
      }).finally(() => { this.docGenerating = false })
    },
    testFeishu() {
      this.docTesting = true
      testFeishuConnection().then(res => {
        this.$modal.msgSuccess(res.msg || '连接正常')
      }).catch(() => {}).finally(() => { this.docTesting = false })
    },
    submitForm() {
      updateAdminMedia(this.form).then(() => { this.$modal.msgSuccess('保存成功'); this.open = false; this.getList() })
    },
    toggleVisible(row) {
      updateAdminMedia({ mediaId: row.mediaId, visible: row.visible }).then(() => this.$modal.msgSuccess('已更新'))
    },
    handleDelete(row) {
      const ids = row.mediaId || this.ids.join(',')
      this.$modal.confirm('确认删除所选影片？').then(() => delAdminMedia(ids)).then(() => {
        this.getList(); this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    openSync() { this.syncForm = { tmdbId: null, type: 'movie' }; this.syncOpen = true },
    doSync() {
      if (!this.syncForm.tmdbId) { this.$modal.msgWarning('请输入 TMDB ID'); return }
      this.syncing = true
      syncTmdb(this.syncForm.tmdbId, this.syncForm.type).then(res => {
        this.$modal.msgSuccess('同步成功，可在弹窗中直接维护下载资源'); this.syncOpen = false; this.getList()
        if (res.data && res.data.mediaId) { this.handleUpdate(res.data) }
      }).finally(() => { this.syncing = false })
    }
  }
}
</script>

<style scoped>
.media-detail-dlg >>> .el-dialog__body { max-height: 70vh; overflow-y: auto; padding-top: 10px; }
.media-images { display: flex; gap: 12px; }
.mi-poster { width: 96px; height: 140px; border-radius: 8px; background: #f5f7fa; flex: none; }
.mi-backdrop { flex: 1; height: 140px; border-radius: 8px; background: #f5f7fa; }
.mi-img-fallback { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 26px; }
.media-detail-dlg >>> .el-divider__text { font-weight: 600; color: #303133; }
.provider-checks { padding: 0 8px 6px; }
.provider-checks .el-checkbox { margin-right: 18px; }
.pv-region { color: #909399; font-size: 12px; margin-left: 4px; }
.provider-empty { color: #909399; font-size: 12px; padding: 0 8px 6px; }
.res-area { padding: 0 8px 6px; }
.res-list { margin-bottom: 8px; }
.res-item { display: flex; align-items: center; gap: 6px; padding: 3px 0; border-bottom: 1px dashed #ebeef5; }
.res-url { flex: 1; font-size: 12px; color: #606266; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.res-pwd { font-size: 12px; color: #e6a23c; white-space: nowrap; }
.res-add { display: flex; gap: 8px; align-items: center; }
.res-editing { background: #fdf6ec; border-radius: 4px; }
.doc-row { display: flex; align-items: center; gap: 10px; }
.doc-link { max-width: 480px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-tip { font-size: 12px; color: #909399; margin-top: 2px; }
</style>
