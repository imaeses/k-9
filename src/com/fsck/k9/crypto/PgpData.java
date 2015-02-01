package com.fsck.k9.crypto;

import java.io.Serializable;

public class PgpData implements Serializable {
    private static final long serialVersionUID = 6314045536470848410L;
    protected long mEncryptionKeyIds[] = null;
    protected long mSignatureKeyId = 0;
    protected String mSignatureUserId = null;
    protected boolean mSignatureSuccess = false;
    protected boolean mSignatureUnknown = false;
    protected String mDecryptedData = null;
    protected String mEncryptedData = null;
    protected String filename = null;
    protected String fullDecryptedMimeMessage = null;
    protected boolean showFile = false;
    protected boolean pgpEncrypted = false;
    protected boolean pgpSigned = false;
    protected String signature = null;
    protected boolean forceArmored = false;
    protected boolean chooseKeys = false;
    
    public void setSignatureKeyId(long keyId) {
        mSignatureKeyId = keyId;
    }

    public long getSignatureKeyId() {
        return mSignatureKeyId;
    }

    public void setEncryptionKeys(long keyIds[]) {
        mEncryptionKeyIds = keyIds;
    }

    public long[] getEncryptionKeys() {
        return mEncryptionKeyIds;
    }

    public boolean hasSignatureKey() {
        return mSignatureKeyId != 0;
    }

    public boolean hasEncryptionKeys() {
        return (mEncryptionKeyIds != null) && (mEncryptionKeyIds.length > 0);
    }

    public String getEncryptedData() {
        return mEncryptedData;
    }

    public void setEncryptedData(String data) {
        mEncryptedData = data;
    }

    public String getDecryptedData() {
        return mDecryptedData;
    }

    public void setDecryptedData(String data) {
        mDecryptedData = data;
    }

    public void setSignatureUserId(String userId) {
        mSignatureUserId = userId;
    }

    public String getSignatureUserId() {
        return mSignatureUserId;
    }

    public boolean getSignatureSuccess() {
        return mSignatureSuccess;
    }

    public void setSignatureSuccess(boolean success) {
        mSignatureSuccess = success;
    }

    public boolean getSignatureUnknown() {
        return mSignatureUnknown;
    }

    public void setSignatureUnknown(boolean unknown) {
        mSignatureUnknown = unknown;
    }
    
    public String getFilename() {
    	return filename;
    }
    
    public void setFilename( String filename ) {
    	this.filename = filename;
    }
    
    public String getFullDecryptedMimeMessage() {
		return fullDecryptedMimeMessage;
	}

	public void setFullDecryptedMimeMessage(String fullDecryptedMimeMessage) {
		this.fullDecryptedMimeMessage = fullDecryptedMimeMessage;
	}

	public boolean showFile() {
    	return showFile;
    }
    
    public void setShowFile( boolean showFile ) {
    	this.showFile = showFile;
    }
    
    public boolean isPgpEncrypted() {
    	return pgpEncrypted;
    }
    
    public void setPgpEncrypted( boolean pgpEncrypted ) {
    	this.pgpEncrypted = pgpEncrypted;
    }
    
    public boolean isPgpSigned() {
    	return pgpSigned;
    }
    
    public void setPgpSigned( boolean pgpSigned ) {
    	this.pgpSigned = pgpSigned;
    }

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public boolean isForceArmored() {
		return forceArmored;
	}

	public void setForceArmored(boolean forceArmored) {
		this.forceArmored = forceArmored;
	}
	
	public boolean isChooseKeys() {
        return chooseKeys;
    }

    public void setChooseKeys(boolean chooseKeys) {
        this.chooseKeys = chooseKeys;
    }
    
}
