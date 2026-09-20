<template>
  <div class="app-container">
    <el-tabs v-model="tab" @tab-click="load">
      <el-tab-pane label="订阅汇总" name="summary" />
      <el-tab-pane label="订阅动态" name="events" />
    </el-tabs>

    <el-table v-if="tab === 'summary'" v-loading="loading" :data="list">
      <el-table-column label="影片ID" prop="mediaId" width="80" align="center" />
      <el-table-column label="影片" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="类型" width="80" align="center">
        <template slot-scope="scope">{{ scope.row.mediaType === 'movie' ? '电影' : '剧集' }}</template>
      </el-table-column>
      <el-table-column label="订阅人数" prop="subCount" width="100" align="center" sortable />
      <el-table-column label="已通知上线" prop="notifiedCount" width="100" align="center" />
      <el-table-column label="最近订阅时间" prop="lastTime" width="170" align="center" />
    </el-table>

    <el-table v-else v-loading="loading" :data="list">
      <el-table-column label="用户" width="140">
        <template slot-scope="scope">{{ scope.row.nickname || scope.row.username || ('#' + scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="动作" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" size="mini" type="success">订阅</el-tag>
          <el-tag v-else size="mini" type="info">取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="影片" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="时间" prop="updateTime" width="170" align="center" />
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="load" />
  </div>
</template>

<script>
import { listSubscriptionSummary, listSubscriptionEvents } from '@/api/kemovie/admin'

export default {
  name: 'KmSubscription',
  data() {
    return {
      tab: 'summary',
      loading: false,
      list: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 20 }
    }
  },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      const fn = this.tab === 'summary' ? listSubscriptionSummary : listSubscriptionEvents
      fn(this.queryParams).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    }
  }
}
</script>
