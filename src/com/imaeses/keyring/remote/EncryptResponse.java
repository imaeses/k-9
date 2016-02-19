package com.imaeses.keyring.remote;

import android.os.Parcel;
import android.os.Parcelable;

public class EncryptResponse implements Parcelable {

    public static final int NONE = 0;
    public static final int ENC_SUCCESS = 1;
    public static final int ENC_PASSWORD_REQUIRED = 2;
    public static final int ENC_FAILURE = 3;
    public static final int ENC_NFC_ERROR = 4;
    
    private int result;
    private String payload;
    private String error;
    private String sigAlg;
    
    public static final Parcelable.Creator<EncryptResponse> CREATOR = new Parcelable.Creator<EncryptResponse>() {
        
        public EncryptResponse createFromParcel(Parcel in) {
            return new EncryptResponse(in);
        }
        
        public EncryptResponse[] newArray(int size) {
            return new EncryptResponse[size];
        }
        
    };
    
    public EncryptResponse() {
    }
    
    public EncryptResponse(Parcel in) {
        readFromParcel(in);
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
    
    public String getSigAlg() {
        return sigAlg;
    }

    public void setSigAlg(String sigAlg) {
        this.sigAlg = sigAlg;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        writeToParcel(out);
    }
    
    public void writeToParcel(Parcel out) {
        out.writeInt(result);
        out.writeString(payload);
        out.writeString(error);
        out.writeString(sigAlg);
    }

    public void readFromParcel(Parcel in) {
        result = in.readInt();
        payload = in.readString();
        error = in.readString();
        sigAlg = in.readString();
    }
    
}
