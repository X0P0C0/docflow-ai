package com.docflow.ai.common.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("文件预览服务测试")
class FilePreviewServiceTest {

    private final FilePreviewService service = new FilePreviewService();

    @Test
    @DisplayName("获取图片 Content-Type")
    void shouldGetImageContentType() {
        assertThat(service.getContentType("photo.jpg")).isEqualTo("image/jpeg");
        assertThat(service.getContentType("icon.png")).isEqualTo("image/png");
    }

    @Test
    @DisplayName("获取 PDF Content-Type")
    void shouldGetPdfContentType() {
        assertThat(service.getContentType("doc.pdf")).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("图片应可预览")
    void shouldPreviewImages() {
        assertThat(service.isPreviewable("photo.jpg")).isTrue();
        assertThat(service.isPreviewable("icon.png")).isTrue();
    }

    @Test
    @DisplayName("PDF 应可预览")
    void shouldPreviewPdf() {
        assertThat(service.isPreviewable("doc.pdf")).isTrue();
    }

    @Test
    @DisplayName("exe 文件应标记为危险")
    void shouldDetectDangerousFiles() {
        assertThat(service.isDangerous("virus.exe")).isTrue();
        assertThat(service.isDangerous("script.bat")).isTrue();
        assertThat(service.isDangerous("safe.pdf")).isFalse();
    }

    @Test
    @DisplayName("图片预览类型应为 IMAGE")
    void shouldReturnImagePreviewType() {
        assertThat(service.getPreviewType("photo.jpg")).isEqualTo(FilePreviewService.PreviewType.IMAGE);
    }

    @Test
    @DisplayName("PDF 预览类型应为 PDF")
    void shouldReturnPdfPreviewType() {
        assertThat(service.getPreviewType("doc.pdf")).isEqualTo(FilePreviewService.PreviewType.PDF);
    }

    @Test
    @DisplayName("文本文件预览类型应为 TEXT")
    void shouldReturnTextPreviewType() {
        assertThat(service.getPreviewType("code.java")).isEqualTo(FilePreviewService.PreviewType.TEXT);
        assertThat(service.getPreviewType("readme.md")).isEqualTo(FilePreviewService.PreviewType.TEXT);
    }

    @Test
    @DisplayName("未知文件预览类型应为 DOWNLOAD")
    void shouldReturnDownloadForUnknown() {
        assertThat(service.getPreviewType("data.xyz")).isEqualTo(FilePreviewService.PreviewType.DOWNLOAD);
    }
}
