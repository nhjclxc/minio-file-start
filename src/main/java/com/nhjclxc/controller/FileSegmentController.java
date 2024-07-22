package com.nhjclxc.controller;

import com.nhjclxc.minio.MinIoTemplate;
import com.nhjclxc.utils.CustomThreadPoolExecutor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 文件分片上传
 */
@RestController()
@RequestMapping("/fileSegment")
public class FileSegmentController {

    /**
     * 分片标记
     */
    private static final String SEGMENT_FILE = "@segmentFile@";

    @Autowired
    private MinIoTemplate minIoTemplate;

    @PostMapping(value = "/upload2")
    public Object upload2(@Validated @RequestParam("file") MultipartFile file) throws IOException, InterruptedException {
        String filePath = minIoTemplate.uploadFileV2(file.getBytes(), file.getOriginalFilename(), "Segment", null);
        return filePath;
    }


    @GetMapping(value = "/download2")
    public void download2(@RequestParam String filePath, HttpServletResponse response) throws IOException {
        try (InputStream inputStream = minIoTemplate.downLoadFile(filePath)) {
            MinIoTemplate.setResponse(response, MinIoTemplate.extractFileName(filePath));
            ServletOutputStream outputStream = response.getOutputStream();
            IOUtils.copy(inputStream, outputStream);
            outputStream.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/upload")
    public Object upload(@Validated @RequestParam("file") MultipartFile file) throws IOException, InterruptedException {
        byte[] bytes = file.getBytes();
//        System.out.println(multiFile.getSize());
//        System.out.println(bytes.length);
        // 1MB=1024KB=1024×1024B=1048576B(字节)
        if (bytes.length > 1048576) {
            // 分片个数
            int segmentSize = 10;
            // 每一片的长度
            int everySegmentLength = bytes.length / segmentSize;
            // 每一个分片的字节数组数据
            List<byte[]> byteArrayList = new ArrayList<>();
            int globalIndex = 0;
            for (int i = 0; i < segmentSize; i++) {
                // 获取每一个分片的数据
                byte[] temp = new byte[everySegmentLength];
                int tempIndex = 0;
                for (int index = i * everySegmentLength; index < everySegmentLength * (i + 1) && tempIndex <= everySegmentLength; index++, tempIndex++) {
                    temp[tempIndex] = bytes[index];
                    globalIndex++;
                }

                // 最后一个分片的时候，把bytes.length / segmentLength;没能整除进去的数据加到最后一个分片里面
                if (i == segmentSize - 1) {
                    byte[] temp2 = new byte[everySegmentLength + (bytes.length - globalIndex)];
                    System.arraycopy(temp, 0, temp2, 0, temp.length);
                    temp = temp2;
                    while (globalIndex < bytes.length) {
                        temp[tempIndex++] = bytes[globalIndex++];
                    }
                }
                byteArrayList.add(temp);
            }

            // 并发上传
            CountDownLatch lock = new CountDownLatch(segmentSize);
            // 获取文件名
            final String fileName = MinIoTemplate.builderFilePath("Segment", file.getOriginalFilename());
            long start = System.currentTimeMillis();
            System.out.println("开始：" + start);
            for (int i = 0; i < byteArrayList.size(); i++) {
                final int finalI = i;

                CustomThreadPoolExecutor.pool.execute(() -> {
                    // byte转File  将byte写到一个临时文件里面
                    byte[] temp = byteArrayList.get(finalI);

                    // 上传
                    try {
                        InputStream inputStream = new ByteArrayInputStream(temp);
                        // 上传到 minio
                        String filePath = minIoTemplate.uploadFile(inputStream, fileName + SEGMENT_FILE + finalI, null);
                        inputStream.close();
                        System.out.println(lock.getCount() + " - " + filePath + "  上传线程：" + Thread.currentThread().getName());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    lock.countDown();
                });
            }
            // 等待完成
            lock.await();
            long end = System.currentTimeMillis();
            System.out.println("结束：" + end);
            System.out.println("耗时：" + (end - start));
        } else {
            // 没有超出阈值的文件，不使用分片上传
            // 普通上传...
//            单文件上传
        }


        return "分片上传完成";
    }

    public void download(@RequestParam(required = true) String fileUuid, HttpServletResponse response) throws IOException, InterruptedException {
//        fileUuid = "8f42c74a-88b9-46d1-8bee.txt@segmentFile@5";

        String[] split = fileUuid.split(SEGMENT_FILE);
        String filename = split[0];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (split.length == 2) {
            // 分片上传的文件，进行分片下载合并
            int maxIndex = Integer.parseInt(split[1]);

            CountDownLatch lock = new CountDownLatch(maxIndex);
            File[] tempFileArray = new File[maxIndex + 1];
            for (int i = 0; i <= maxIndex; i++) {
                final int finalI = i;
                CustomThreadPoolExecutor.pool.execute(() -> {
                    String tempFilename = filename + SEGMENT_FILE + finalI;
                    // 调用oss下载
                    File tempFile = new File("E:\\lxc\\file-test\\" + tempFilename);

                    // 保证下载的时候拼接的顺序是上传时的顺序
                    tempFileArray[finalI] = tempFile;

                    System.out.println(Thread.currentThread().getName() + "下载：" + tempFile.getName() + finalI);
                    lock.countDown();
                });
            }
            // 等待完成
            lock.await();

            // 数据拼接
            for (File file : tempFileArray) {
                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    IOUtils.copy(inputStream, byteArrayOutputStream);
                    byteArrayOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // 没有分片的文件
        }

        // 后续操作

        // 以流的形式下载文件
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        byte[] buffer = new byte[byteArrayInputStream.available()];
        byteArrayInputStream.read(buffer);
        byteArrayInputStream.close();
        byteArrayOutputStream.close();

        download(response, filename, buffer);

    }

    private void download(HttpServletResponse response, String filename, byte[] buffer) throws IOException {
        response.reset(); // 清空response
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
        response.addHeader("Content-Length", "" + buffer.length);
        response.setContentType("application/octet-stream");
        OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
        toClient.write(buffer);
        toClient.flush();
        toClient.close();
    }
}
