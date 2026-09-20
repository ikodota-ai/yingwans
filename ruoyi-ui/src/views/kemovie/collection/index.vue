<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
      <el-form-item label="片单名" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入片单名" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-select v-model="queryParams.source" placeholder="全部" clearable style="width: 120px">
          <el-option label="系统" value="system" />
          <el-option label="TMDB" value="tmdb" />
          <el-option label="MDL" value="mdl" />
          <el-option label="手动" value="manual" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['kemovie:collection:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="collectionId" width="60" />
      <el-table-column label="片单名" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="来源" align="center" prop="source" width="80" />
      <el-table-column label="影片数" align="center" prop="itemCount" width="80" />
      <el-table-column label="所有者" align="center" prop="ownerName" width="100" />
      <el-table-column label="公开" align="center" width="70">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isPublic === '0' ? 'success' : 'info'">{{ scope.row.isPublic === '0' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="150" />
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['kemovie:collection:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['kemovie:collection:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="480px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="片单名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="简介" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="来源" prop="source">
          <el-select v-model="form.source" placeholder="选择来源">
            <el-option label="系统" value="system" />
            <el-option label="手动" value="manual" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面URL" prop="coverUrl"><el-input v-model="form.coverUrl" /></el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-radio-group v-model="form.isPublic">
            <el-radio label="0">公开</el-radio>
            <el-radio label="1">私有</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAdminCollection, getAdminCollection, addAdminCollection, updateAdminCollection, delAdminCollection } from '@/api/kemovie/admin'

export default {
  name: 'KmCollection',
  data() {
    return {
      loading: true, total: 0, list: [],
      title: '', open: false, form: {},
      queryParams: { pageNum: 1, pageSize: 10, name: null, source: null },
      rules: { name: [{ required: true, message: '请输入片单名', trigger: 'blur' }] }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listAdminCollection(this.queryParams).then(res => {
        this.list = res.rows || []; this.total = res.total; this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, name: null, source: null }; this.getList() },
    reset() {
      this.form = { collectionId: null, name: null, description: null, source: 'manual', coverUrl: null, isPublic: '0' }
      this.resetForm && this.resetForm('form')
    },
    cancel() { this.open = false; this.reset() },
    handleAdd() { this.reset(); this.open = true; this.title = '新增片单' },
    handleUpdate(row) {
      this.reset()
      getAdminCollection(row.collectionId).then(res => { this.form = res.data; this.open = true; this.title = '修改片单' })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const fn = this.form.collectionId ? updateAdminCollection : addAdminCollection
        fn(this.form).then(() => { this.$modal.msgSuccess('保存成功'); this.open = false; this.getList() })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除片单「' + row.name + '」？').then(() => delAdminCollection(row.collectionId)).then(() => {
        this.getList(); this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
