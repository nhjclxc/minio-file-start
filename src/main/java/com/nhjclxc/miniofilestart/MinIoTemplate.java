//package com.nhjclxc.miniofilestart;
//
//
//import io.minio.GetObjectArgs;
//import io.minio.MinioClient;
//import io.minio.PutObjectArgs;
//import io.minio.RemoveObjectArgs;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.util.StringUtils;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.text.SimpleDateFormat;
//import java.util.Date;
///**
// * MinIO模板类
// *
// * @author nhjclxc@163.com
// */
//@Slf4j
//public class MinIoTemplate {
//
//    private final static String separator = "/";
//
//    @Autowired
//    private MinioClient minioClient;
//
//    private MinIoProperties minIoProperties;
//
//    public MinIoTemplate(MinIoProperties minIoProperties) {
//        this.minIoProperties = minIoProperties;
//    }
//
//    /**
//     * 构建文件路径
//     *
//     * @param dirPath  目录
//     * @param filename 文件名{yyyy/mm/dd/file.jpg}
//     * @return 文件路径
//     */
//    public String builderFilePath(String dirPath, String filename) {
//        StringBuilder stringBuilder = new StringBuilder(50);
//        if (!StringUtils.isEmpty(dirPath)) {
//            stringBuilder.append(dirPath).append(separator);
//        }
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
//        String todayStr = sdf.format(new Date());
//        stringBuilder.append(todayStr).append(separator);
//        stringBuilder.append(filename);
//        return stringBuilder.toString();
//    }
//
//    /**
//     * 上传图片文件
//     *
//     * @param prefix      文件前缀
//     * @param filename    文件名
//     * @param inputStream 文件流
//     * @return 文件全路径
//     */
//    public String uploadFile(String prefix, String filename, InputStream inputStream, String type) {
//        String filePath = builderFilePath(prefix, filename);
//        try {
//            String contentType = "image/jpg";
//            if ("html".equals(type)) {
//                contentType = "text/html";
//            }
//
//            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
//                    .object(filePath)
//                    .contentType(contentType)
//                    .bucket(minIoProperties.getBucket()).stream(inputStream, inputStream.available(), -1)
//                    .build();
//            minioClient.putObject(putObjectArgs);
//            StringBuilder urlPath = new StringBuilder(minIoProperties.getReadPath());
//            urlPath.append(separator + minIoProperties.getBucket());
//            urlPath.append(separator);
//            urlPath.append(filePath);
//            return urlPath.toString();
//        } catch (Exception ex) {
//            log.error("minio put file error.", ex);
//            throw new RuntimeException("上传文件失败");
//        }
//    }
//
//    /**
//     * 删除文件
//     *
//     * @param pathUrl 文件全路径
//     */
//    public boolean delete(String pathUrl) {
//        String key = pathUrl.replace(minIoProperties.getEndpoint() + "/", "");
//        int index = key.indexOf(separator);
//        String bucket = key.substring(0, index);
//        String filePath = key.substring(index + 1);
//        // 删除Objects
//        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket(bucket).object(filePath).build();
//        try {
//            minioClient.removeObject(removeObjectArgs);
//            return true;
//        } catch (Exception e) {
//            log.error("minio remove file error.  pathUrl:{}", pathUrl);
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//
//    /**
//     * 下载文件
//     *
//     * @param pathUrl 文件全路径
//     * @return 文件流
//     */
//    public byte[] downLoadFile(String pathUrl) {
//        String key = pathUrl.replace(minIoProperties.getEndpoint() + "/", "");
//        int index = key.indexOf(separator);
//        String bucket = key.substring(0, index);
//        String filePath = key.substring(index + 1);
//        InputStream inputStream = null;
//        try {
//            inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(filePath).build());
//        } catch (Exception e) {
//            log.error("minio down file error.  pathUrl:{}", pathUrl);
//            e.printStackTrace();
//        }
//
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        byte[] buff = new byte[100];
//        int rc = 0;
//        while (true) {
//            try {
//                if (!((rc = inputStream.read(buff, 0, 100)) > 0)) break;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            byteArrayOutputStream.write(buff, 0, rc);
//        }
//        return byteArrayOutputStream.toByteArray();
//    }
//}
