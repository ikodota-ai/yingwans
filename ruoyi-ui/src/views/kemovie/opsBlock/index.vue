<template>
  <div class="app-container">
    <el-alert title="运营位用于在前台展示运营内容（如私域二维码）。blockKey 对应位置：home_side 首页侧栏、detail_footer 详情页底部。" type="info" :closable="false" class="mb8" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['kemovie:ops:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="blockId" width="60" />
      <el-table-column label="位置Key" align="center" prop="blockKey" width="130" />
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="图片" align="center" width="70">
        <template slot-scope="scope">
          <el-image v-if="scope.row.imageUrl" :src="scope.row.imageUrl" style="width: 40px; height: 40px" fit="cover" />
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
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['kemovie:ops:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['kemovie:ops:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="位置Key" prop="blockKey">
          <el-select v-model="form.blockKey" placeholder="选择展示位置" style="width: 100%">
            <el-option label="首页侧栏 (home_side)" value="home_side" />
            <el-option label="详情页底部 (detail_footer)" value="detail_footer" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="图片URL" prop="imageUrl"><el-input v-model="form.imageUrl" placeholder="如二维码图片地址" /></el-form-item>
        <el-form-item label="文本内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="跳转链接" prop="link"><el-input v-model="form.link" /></el-form-item>
        <el-form-item label="排序" prop="sort"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="是否启用" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
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
import { listOpsBlock, getOpsBlock, addOpsBlock, updateOpsBlock, delOpsBlock } from '@/api/kemovie/admin'

export default {
  name: 'KmOpsBlock',
  data() {
    return {
      loading: true, total: 0, list: [],
      title: '', open: false, form: {},
      queryParams: { pageNum: 1, pageSize: 10 },
      rules: {
        blockKey: [{ required: true, message: '请选择位置', trigger: 'change' }],
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listOpsBlock(this.queryParams).then(res => {
        this.list = res.rows || []; this.total = res.total; this.loading = false
      })
    },
    reset() {
      this.form = { blockId: null, blockKey: 'home_side', title: null, imageUrl: null, content: null, link: null, sort: 0, enabled: '0' }
      this.resetForm && this.resetForm('form')
    },
    cancel() { this.open = false; this.reset() },
    handleAdd() { this.reset(); this.open = true; this.title = '新增运营位' },
    handleUpdate(row) {
      this.reset()
      getOpsBlock(row.blockId).then(res => { this.form = res.data; this.open = true; this.title = '修改运营位' })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const fn = this.form.blockId ? updateOpsBlock : addOpsBlock
        fn(this.form).then(() => { this.$modal.msgSuccess('保存成功'); this.open = false; this.getList() })
      })
    },
    toggleEnabled(row) {
      updateOpsBlock({ blockId: row.blockId, enabled: row.enabled }).then(() => this.$modal.msgSuccess('已更新'))
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该运营位？').then(() => delOpsBlock(row.blockId)).then(() => {
        this.getList(); this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
