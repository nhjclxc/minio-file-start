//package com.nhjclxc.miniofilestart;
//
//import io.minio.MinioClient;
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//
///**
// * MinIO属性配置类
// *
// * @author nhjclxc@163.com
// */
//@Data
//@ConfigurationProperties(prefix = "minio")
//public class MinIoProperties {
//    private String accessKey;
//    private String secretKey;
//    private String bucket;
//    private String endpoint;
//    private String readPath;
//
//    @Bean
//    public MinioClient buildMinIoClient(){
//        return MinioClient
//                .builder()
//                .credentials(accessKey, secretKey)
//                .endpoint(endpoint)
//                .build();
//    }
//}
