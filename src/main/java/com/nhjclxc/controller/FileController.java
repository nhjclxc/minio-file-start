package com.nhjclxc.controller;

import com.nhjclxc.minio.MinIoTemplate;
import com.nhjclxc.utils.ImageCompressUtils;
import com.nhjclxc.utils.JsonResult;
import lombok.Cleanup;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 *
 */
@RestController
@RequestMapping("/file")
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private MinIoTemplate minIoTemplate;

    @PostMapping("/uploadFile")
    public JsonResult<String> uploadFile(MultipartFile file) {
        try {
            System.out.println(file.getSize());
            System.out.println(file.getBytes().length);
            long start = System.currentTimeMillis();
            System.out.println("开始：" + start);
            InputStream inputStream = file.getInputStream();
            String filePath = minIoTemplate.uploadFile(inputStream, file.getOriginalFilename(), "Segment2", null);
            inputStream.close();
            log.info("文件上传成功 {}", filePath);
            long end = System.currentTimeMillis();
            System.out.println("结束：" + end);
            System.out.println("耗时：" + (end - start));
            return JsonResult.success(JsonResult.Type.SUCCESS.typeName(), filePath);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }



//    @ApiOperation(value = "上传文件超过1M自带压缩")
//    @ApiResponse(code = 200, message = "success")
    @PostMapping("/uploadFileWithCompress")
    public JsonResult<String> uploadFileWithCompress(MultipartFile file) {
        try {
            @Cleanup
            InputStream is = file.getInputStream();
            InputStream inputStream = ImageCompressUtils.compressByInputStream(is, 1024 * 1024);
            String filePath = minIoTemplate.uploadFile(inputStream, file.getOriginalFilename(), null);
            inputStream.close();
            log.info("文件上传成功 {}", filePath);
            return JsonResult.success(JsonResult.Type.SUCCESS.typeName(), filePath);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/downLoadFile")
    public void downLoadFile(String filePath, HttpServletResponse response) throws IOException, InterruptedException {
        InputStream inputStream = minIoTemplate.downLoadFile(filePath);
        MinIoTemplate.setResponse(response, MinIoTemplate.extractFileName(filePath));
//        write(response, inputStream);

        byte[] bytes = IOUtils.toByteArray(inputStream);
        write(response, bytes);
        log.info("文件下载成功 {}", filePath);
    }

    private void write(HttpServletResponse response, byte[] bytes) throws IOException {
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
    }

    private void write(HttpServletResponse response, InputStream inputStream) throws IOException {
        ServletOutputStream outputStream = response.getOutputStream();
        IOUtils.copy(inputStream, outputStream);
    }

    @DeleteMapping("/deleteFile")
    public JsonResult<Object> deleteFile(String filePath) {
        boolean delete = minIoTemplate.delete(filePath);
        if (delete) {
            log.info("文件删除成功 {}", filePath);
            return JsonResult.success(delete);
        }
        return JsonResult.error();
    }


}
