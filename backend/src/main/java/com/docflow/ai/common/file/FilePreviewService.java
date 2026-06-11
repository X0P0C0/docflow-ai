package com.docflow.ai.common.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 文件预览服务 —— 支持多种文件格式预览
 * <p>
 * 支持的文件类型：
 * <ul>
 *   <li>图片：jpg, png, gif, webp</li>
 *   <li>文档：pdf, doc, docx, xls, xlsx</li>
 *   <li>文本：txt, md, json, xml</li>
 *   <li>代码：java, py, js, ts</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>文件类型检测：Magic Number vs 扩展名</li>
 *   <li>在线预览方案：前端 Canvas 渲染 vs 后端转换</li>
 *   <li>安全风险：文件上传漏洞、XXE 攻击</li>
 * </ul>
 */
@Slf4j
@Component
public class FilePreviewService {

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("txt", "text/plain"),
            Map.entry("html", "text/html"),
            Map.entry("css", "text/css"),
            Map.entry("js", "application/javascript"),
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml"),
            Map.entry("md", "text/markdown"),
            Map.entry("java", "text/x-java"),
            Map.entry("py", "text/x-python")
    );

    private static final Set<String> PREVIEWABLE_TYPES = Set.of(
            "pdf", "jpg", "jpeg", "png", "gif", "webp", "txt", "md", "json", "xml",
            "html", "css", "js", "java", "py"
    );

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "war"
    );

    /**
     * 获取文件 Content-Type
     */
    public String getContentType(String filename) {
        String ext = getExtension(filename);
        return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    /**
     * 检查文件是否可预览
     */
    public boolean isPreviewable(String filename) {
        String ext = getExtension(filename);
        return PREVIEWABLE_TYPES.contains(ext);
    }

    /**
     * 检查文件是否危险
     */
    public boolean isDangerous(String filename) {
        String ext = getExtension(filename);
        return DANGEROUS_EXTENSIONS.contains(ext);
    }

    /**
     * 获取预览方式
     */
    public PreviewType getPreviewType(String filename) {
        String ext = getExtension(filename);

        if (Set.of("jpg", "jpeg", "png", "gif", "webp").contains(ext)) {
            return PreviewType.IMAGE;
        }
        if ("pdf".equals(ext)) {
            return PreviewType.PDF;
        }
        if (Set.of("txt", "md", "json", "xml", "html", "css", "js", "java", "py").contains(ext)) {
            return PreviewType.TEXT;
        }
        return PreviewType.DOWNLOAD;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public enum PreviewType {
        IMAGE, PDF, TEXT, DOWNLOAD
    }
}
