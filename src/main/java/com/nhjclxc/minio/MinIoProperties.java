package com.nhjclxc.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO属性配置类
 *
 * @author nhjclxc@163.com
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinIoProperties {
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String endpoint;
    private String readPath;
}
