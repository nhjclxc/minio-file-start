package com.nhjclxc.minio;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 上传工具入口类
 *
 * @author nhjclxc@163.com
 */
@EnableConfigurationProperties({
        MinIoProperties.class
})
public class MinIoConfiguration {
    @Bean
    public MinIoTemplate smsTemplate(MinIoProperties properties) {
        return new MinIoTemplate(properties);
    }
}
