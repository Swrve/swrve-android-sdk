package com.swrve.sdk;

public class SwrveAssetsQueueItem {

    private String name;
    private String digest;
    private boolean isImage;
    private boolean isExternalSource;

    public SwrveAssetsQueueItem(String name, String digest, boolean isImage, boolean isExternalSource) {
        this.name = name;
        this.digest = digest;
        this.isImage = isImage;
        this.isExternalSource = isExternalSource;
    }

    public String getName() {
        return name;
    }

    public String getDigest() {
        return digest;
    }

    public boolean isImage() {
        return isImage;
    }

    public boolean isExternalSource() {
        return isExternalSource;
    }

    // SwrveAssetsQueueItem used in a Set, so equals() and hashCode() are important

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SwrveAssetsQueueItem that = (SwrveAssetsQueueItem) o;

        if (isImage != that.isImage) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return digest != null ? digest.equals(that.digest) : that.digest == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (digest != null ? digest.hashCode() : 0);
        result = 31 * result + (isImage ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SwrveAssetsQueueItem{" +
                "name='" + name + '\'' +
                ", digest='" + digest + '\'' +
                ", isImage=" + isImage +
                '}';
    }
}