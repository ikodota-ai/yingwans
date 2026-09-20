<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
      <el-form-item label="姓名" prop="name">
        <el-input v-model="queryParams.name" placeholder="姓名/原名" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="领域" prop="knownFor">
        <el-select v-model="queryParams.knownFor" placeholder="全部" clearable style="width: 140px">
          <el-option label="演员" value="Acting" />
          <el-option label="导演" value="Directing" />
          <el-option label="编剧" value="Writing" />
          <el-option label="制片" value="Production" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="personId" width="60" />
      <el-table-column label="头像" align="center" width="70">
        <template slot-scope="scope">
          <el-image :src="avatar(scope.row)" style="width:40px;height:56px;border-radius:4px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="姓名" align="center" prop="name" />
      <el-table-column label="原名" align="center" prop="originalName" :show-overflow-tooltip="true" />
      <el-table-column label="领域" align="center" prop="knownFor" width="100" />
      <el-table-column label="性别" align="center" width="70">
        <template slot-scope="scope">{{ genderText(scope.row.gender) }}</template>
      </el-table-column>
      <el-table-column label="TMDB" align="center" prop="tmdbId" width="90" />
      <el-table-column label="热度" align="center" prop="popularity" width="90" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">作品</el-button>
          <el-button size="mini" type="text" icon="el-icon-refresh" :loading="scope.row._loading" @click="handleRefresh(scope.row)" v-hasPermi="['kemovie:person:edit']">TMDB刷新</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['kemovie:person:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="detail ? detail.name : '人物详情'" :visible.sync="open" width="640px" append-to-body>
      <div v-if="detail" class="person-detail">
        <div class="head">
          <el-image :src="avatar(detail)" style="width:100px;height:140px;border-radius:6px" fit="cover" />
          <div class="meta">
            <p v-if="detail.originalName"><b>原名：</b>{{ detail.originalName }}</p>
            <p><b>性别：</b>{{ genderText(detail.gender) }}</p>
            <p v-if="detail.birthday"><b>生日：</b>{{ detail.birthday }}</p>
            <p v-if="detail.placeOfBirth"><b>出生地：</b>{{ detail.placeOfBirth }}</p>
            <p v-if="detail.knownFor"><b>领域：</b>{{ detail.knownFor }}</p>
          </div>
        </div>
        <p v-if="detail.biography" class="bio">{{ detail.biography }}</p>
        <h4>站内参演作品（{{ (detail.credits || []).length }}）</h4>
        <el-table :data="detail.credits" size="mini" max-height="260">
          <el-table-column label="作品" prop="mediaTitle" />
          <el-table-column label="类型" width="80">
            <template slot-scope="s">{{ s.row.creditType === 'cast' ? '演员' : (s.row.job || '剧组') }}</template>
          </el-table-column>
          <el-table-column label="角色/职务" prop="character" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPerson, getAdminPerson, refreshPerson, delPerson } from '@/api/kemovie/admin'

const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p'

export default {
  name: 'KmPerson',
  data() {
    return {
      loading: true, total: 0, list: [],
      open: false, detail: null,
      queryParams: { pageNum: 1, pageSize: 10, name: undefined, knownFor: undefined }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listPerson(this.queryParams).then(res => {
        this.list = res.rows; this.total = res.total; this.loading = false
      })
    },
    avatar(p) {
      if (p.profileLocalUrl) return p.profileLocalUrl
      if (p.profilePath) return p.profilePath.startsWith('http') ? p.profilePath : TMDB_IMAGE_BASE + '/w185' + p.profilePath
      return require('@/assets/images/profile.jpg')
    },
    genderText(g) { return { 1: '女', 2: '男' }[g] || '未知' },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleView(row) {
      getAdminPerson(row.personId).then(res => { this.detail = res.data; this.open = true })
    },
    handleRefresh(row) {
      this.$set(row, '_loading', true)
      refreshPerson(row.personId).then(() => {
        this.$modal.msgSuccess('刷新成功'); this.getList()
      }).finally(() => { this.$set(row, '_loading', false) })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除人物「' + row.name + '」？').then(() => {
        return delPerson(row.personId)
      }).then(() => { this.getList(); this.$modal.msgSuccess('删除成功') }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.person-detail .head { display: flex; gap: 16px; }
.person-detail .meta p { margin: 4px 0; font-size: 13px; }
.person-detail .bio { margin: 14px 0; font-size: 13px; color: #666; line-height: 1.7; max-height: 160px; overflow: auto; }
.person-detail h4 { margin: 10px 0; }
</style>
