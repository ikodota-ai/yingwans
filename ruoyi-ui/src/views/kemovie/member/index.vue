<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
      <el-form-item label="账号" prop="username">
        <el-input v-model="queryParams.username" placeholder="登录账号" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="昵称" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="memberId" width="60" />
      <el-table-column label="账号" align="center" prop="username" />
      <el-table-column label="昵称" align="center" prop="nickname" />
      <el-table-column label="邮箱" align="center" prop="email" :show-overflow-tooltip="true" />
      <el-table-column label="手机" align="center" prop="phone" width="120" />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
            @change="handleStatusChange(scope.row)" v-hasPermi="['kemovie:member:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="最后登录" align="center" prop="loginDate" width="150" />
      <el-table-column label="注册时间" align="center" prop="createTime" width="150" />
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['kemovie:member:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-key" @click="handleResetPwd(scope.row)" v-hasPermi="['kemovie:member:resetPwd']">重置密码</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['kemovie:member:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="修改会员资料" :visible.sync="open" width="480px" append-to-body>
      <el-form ref="form" :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" disabled /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMember, updateMember, changeMemberStatus, resetMemberPwd, delMember } from '@/api/kemovie/admin'

export default {
  name: 'KmMember',
  data() {
    return {
      loading: true,
      total: 0,
      list: [],
      open: false,
      queryParams: { pageNum: 1, pageSize: 10, username: undefined, nickname: undefined, status: undefined },
      form: {}
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listMember(this.queryParams).then(res => {
        this.list = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '停用'
      this.$modal.confirm('确认要「' + text + '」会员「' + row.username + '」吗？').then(() => {
        return changeMemberStatus(row.memberId, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    handleUpdate(row) {
      this.form = { memberId: row.memberId, username: row.username, nickname: row.nickname, email: row.email, phone: row.phone, remark: row.remark }
      this.open = true
    },
    submitForm() {
      updateMember(this.form).then(() => {
        this.$modal.msgSuccess('修改成功')
        this.open = false
        this.getList()
      })
    },
    handleResetPwd(row) {
      this.$prompt('请输入会员「' + row.username + '」的新密码', '重置密码', {
        confirmButtonText: '确定', cancelButtonText: '取消',
        inputPattern: /^.{5,20}$/, inputErrorMessage: '密码长度需在 5 到 20 个字符之间'
      }).then(({ value }) => {
        return resetMemberPwd(row.memberId, value)
      }).then(() => {
        this.$modal.msgSuccess('重置成功')
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除会员「' + row.username + '」？').then(() => {
        return delMember(row.memberId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
