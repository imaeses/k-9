package com.imaeses.keyring.remote;

import android.os.Parcel;
import android.os.Parcelable;

public final class DecryptResponse implements Parcelable {

    private String userId;
    private long keyId;
    private String destFilename;
    private int decryptionResult;
    private int verificationResult;
    private String error;

    public static final int NONE = 0;
    public static final int DEC_SUCCESS = 1;
    public static final int DEC_PASSWORD_REQUIRED = 2;
    public static final int DEC_FAILURE = 3;
    public static final int VER_SUCCESS = 1;
    public static final int VER_SIGNER_UNKNOWN = 2;
    public static final int VER_FAILURE = 3;
    
    public static final Parcelable.Creator<DecryptResponse> CREATOR = new Parcelable.Creator<DecryptResponse>() {
    
        public DecryptResponse createFromParcel(Parcel in) {
            return new DecryptResponse(in);
        }
        
        public DecryptResponse[] newArray(int size) {
            return new DecryptResponse[size];
        }
        
    };
    
    public DecryptResponse() {    
    }
    
    private DecryptResponse(Parcel in) {
        readFromParcel(in);
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public long getKeyId() {
        return keyId;
    }
    
    public void setKeyId(long keyId) {
        this.keyId = keyId;
    }
    
    public String getDestFilename() {
        return destFilename;
    }

    public void setDestFilename(String destFilename) {
        this.destFilename = destFilename;
    }

    public int getDecryptionResult() {
        return decryptionResult;
    }

    public void setDecryptionResult(int decryptionResult) {
        this.decryptionResult = decryptionResult;
    }

    public int getVerificationResult() {
        return verificationResult;
    }

    public void setVerificationResult(int verificationResult) {
        this.verificationResult = verificationResult;
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
        out.writeString(userId);
        out.writeLong(keyId);
        out.writeString(destFilename);
        out.writeInt(decryptionResult);
        out.writeInt(verificationResult);
        out.writeString(error);
    }
    
    public void readFromParcel(Parcel in) {
        userId = in.readString();
        keyId = in.readLong();
        destFilename = in.readString();
        decryptionResult = in.readInt();
        verificationResult = in.readInt();
        error = in.readString();
    }

}
