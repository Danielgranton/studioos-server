package com.studioos.server.shared.enums;

public enum MediaJobOperation {
    AUDIO_NORMALIZE("audio.normalize"),
    AUDIO_PREVIEW("audio.generatePreview"),
    AUDIO_WAVEFORM("audio.generateWaveform"),
    COVER_RESIZE("image.resize"),
    COVER_THUMBNAIL("image.generateThumbnail"),
    COVER_WEBP("image.convertWebp");

    private final String operationString;

    MediaJobOperation(String operationString) {
        this.operationString = operationString;
    }

    public String getOperationString() {
        return operationString;
    }
}