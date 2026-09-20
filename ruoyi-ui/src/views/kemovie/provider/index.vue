<template>
  <div class="app-container">
    <el-alert title="自定义播放服务商（如 GagaOOLala / LINE TV / HamiVideo / WeTV）。启用后可在「影视库 → 影片编辑」中为影片勾选可观看平台，勾选结果会展示到前台影片详情页。" type="info" :closable="false" class="mb8" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="mb8">
      <el-form-item prop="name">
        <el-input v-model="queryParams.name" placeholder="名称" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['kemovie:provider:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="providerId" width="60" />
      <el-table-column label="Logo" align="center" width="70">
        <template slot-scope="scope">
          <el-image v-if="scope.row.logoUrl" :src="scope.row.logoUrl" style="width: 36px; height: 36px; border-radius: 6px" fit="cover" />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="标识" align="center" prop="slug" width="110" :show-overflow-tooltip="true" />
      <el-table-column label="区域" align="center" prop="region" width="80" />
      <el-table-column label="官网" align="center" prop="siteUrl" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <el-link v-if="scope.row.siteUrl" :href="scope.row.siteUrl" target="_blank" type="primary">{{ scope.row.siteUrl }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="70" />
      <el-table-column label="启用" align="center" width="70">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.enabled" active-value="0" inactive-value="1" @change="toggleEnabled(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['kemovie:provider:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['kemovie:provider:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="如 GagaOOLala" /></el-form-item>
        <el-form-item label="标识" prop="slug"><el-input v-model="form.slug" placeholder="可选，如 gagaoolala" /></el-form-item>
        <el-form-item label="官网" prop="siteUrl"><el-input v-model="form.siteUrl" placeholder="https://..." /></el-form-item>
        <el-form-item label="Logo" prop="logoUrl"><el-input v-model="form.logoUrl" placeholder="Logo 图片地址（可选）" /></el-form-item>
        <el-form-item label="区域" prop="region"><el-input v-model="form.region" placeholder="如 TW / HK / US" style="width: 160px" /></el-form-item>
        <el-form-item label="排序" prop="sort"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="是否启用" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProvider, getProvider, addProvider, updateProvider, delProvider } from '@/api/kemovie/admin'

export default {
  name: 'KmProvider',
  data() {
    return {
      loading: true, total: 0, list: [],
      title: '', open: false, form: {},
      queryParams: { pageNum: 1, pageSize: 10, name: null },
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listProvider(this.queryParams).then(res => {
        this.list = res.rows || []; this.total = res.total; this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, name: null }; this.getList() },
    reset() { this.form = { providerId: null, name: null, slug: null, siteUrl: null, logoUrl: null, region: null, enabled: '0', sort: 0, remark: null } },
    handleAdd() { this.reset(); this.title = '新增服务商'; this.open = true },
    handleUpdate(row) {
      getProvider(row.providerId).then(res => { this.form = res.data; this.title = '修改服务商'; this.open = true })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.providerId ? updateProvider(this.form) : addProvider(this.form)
        req.then(() => { this.$modal.msgSuccess('保存成功'); this.open = false; this.getList() })
      })
    },
    toggleEnabled(row) {
      updateProvider({ providerId: row.providerId, enabled: row.enabled }).then(() => this.$modal.msgSuccess('已更新'))
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除服务商「' + row.name + '」？影片上已勾选的该服务商展示会一并清除。').then(() => delProvider(row.providerId)).then(() => {
        this.getList(); this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
