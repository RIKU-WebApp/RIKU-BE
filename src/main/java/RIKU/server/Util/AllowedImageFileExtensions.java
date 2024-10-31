package RIKU.server.Util;

import lombok.Getter;

@Getter
public enum AllowedImageFileExtensions {
    /**
     * 허용가능한 이미지 파일 확장자 list
     */
    JPEG("jpeg"),
    JPG("jpg"),
    PNG("png"),
    GIF("gif"),
    WebP("webp"),
    SVG("svg"),
    BMP("bmp"),
    TIF("tif"),
    TIFF("tiff"),
    HEIC("heic");

    private String extension;

    AllowedImageFileExtensions(String extension) {
        this.extension = extension;
    }

    public static boolean contains(String extension) {
        extension = extension.toLowerCase();        // 입력된 확장자를 소문자로 변환

        for (AllowedImageFileExtensions allowedImageFileExtensions : AllowedImageFileExtensions.values()) {
            if (allowedImageFileExtensions.getExtension().equals(extension)) {
                return true;
            }
        }
        return false;
    }
}
