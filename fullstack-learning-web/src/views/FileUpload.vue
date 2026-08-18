<template>
  <el-card>
    <template #header>
      <h3>📸 MinIO 分布式文件上传实战 (Day 10)</h3>
    </template>

    <div class="upload-container">
      <el-upload class="avatar-uploader" action="/api/file/upload" :headers="uploadHeaders" :show-file-list="false"
        :on-success="handleUploadSuccess" :before-upload="beforeAvatarUpload">
        <img v-if="imageUrl" :src="imageUrl" class="avatar" />
        <el-icon v-else class="avatar-uploader-icon">
          <Plus />
        </el-icon>
      </el-upload>
      <div style="margin-top: 10px; color: #909399; font-size: 13px;">
        支持 jpg/png 格式，单张图片不超过 2MB
      </div>
    </div>

    <!-- 显示上传成功后的文件链接 -->
    <div v-if="imageUrl" class="url-display">
      <p><b>文件线上访问地址：</b></p>
      <el-input :value="imageUrl" readonly>
        <template #append>
          <el-button @click="copyUrl">复制链接</el-button>
        </template>
      </el-input>
    </div>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const imageUrl = ref('')

// 上传需要带上 JWT Token 身份认证
const uploadHeaders = ref({
  Authorization: `Bearer ${localStorage.getItem('token')}`
})

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    imageUrl.value = response.data
    ElMessage.success('上传成功！')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const beforeAvatarUpload = (rawFile) => {
  const isImage = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png'
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('上传头像图片只能是 JPG/PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!')
    return false
  }
  return true
}

const copyUrl = () => {
  navigator.clipboard.writeText(imageUrl.value)
  ElMessage.success('已复制到剪贴板！')
}
</script>

<style scoped>
.upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.avatar-uploader .avatar {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
  border-radius: 6px;
}
</style>

<style>
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
}
</style>