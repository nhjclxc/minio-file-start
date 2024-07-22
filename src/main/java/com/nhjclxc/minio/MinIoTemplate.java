package com.nhjclxc.minio;


import com.nhjclxc.utils.CustomThreadPoolExecutor;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * MinIO模板类
 *
 * @author LuoXianchao
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
     * @param prefix  目录前缀
     * @param filename 文件名{yyyy/mm/dd/file.jpg}
     * @return 文件路径
     */
    public static String builderFilePath(String prefix, String filename) {
        StringBuilder stringBuilder = new StringBuilder(50);
        if (prefix != null && !"".equals(prefix)) {
            stringBuilder.append(prefix).append(separator);
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
        name = name.replace(" ", ""); //去除原有的空格
        String extension = fileName.substring(lastDotIndex + 1);
        return new String[]{name, extension};
    }

    /**
     * 提取路径里面的文件名
     */
    public static String extractFileName(String filename) {
        try {
            int lastSlashIndex = filename.lastIndexOf('/');
            return lastSlashIndex != -1 ? filename.substring(lastSlashIndex + 1) : filename;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取扩展名
     */
    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex != -1) {
            return fileName.substring(dotIndex);
        }
        return "";
    }

    /**
     * 拼接访问路径 http://ip:port/bucket/
     */
    private String getAccessPathPrefix(String bucket){
        return minIoProperties.getEndpoint() + "/" + bucket + "/";
    }


    /**
     * 上传图片文件
     *
     * @param prefix      文件前缀
     * @param filename    文件名
     * @param inputStream 文件流
     * @return 文件全路径
     */
    public String uploadFile(InputStream inputStream, String filename, String prefix, String contentType) {
        String filePath = builderFilePath(prefix, filename);
        return doUploadFile(inputStream, filePath, contentType);
    }


    public String uploadFile(InputStream inputStream, String filename, String contentType) {
        return uploadFile(inputStream, filename, null, contentType);
    }

    private String doUploadFile(InputStream inputStream, String filePath, String contentType) {
        try {
            if (contentType == null || !"".equals(contentType)){
                contentType = "application/octet-stream";
            }
            String bucket = minIoProperties.getBucket();
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object(filePath)
                    .contentType(contentType)
                    .bucket(bucket).stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(putObjectArgs);
            // 构建返回路径
            return getAccessPathPrefix(bucket) + filePath;
        } catch (Exception ex) {
            log.error("minio put file error.", ex);
            throw new RuntimeException("上传文件失败");
        }
    }

    /**
     * 分片标记
     */
    private static final String CHUNKS_FILE = "@ChunksFile@";


    public String uploadFileV2(byte[] fileBytes, String originalFilename, String prefix, String contentType) throws InterruptedException {
        String filePath;
        int fileLength = fileBytes.length;
        // 1MB=1024KB=1024×1024B=1048576B(字节)
        int chunkSize = 1048576;
        if (fileLength > chunkSize){
            final String fileName = MinIoTemplate.builderFilePath(prefix, originalFilename);

            int numChunks = (int) Math.ceil((double) fileLength / chunkSize);
            System.out.println("numChunks = " + numChunks);

            long startTime = System.currentTimeMillis();
            System.out.println("开始：" + startTime);
            // 并发上传
            CountDownLatch lock = new CountDownLatch(numChunks);
            for (int i = 0; i < numChunks; i++) {
                int finalI = i;
                CustomThreadPoolExecutor.pool.execute(() -> {
                    int start = finalI * chunkSize;
                    int end = Math.min(start + chunkSize, fileLength);
                    byte[] chunk = new byte[end - start];
                    System.arraycopy(fileBytes, start, chunk, 0, end - start);
                    InputStream inputStream = new ByteArrayInputStream(chunk);
                    String filePath2 = doUploadFile(inputStream, fileName + CHUNKS_FILE + finalI, contentType);

                    lock.countDown();
                    System.out.println(finalI + "-Saved " + filePath2);
                });
            }
            // 等待完成
            lock.await();
            long endTime = System.currentTimeMillis();
            System.out.println("结束：" + endTime);
            System.out.println("耗时：" + (endTime - startTime));
            System.out.println("耗时：" + (endTime - startTime) / 1000);

            // 构建返回路径
            filePath = getAccessPathPrefix(minIoProperties.getBucket()) + fileName + CHUNKS_FILE + numChunks;
        }else {
            filePath = uploadFile(new ByteArrayInputStream(fileBytes), originalFilename, "Segment", null);
        }
        return filePath;
    }


    /**
     * 下载文件
     *
     * @param pathUrl 文件全路径
     * @return 文件流
     */
    public InputStream downLoadFile(String pathUrl) throws IOException, InterruptedException {
        InputStream inputStream = null;

        // 找到 "@segmentFile@" 的位置
        int index = pathUrl.indexOf(CHUNKS_FILE);
        // 如果找到了，从该位置开始截取子字符串
        if (index != -1){
            // 获取分片个数
            int chunkSize = Integer.parseInt(pathUrl.substring(index + CHUNKS_FILE.length()));

            // 获取路径前缀，不含CHUNKS_FILE和索引
            pathUrl = pathUrl.substring(0, index);
            List<InputStream> inputStreamList = new ArrayList<>(chunkSize);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            long startTime = System.currentTimeMillis();
            System.out.println("开始：" + startTime);
            CountDownLatch lock = new CountDownLatch(chunkSize);
            byte[][] byteArrayList = new byte[chunkSize][];
            final String finalPathUrl = pathUrl;
            for (int i = 0; i < chunkSize; i++) {
                int finalI = i;
                CustomThreadPoolExecutor.pool.execute(() -> {
                    String tempPathUrl = finalPathUrl + CHUNKS_FILE + finalI;
                    InputStream tempInputStream = doDownLoadFile(tempPathUrl);
//                    inputStreamList.add(tempInputStream);
                    try {
                        byteArrayList[finalI] = IOUtils.toByteArray(tempInputStream);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    System.out.println(tempPathUrl + "- doDownLoadFile " + finalI);

                    lock.countDown();
                });
            }
            // 等待完成
            lock.await();
            for (byte[] bytes : byteArrayList) {
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            long endTime = System.currentTimeMillis();
            System.out.println("结束：" + endTime);
            System.out.println("耗时：" + (endTime - startTime));
            System.out.println("耗时：" + (endTime - startTime) / 1000);
            byte[] bytes = outputStream.toByteArray();
            inputStream = new ByteArrayInputStream(bytes);
            // 使用SequenceInputStream合并多个InputStream
//            inputStream = new SequenceInputStream(Collections.enumeration(inputStreamList));
//            https://www.jianshu.com/p/9f41c370eb0e
        }else {
            inputStream = doDownLoadFile(pathUrl);
        }
        return inputStream;
    }
    public InputStream doDownLoadFile(String pathUrl) {
        InputStream inputStream = null;
        try {
            String bucket = minIoProperties.getBucket();
            String accessPathPrefix = getAccessPathPrefix(bucket);
            pathUrl = pathUrl.replace(accessPathPrefix, "");
            inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(pathUrl).build());
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

    /**
     * 删除文件
     *
     * @param pathUrl 文件全路径
     */
    public boolean delete(String pathUrl) {
        String bucket = minIoProperties.getBucket();
        String accessPathPrefix = getAccessPathPrefix(bucket);
        pathUrl = pathUrl.replace(accessPathPrefix, "");
        // 删除Objects
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket(bucket).object(pathUrl).build();
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
     * 设置响应流
     */
    public static void setResponse(HttpServletResponse response, String fileName) throws IOException {
        response.reset();
        response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "*");
        response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()));
        response.setContentType("application/octet-stream; charset=UTF-8");
    }
}
