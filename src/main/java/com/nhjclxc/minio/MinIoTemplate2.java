//package com.nhjclxc.minio;
//
//
//import io.minio.*;
//import io.minio.errors.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.util.StringUtils;
//
//import java.io.ByteArrayInputStream;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
///**
// * MinIO模板类
// *
// * @author LuoXianchao
// */
//@Slf4j
//public class MinIoTemplate2 {
//
//    private final static String separator = "/";
//
//    private final MinioClient minioClient;
//
//    private final MinIoProperties minIoProperties;
//
//    private static final List<String> IMAGE_EXTENSION_NAME_LIST = Arrays.asList("jpg", "jpeg", "png", "bmp", "tiff", "tif", "webp");
//    private static final List<String> FILE_EXTENSION_NAME_LIST = Arrays.asList("xml", "json", "pdf", "msword", "octet-stream");
//    private static final List<String> TEXT_EXTENSION_NAME_LIST = Arrays.asList("txt");
//
//    public MinIoTemplate2(MinIoProperties minIoProperties) {
//        this.minIoProperties = minIoProperties;
//        this.minioClient = MinioClient.builder()
//                .credentials(minIoProperties.getAccessKey(), minIoProperties.getSecretKey())
//                .endpoint(minIoProperties.getEndpoint())
//                .build();
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
//        String[] parts = extractFileNameAndExtension(filename);
//        // 不保留原来的文件名
////        stringBuilder.append(parts[0]);
//        stringBuilder.append(System.currentTimeMillis());
//        stringBuilder.append("-");
//        stringBuilder.append(UUID.randomUUID().toString().replaceAll("-", ""));
//        // 扩展名回填
//        if (parts[1] != null) {
//            stringBuilder.append(".").append(parts[1]);
//        }
//        return stringBuilder.toString();
//    }
//
//    /**
//     * 提取文件名与扩展名
//     *
//     * @param fileName 原始文件名称 "/path/to/bigfile.zip"
//     * @return [0] = /path/to/bigfile, [1] = zip
//     * @author 罗贤超
//     */
//    public static String[] extractFileNameAndExtension(String fileName) {
//        int lastDotIndex = fileName.lastIndexOf('.');
//
//        if (lastDotIndex == -1) {
//            // 没有扩展名
//            return new String[]{fileName, null};
//        }
//        String name = fileName.substring(0, lastDotIndex);
//        String extension = fileName.substring(lastDotIndex + 1);
//        return new String[]{name, extension};
//    }
//
//    /**
//     * 提取路径里面的文件名
//     *
//     * @param fileName "/path/to/bigfile.zip"
//     * @return bigfile.zip
//     */
//    public static String extractFileName(String fileName) {
//        try {
////            URL u = new URL(url);
////            String path = u.getPath();
//            int lastSlashIndex = fileName.lastIndexOf('/');
//
//            if (lastSlashIndex != -1) {
//                return fileName.substring(lastSlashIndex + 1);
//            } else {
//                return fileName;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//
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
//            String contentType = "application/octet-stream";
//            if (null == type) {
//                type = getContentType(filename);
//            }
//            if (type != null && !"".equals(type)) {
//                contentType = type;
//            }
//            String bucket = minIoProperties.getBucket();
//            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
//                    .object(filePath)
//                    .contentType(contentType)
//                    .bucket(bucket).stream(inputStream, inputStream.available(), -1)
//                    .build();
//            minioClient.putObject(putObjectArgs);
//            // 构建返回路径
//            return getAccessPathPrefix(bucket) + filePath;
//        } catch (Exception ex) {
//            log.error("minio put file error.", ex);
//            throw new RuntimeException("上传文件失败");
//        }
//    }
//
//    public String multipart(String prefix, String filename, InputStream inputStream, String type) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
//
//
//        String bucketName = minIoProperties.getBucket();
////        String objectName = extractFileName(filename);
////        String filePath = "/path/to/bigfile.zip";
////        int partSize = 5 * 1024 * 1024; // 每片5MB
////
////// 第一步：初始化分片上传 ).createMultipartUpload(
////        CreateMultipartUploadResponse response = minioClient.createMultipartUpload(
////                CreateMultipartUploadArgs.builder()
////                        .bucket(bucketName)
////                        .object(objectName)
////                        .build()
////        );
////        String uploadId = response.result().uploadId();
////
////// 第二步：分片上传
////        List<Part> parts = new ArrayList<>();
////        try (InputStream in = new FileInputStream(filePath)) {
////            byte[] buffer = new byte[partSize];
////            int bytesRead;
////            int partNumber = 1;
////            while ((bytesRead = in.read(buffer)) != -1) {
////                // 注意：最后一片可能小于 partSize，要切割
////                ByteArrayInputStream partStream = new ByteArrayInputStream(buffer, 0, bytesRead);
////                UploadPartResponse partResponse = minioClient.uploadPart(
////                        UploadPartArgs.builder()
////                                .bucket(bucketName)
////                                .object(objectName)
////                                .uploadId(uploadId)
////                                .partNumber(partNumber)
////                                .stream(partStream, bytesRead, -1)
////                                .build()
////                );
////
////                parts.add(new Part(partNumber, partResponse.result().etag()));
////                partNumber++;
////            }
////        }
////
////// 第三步：合并分片
////        minioClient.completeMultipartUpload(
////                CompleteMultipartUploadArgs.builder()
////                        .bucket(bucketName)
////                        .object(objectName)
////                        .uploadId(uploadId)
////                        .parts(parts)
////                        .build()
////        );
//
//
//        ObjectWriteResponse objectWriteResponse = minioClient.uploadObject(
//                UploadObjectArgs.builder()
//                        .bucket(bucketName)
//                        .object("bigfile.zip")
//                        .filename("/path/to/bigfile.zip")
//                        .build()
//        );
//
//
//        return "";
//    }
//
//
//    public String getContentType(String filename) {
//        String[] parts = extractFileNameAndExtension(filename);
//        String part = parts[1];
//        if (part != null) {
//            if (IMAGE_EXTENSION_NAME_LIST.contains(part)) {
//                return "image/" + part;
//            } else if (FILE_EXTENSION_NAME_LIST.contains(part)) {
//                return "application/" + part;
//            } else if (TEXT_EXTENSION_NAME_LIST.contains(part)) {
//                return "text/" + part;
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 拼接访问路径 http://ip:port/bucket/
//     */
//    private String getAccessPathPrefix(String bucket) {
//        return minIoProperties.getReadPath() + "/" + bucket + "/";
//    }
//
//    /**
//     * 删除文件
//     *
//     * @param pathUrl 文件全路径
//     */
//    public boolean delete(String pathUrl) {
//        String bucket = minIoProperties.getBucket();
//        String accessPathPrefix = getAccessPathPrefix(bucket);
//        pathUrl = pathUrl.replace(accessPathPrefix, "");
//        // 删除Objects
//        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket(bucket).object(pathUrl).build();
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
//    public InputStream downLoadFile(String pathUrl) {
//        InputStream inputStream = null;
//        try {
//            String bucket = minIoProperties.getBucket();
//            String accessPathPrefix = getAccessPathPrefix(bucket);
//            pathUrl = pathUrl.replace(accessPathPrefix, "");
//            inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(pathUrl).build());
//        } catch (Exception e) {
//            String msg = String.format("minio down file error. pathUrl: %s， msg = %s", pathUrl, e.getMessage());
//            log.error(msg);
//            e.printStackTrace();
//            if (e.getMessage() != null && !"".equals(e.getMessage()) && e.getMessage().contains("The specified key does not exist")) {
//                msg = "文件不存在";
//            }
//            throw new RuntimeException(msg);
//        }
////        byte[] bytes = IOUtils.toByteArray(inputStream);
//        return inputStream;
//    }
//
//
//}
