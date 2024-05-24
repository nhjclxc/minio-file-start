//
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiResponse;
//import org.apache.commons.io.IOUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import javax.servlet.ServletOutputStream;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//
///**
// * 系统文件相关接口
// */
//@RestController
//@RequestMapping("/file")
//@Api(tags = "FileController", description = "系统文件相关接口")
//public class FileController {
//    private static final Logger log = LoggerFactory.getLogger(FileController.class);
//
//    @Autowired
//    private MinIoTemplate minIoTemplate;
//
//    @ApiOperation(value = "上传文件")
//    @ApiResponse(code = 200, message = "success")
//    @PostMapping("/uploadFile")
//    public JsonResult<String> uploadFile(MultipartFile file) {
//        try {
//            InputStream inputStream = file.getInputStream();
//            String filePath = minIoTemplate.uploadFile("", file.getOriginalFilename(), inputStream, null);
//            inputStream.close();
//            log.info("文件上传成功 {}", filePath);
//            return JsonResult.success(JsonResult.Type.SUCCESS.typeName(), filePath);
//        } catch (Exception e) {
//            return JsonResult.error(e.getMessage());
//        }
//    }
//
//    @ApiOperation(value = "下载文件")
//    @ApiResponse(code = 200, message = "success")
//    @GetMapping("/downLoadFile")
//    public void downLoadFile(String filePath, HttpServletResponse response) throws IOException {
//        InputStream inputStream = minIoTemplate.downLoadFile(filePath);
//        MinIoTemplate.setResponse(response, MinIoTemplate.extractFileName(filePath));
////        write(response, inputStream);
//
//        byte[] bytes = IOUtils.toByteArray(inputStream);
//        write(response, bytes);
//        log.info("文件下载成功 {}", filePath);
//    }
//
//    private void write(HttpServletResponse response, byte[] bytes) throws IOException {
//        ServletOutputStream outputStream = response.getOutputStream();
//        outputStream.write(bytes);
//    }
//    private void write(HttpServletResponse response, InputStream inputStream) throws IOException {
//        ServletOutputStream outputStream = response.getOutputStream();
//        IOUtils.copy(inputStream, outputStream);
//    }
//
//    @ApiOperation(value = "删除文件")
//    @ApiResponse(code = 200, message = "success")
//    @DeleteMapping("/deleteFile")
//    public JsonResult<Object> deleteFile(String filePath) {
//        boolean delete = minIoTemplate.delete(filePath);
//        if (delete){
//            log.info("文件删除成功 {}", filePath);
//            return JsonResult.success(delete);
//        }
//        return JsonResult.error();
//    }
//
//
///**
// * 设置响应流
// */
//public static void setResponse(HttpServletResponse response, String fileName) throws IOException {
//        response.reset();
//        response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
//        response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "*");
//        response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
//        response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
//        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()));
//        response.setContentType("application/octet-stream; charset=UTF-8");
//        }
//
//}
