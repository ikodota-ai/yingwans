<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
      <el-form-item prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="片名 / 原名" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="mediaType">
        <el-select v-model="queryParams.mediaType" placeholder="类型" clearable style="width: 120px">
          <el-option label="电影" value="movie" />
          <el-option label="剧集" value="tv" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="queryParams.lackResource">仅看无资源</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" row-key="mediaId" @expand-change="loadUsers">
      <el-table-column type="expand">
        <template slot-scope="props">
          <div v-loading="props.row._usersLoading" style="padding: 4px 24px">
            <el-table :data="props.row._users || []" size="mini" max-height="260">
              <el-table-column prop="username" label="用户名" width="160" />
              <el-table-column prop="nickname" label="昵称" width="160" />
              <el-table-column prop="wishedTime" label="想看时间" width="180" />
            </el-table>
            <div v-if="props.row._users && !props.row._users.length" style="color: #909399; padding: 8px 0">暂无想看用户</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="海报" width="70" align="center">
        <template slot-scope="scope">
          <el-image v-if="scope.row.posterLocalUrl" :src="imgUrl(scope.row.posterLocalUrl)" fit="cover" style="width: 40px; height: 56px; border-radius: 3px" lazy />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="片名" min-width="220" show-overflow-tooltip>
        <template slot-scope="scope">
          <div>{{ scope.row.title }}</div>
          <div v-if="scope.row.originalTitle && scope.row.originalTitle !== scope.row.title" style="color: #909399; font-size: 12px">{{ scope.row.originalTitle }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="70" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.mediaType === 'tv' ? 'warning' : 'success'">{{ scope.row.mediaType === 'tv' ? '剧集' : '电影' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="releaseDate" label="上映" width="100" align="center" />
      <el-table-column label="想看人数" prop="wishCount" width="90" align="center" sortable />
      <el-table-column prop="lastWishTime" label="最近想看" width="160" align="center" />
      <el-table-column label="下载资源" width="90" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.resourceCount > 0" size="mini" type="success">{{ scope.row.resourceCount }}</el-tag>
          <el-tag v-else size="mini" type="danger">无</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="观看平台" width="90" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.providerCount > 0" size="mini" type="success">{{ scope.row.providerCount }}</el-tag>
          <el-tag v-else size="mini" type="info">无</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="90" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" type="info">{{ scope.row.source || 'tmdb' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listWishMedia, listWishUsers } from '@/api/kemovie/wish'

export default {
  name: 'WishAdmin',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 20, keyword: '', mediaType: '', lackResource: false }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listWishMedia(this.queryParams).then(res => {
        this.list = res.rows
        this.total = res.total
      }).finally(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 20, keyword: '', mediaType: '', lackResource: false }
      this.getList()
    },
    loadUsers(row, expandedRows) {
      if (!expandedRows.length || row._users) return
      this.$set(row, '_usersLoading', true)
      listWishUsers(row.mediaId).then(res => {
        this.$set(row, '_users', res.data || [])
      }).finally(() => this.$set(row, '_usersLoading', false))
    },
    imgUrl(url) {
      if (!url) return ''
      if (/^https?:\/\//.test(url)) return url
      return process.env.VUE_APP_BASE_API + url
    }
  }
}
</script>
