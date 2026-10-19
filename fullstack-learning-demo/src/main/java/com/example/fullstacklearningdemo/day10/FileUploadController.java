package com.example.fullstacklearningdemo.day10;

import com.example.fullstacklearningdemo.day02.Result;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileUploadController {

  @Autowired
  private MinioClient minioClient;

  @Value("${minio.bucketName}")
  private String bucketName;

  // 返回给浏览器的访问地址，未配置时退回内部 endpoint
  @Value("${minio.public-endpoint:${minio.endpoint}}")
  private String endpoint;

  @PostMapping("/upload")
  public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
      return Result.error(400, "上传文件不能为空");
    }

    try {
      // 获取原始文件名并生成唯一 UUID 文件名，避免冲突
      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }
      String objectName = UUID.randomUUID().toString() + extension;

      // 上传文件流到 MinIO
      InputStream inputStream = file.getInputStream();
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .stream(inputStream, file.getSize(), -1)
              .contentType(file.getContentType())
              .build());

      // 拼接文件访问外网 URL
      String fileUrl = endpoint + "/" + bucketName + "/" + objectName;
      return Result.success(fileUrl);

    } catch (Exception e) {
      e.printStackTrace();
      return Result.error(500, "文件上传失败：" + e.getMessage());
    }
  }
}