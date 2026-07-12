package com.studioos.server.shared.enums;

public enum AdMediaJobOperation {
    IMAGE_RESIZE("image.resize"),
    IMAGE_THUMBNAIL("image.generateThumbnail"),
    IMAGE_WEBP("image.convertWebp"),
    VIDEO_COMPRESS("video.compress"),
    VIDEO_THUMBNAIL("video.generateThumbnail"),
    AUDIO_NORMALIZE("audio.normalize"),
    AUDIO_COMPRESS("audio.compress");

    private final String operationString;

    AdMediaJobOperation(String operationString) {
        this.operationString = operationString;
    }

    public String getOperationString() {
        return operationString;
    }
}