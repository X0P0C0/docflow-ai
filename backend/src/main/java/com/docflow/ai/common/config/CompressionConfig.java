package com.docflow.ai.common.config;

import org.springframework.boot.web.servlet.server.Encoding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * 响应压缩配置 —— 减少网络传输数据量
 * <p>
 * 压缩策略：
 * <ul>
 *   <li>启用 Gzip 压缩</li>
 *   <li>最小压缩阈值：1024 bytes</li>
 *   <li>支持的 MIME 类型：text/html, application/json, application/xml</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>Gzip vs Brotli 压缩效率对比</li>
 *   <li>压缩对 CPU 和带宽的权衡</li>
 *   <li>哪些内容不适合压缩（已压缩的图片、视频）</li>
 * </ul>
 * <p>
 * 配置方式（application.yml）：
 * <pre>
 *   server:
 *     compression:
 *       enabled: true
 *       mime-types: application/json,application/xml,text/html,text/plain
 *       min-response-size: 1024
 * </pre>
 */
@Configuration
public class CompressionConfig {

    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }
}
