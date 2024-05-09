package com.nhjclxc.minio;


import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * MinIO模板类
 *
 * @author nhjclxc@163.com
 */
@Slf4j
public class MinIoTemplate {

    private final static String separator = "/";

    private final MinioClient minioClient;

    private final MinIoProperties minIoProperties;

    public MinIoTemplate(MinIoProperties minIoProperties) {
        this.minIoProperties = minIoProperties;
        this.minioClient = MinioClient.builder()
                .credentials(minIoProperties.getAccessKey(), minIoProperties.getSecretKey())
                .endpoint(minIoProperties.getEndpoint())
                .build();
    }

    /**
     * 构建文件路径
     *
     * @param dirPath  目录
     * @param filename 文件名{yyyy/mm/dd/file.jpg}
     * @return 文件路径
     */
    public String builderFilePath(String dirPath, String filename) {
        StringBuilder stringBuilder = new StringBuilder(50);
        if (dirPath != null && !"".equals(dirPath)) {
            stringBuilder.append(dirPath).append(separator);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String todayStr = sdf.format(new Date());
        stringBuilder.append(todayStr).append(separator);
        String[] parts = extractFileNameAndExtension(filename);
        stringBuilder.append(parts[0]);
        stringBuilder.append("-");
        stringBuilder.append(UUID.randomUUID().toString().replaceAll("-", ""));
        // 扩展名回填
        if (parts[1] != null){
            stringBuilder.append(".").append(parts[1]);
        }
        return stringBuilder.toString();
    }

    /**
     * 提取文件名与扩展名
     *
     * @param fileName 原始文件名称
     */
    public static String[] extractFileNameAndExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');

        if (lastDotIndex == -1) {
            // 没有扩展名
            return new String[]{fileName, null};
        }
        String name = fileName.substring(0, lastDotIndex);
        String extension = fileName.substring(lastDotIndex + 1);
        return new String[]{name, extension};
    }

    /**
     * 提取路径里面的文件名
     */
    public static String extractFileName(String filename) {
        try {
            int lastSlashIndex = filename.lastIndexOf('/');

            if (lastSlashIndex != -1) {
                return filename.substring(lastSlashIndex + 1);
            } else {
                return filename;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 上传图片文件
     *
     * @param prefix      文件前缀
     * @param filename    文件名
     * @param inputStream 文件流
     * @return 文件全路径
     */
    public String uploadFile(String prefix, String filename, InputStream inputStream, String type) {
        String filePath = builderFilePath(prefix, filename);
        try {
            String contentType = "application/octet-stream";
            if (type != null && !"".equals(type)){
                contentType = type;
            }

            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object(filePath)
                    .contentType(contentType)
                    .bucket(minIoProperties.getBucket()).stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(putObjectArgs);
            return filePath;
        } catch (Exception ex) {
            log.error("minio put file error.", ex);
            throw new RuntimeException("上传文件失败");
        }
    }

    /**
     * 删除文件
     *
     * @param pathUrl 文件全路径
     */
    public boolean delete(String pathUrl) {
        // 删除Objects
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket(minIoProperties.getBucket()).object(pathUrl).build();
        try {
            minioClient.removeObject(removeObjectArgs);
            return true;
        } catch (Exception e) {
            log.error("minio remove file error.  pathUrl:{}", pathUrl);
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 下载文件
     *
     * @param pathUrl 文件全路径
     * @return 文件流
     */
    public InputStream downLoadFile(String pathUrl) {
        InputStream inputStream = null;
        try {
            inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(minIoProperties.getBucket()).object(pathUrl).build());
        } catch (Exception e) {
            String msg = String.format("minio down file error. pathUrl: %s， msg = %s", pathUrl, e.getMessage());
            log.error(msg);
            e.printStackTrace();
            if (e.getMessage() != null && !"".equals(e.getMessage()) && e.getMessage().contains("The specified key does not exist")){
                msg = "文件不存在";
            }
            throw new RuntimeException(msg);
        }
//        byte[] bytes = IOUtils.toByteArray(inputStream);
        return inputStream;
    }
}
