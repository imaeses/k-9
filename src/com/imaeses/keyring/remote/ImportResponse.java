package com.imaeses.keyring.remote;

import android.os.Parcel;
import android.os.Parcelable;

public class ImportResponse implements Parcelable {

    private long masterKeyId;
    private boolean masterCanSign;
    private boolean masterCanEncrypt;
    private int numSubkeys;
    private long[] subkeyIds;
    private boolean[] subkeyCanSign;
    private boolean[] subkeyCanEncrypt;
    private String error;
    
    public static final Parcelable.Creator<ImportResponse> CREATOR = new Parcelable.Creator<ImportResponse>() {
        
        public ImportResponse createFromParcel(Parcel in) {
            return new ImportResponse(in);
        }
        
        public ImportResponse[] newArray(int size) {
            return new ImportResponse[size];
        }
        
    };
    
    public ImportResponse() {
    }
    
    public ImportResponse(Parcel in) {
        readFromParcel(in);
    }

    public long getMasterKeyId() {
        return masterKeyId;
    }

    public void setMasterKeyId(long masterKeyId) {
        this.masterKeyId = masterKeyId;
    }

    public boolean isMasterCanSign() {
        return masterCanSign;
    }

    public void setMasterCanSign(boolean masterCanSign) {
        this.masterCanSign = masterCanSign;
    }

    public boolean isMasterCanEncrypt() {
        return masterCanEncrypt;
    }

    public void setMasterCanEncrypt(boolean masterCanEncrypt) {
        this.masterCanEncrypt = masterCanEncrypt;
    }

    public int getNumSubkeys() {
        return numSubkeys;
    }

    public void setNumSubkeys(int numSubkeys) {
        this.numSubkeys = numSubkeys;
    }

    public long[] getSubkeyIds() {
        return subkeyIds;
    }

    public void setSubkeyIds(long[] subkeyIds) {
        this.subkeyIds = subkeyIds;
    }

    public boolean[] getSubkeyCanSign() {
        return subkeyCanSign;
    }

    public void setSubkeyCanSign(boolean[] subkeyCanSign) {
        this.subkeyCanSign = subkeyCanSign;
    }

    public boolean[] getSubkeyCanEncrypt() {
        return subkeyCanEncrypt;
    }

    public void setSubkeyCanEncrypt(boolean[] subkeyCanEncrypt) {
        this.subkeyCanEncrypt = subkeyCanEncrypt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
    
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        writeToParcel(out);
    }
    
    public void writeToParcel(Parcel out) {
        out.writeLong(masterKeyId);
        out.writeByte((byte) (masterCanSign ? 1 : 0));
        out.writeByte((byte) (masterCanEncrypt ? 1 : 0));
        out.writeInt(numSubkeys);
        out.writeLongArray(subkeyIds);
        out.writeBooleanArray(subkeyCanSign);
        out.writeBooleanArray(subkeyCanEncrypt);
        out.writeString(error);
    }

    public void readFromParcel(Parcel in) {
        masterKeyId = in.readLong();
        masterCanSign = in.readByte() != 0;
        masterCanEncrypt = in.readByte() != 0;
        numSubkeys = in.readInt();
        subkeyIds = new long[numSubkeys];
        in.readLongArray(subkeyIds);
        subkeyCanSign = new boolean[numSubkeys];
        in.readBooleanArray(subkeyCanSign);
        subkeyCanEncrypt = new boolean[numSubkeys];
        in.readBooleanArray(subkeyCanEncrypt);
        error = in.readString();
    }
    
}
