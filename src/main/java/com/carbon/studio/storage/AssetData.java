package com.carbon.studio.storage;

import java.util.Arrays;

/** Embedded source bytes needed by image layers and future replay reconstruction. */
public final class AssetData {
    private final String id;
    private final String mime;
    private final byte[] bytes;
    public AssetData(String id,String mime,byte[] bytes){
        if(id==null||id.isBlank())throw new IllegalArgumentException("Asset id is required.");
        this.id=id;this.mime=mime==null||mime.isBlank()?"application/octet-stream":mime;
        this.bytes=bytes==null?new byte[0]:bytes.clone();
    }
    public String id(){return id;}
    public String mime(){return mime;}
    public byte[] bytes(){return bytes.clone();}
    byte[] unsafeBytes(){return bytes;}
    @Override public boolean equals(Object o){return o instanceof AssetData a&&id.equals(a.id)&&mime.equals(a.mime)&&Arrays.equals(bytes,a.bytes);}
    @Override public int hashCode(){return 31*(31*id.hashCode()+mime.hashCode())+Arrays.hashCode(bytes);}
}
