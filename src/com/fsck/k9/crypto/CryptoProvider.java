package com.fsck.k9.crypto;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.fsck.k9.mail.Message;

/**
 * A CryptoProvider provides functionalities such as encryption, decryption, digital signatures.
 * It currently also stores the results of such encryption or decryption.
 * TODO: separate the storage from the provider
 *
 * Modified by Adam Wasserman to include PGPKeyRing provider. (9 May 2013)
 */
abstract public class CryptoProvider {
    static final long serialVersionUID = 0x21071234;

    //public static final String SIG_ALG = "sha256";
    
    public static Pattern PGP_MESSAGE = 
            Pattern.compile( ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*", Pattern.DOTALL );
    
    public static Pattern PGP_SIGNED_MESSAGE =
            Pattern.compile( ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*", Pattern.DOTALL );
    
    public static Pattern PGP_PUBLIC_KEY_BLOCK =
            Pattern.compile( ".*?(-----BEGIN PGP PUBLIC KEY BLOCK-----.*?-----END PGP PUBLIC KEY BLOCK-----).*", Pattern.DOTALL );
    
    
    abstract public boolean isAvailable(Context context);
    abstract public boolean isEncrypted(Message message);
    abstract public boolean isSigned(Message message);
    abstract public boolean onActivityResult(CryptoEncryptCallback callback, int requestCode, int resultCode,
            Intent data, PgpData pgpData);
    abstract public boolean onDecryptActivityResult(CryptoDecryptCallback callback,
            int requestCode, int resultCode, Intent data, PgpData pgpData);
    abstract public boolean selectSecretKey(Activity activity, PgpData pgpData);
    abstract public boolean selectEncryptionKeys(Activity activity, String emails, PgpData pgpData);
    abstract public boolean encrypt(Activity activity, String data, PgpData pgpData);
    abstract public boolean encryptFile( Activity activity, String filename, PgpData pgpData );
    abstract public boolean sign( Activity activity, String filename, PgpData pgpData );
    abstract public boolean decrypt( Fragment fragment, String data, String originalCharset, PgpData pgpData);
    abstract public boolean decryptFile( Fragment fragmen, String filename, PgpData pgpData );
    abstract public boolean verify( Fragment fragmen, String filename, String sig, PgpData pgpData );
    abstract public long[] getSecretKeyIdsFromEmail(Context context, String email);
    abstract public long[] getPublicKeyIdsFromEmail(Context context, String email);
    abstract public boolean hasSecretKeyForEmail(Context context, String email);
    abstract public boolean hasPublicKeyForEmail(Context context, String email);
    abstract public String getUserId(Context context, long keyId);
    abstract public String getName();
    abstract public boolean test(Context context);
    abstract public boolean isTrialVersion();
    
    public boolean supportsAttachments( Context context ) {
    	return false;
    }
    
    public boolean supportsPgpMimeReceive( Context context ) {
    	return false;
    }
    
    public boolean supportsPgpMimeSend( Context context ) {
    	return false;
    }

    public static CryptoProvider createInstance(String name) {
        if (Apg.NAME.equals(name)) {
            return Apg.createInstance();
        } else if( PGPKeyRing.NAME.equals(name)) {
            return PGPKeyRing.createInstance();
        }

        return None.createInstance();
    }

    public interface CryptoDecryptCallback {
        void onDecryptDone(PgpData pgpData);
        void onDecryptFileDone(PgpData pgpData);
        void requestCryptoPassword(CryptoRetry retry);
        void showProgressBar(boolean display);
    }
    
    public interface CryptoEncryptCallback {
        void onEncryptDone();
        void updateEncryptLayout();
        void onEncryptionKeySelectionDone();
        void requestCryptoPassword(CryptoRetry retry);
        void showProgressBar(boolean display);
    }
    
    public static class CryptoRetry {
        
        private Method retryMethod;
        private Object[] retryParams;
        
        public CryptoRetry( Method retryMethod, Object[] retryParams ) {
            
            this.retryMethod = retryMethod;
            this.retryParams = retryParams;
            
        }

        public Method getRetryMethod() {
            return retryMethod;
        }

        public void setRetryMethod( Method retryMethod ) {
            this.retryMethod = retryMethod;
        }

        public Object[] getRetryParams() {
            return retryParams;
        }

        public void setRetryParams( Object[] retryParams ) {
            this.retryParams = retryParams;
        }
        
    }
    
   
}
