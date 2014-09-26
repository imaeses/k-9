package com.fsck.k9.crypto;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.widget.Toast;
import android.util.Log;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.imaeses.squeaky.K9;
import com.imaeses.squeaky.R;

/**
 * PGP KeyRing integration. (9 May 2013).
 * Also modified: crypto/CryptoProvider.java, activity/setup/AccountSettings.java, res/values/strings.xml, and res/values/arrays.xml
 *
 * @author Adam Wasserman
 *
 */
public class PGPKeyRing extends CryptoProvider {

    public static final String NAME = "pgpkeyring";
    
    public static final String PACKAGE_PAID = "com.imaeses.keyring";
    public static final String PACKAGE_TRIAL = "com.imaeses.keyring.trial";
    public static final String APP_NAME_PAID = "KeyRing";
    public static final int VERSION_REQUIRED_MIN = 22;
    public static final int VERSION_REQUIRED_ATTACHMENTS_MIN = 29;
    public static final int VERSION_REQUIRED_FLOATING_SIGS_MIN = 30;
    public static final int VERSION_REQUIRED_PGP_MIME_SEND = 38;

    public static final String AUTHORITY_PAID = "com.imaeses.KeyRing";
    public static final String AUTHORITY_TRIAL = "com.imaeses.trial.KeyRing";
    
    public static final String PROVIDER_KEYID = "keyid";
    public static final String PROVIDER_USERID = "userid";
    public static final String PROVIDER_IDENTITY = "identity";
    
    public static final int DECRYPT_MESSAGE = 100;
    public static final int ENCRYPT_MESSAGE = 101;
    public static final int SELECT_PUBLIC_KEYS = 102;
    public static final int SELECT_SECRET_KEY = 103;
    public static final int DECRYPT_FILE = 104;
    public static final int VERIFY = 105;
    public static final int ENCRYPT_FILE = 106;
    public static final int SIGN = 107;

    private Uri uriSelectPrivateSigningKey;
    private Uri uriSelectPublicEncKey;
    private Uri uriSelectPublicKeysByEmail;
    private Uri uriSelectPrivateKeysByEmail;
    private Uri uriSelectPrimaryUserIdByKeyid;
    private boolean isTrialVersion;
    private final Handler handler = new Handler();

    public static final String EXTRAS_MSG = "msg";
    public static final String EXTRAS_FILENAME = "file.name";
    public static final String EXTRAS_DEST_FILENAME = "file.dest.name";
    public static final String EXTRAS_SHOW_FILE = "file.show";
    public static final String EXTRAS_ENCRYPTION_KEYIDS = "keys.enc";
    public static final String EXTRAS_SIGNATURE = "sig";
    public static final String EXTRAS_SIGNATURE_KEYID = "sig.key";
    public static final String EXTRAS_SIGNATURE_SUCCESS = "sig.success";
    public static final String EXTRAS_SIGNATURE_IDENTITY = "sig.identity";
    public static final String EXTRAS_EMAIL_ADDRESSES = "email.addresses";
    public static final String EXTRAS_PRESELECTED = "keys.preselected";
    public static final String EXTRAS_SELECTION_MULTI = "selection.mode.multi";
    public static final String EXTRAS_CHOSEN_KEYIDS = "chosen.keyids";
    public static final String EXTRAS_CHOSEN_KEY = "chosen.key";
    public static final String EXTRAS_SIGNATURE_UNKNOWN = "sig.unknown";
    public static final String EXTRAS_SHOW_KEYID_IN_SINGLE_SELECTION = "show.keyid.single.selection";
    public static final String EXTRAS_CHARSET = "charset";
    public static final String EXTRAS_SIGNATURE_ALG = "sig.algorithm";
    
    public static class PGPKeyRingIntent {
        
        public static final String ENCRYPT_MSG_AND_RETURN = "com.imaeses.keyring.ENCRYPT_MSG_AND_RETURN";
        public static final String ENCRYPT_FILE_AND_RETURN = "com.imaeses.keyring.ENCRYPT_FILE_AND_RETURN";
        public static final String DECRYPT_MSG_AND_RETURN = "com.imaeses.keyring.DECRYPT_MSG_AND_RETURN";
        public static final String DECRYPT_FILE_AND_RETURN = "com.imaeses.keyring.DECRYPT_FILE_AND_RETURN";
        public static final String VERIFY_AND_RETURN = "com.imaeses.keyring.VERIFY_AND_RETURN";
        public static final String SIGN_AND_RETURN = "com.imaeses.keyring.SIGN_AND_RETURN";
        
    }
    
    public static Pattern PGP_MESSAGE = 
            Pattern.compile( ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*", Pattern.DOTALL );
    
    public static Pattern PGP_SIGNED_MESSAGE =
            Pattern.compile( ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*", Pattern.DOTALL );
    
    public static PGPKeyRing createInstance() {
        return new PGPKeyRing();
    }
    
    public PGPKeyRing() {
        
        isTrialVersion = true;      
        setContentUris();
        
    }
    
    /**
     * Check whether PGPKeyRing is installed and at a high enough version.
     *
     * @param context
     * @return whether a suitable version of PGPKeyRing was found
     */
    @Override
    public boolean isAvailable( Context context ) {
              
        boolean isSuitable = false;
                  
        PackageInfo pi = null;
        PackageManager packageManager = context.getPackageManager();
        
        try {
           
            pi = packageManager.getPackageInfo( PACKAGE_PAID, 0 ); 
            
            isTrialVersion = false;
            setContentUris();
                
        } catch( NameNotFoundException e ) {
                
            try {         
                pi = packageManager.getPackageInfo( PACKAGE_TRIAL, 0 );
            } catch( NameNotFoundException f ) {
            }
                
        }
            
        if( pi != null && pi.versionCode >= VERSION_REQUIRED_MIN ) {
            isSuitable = true;
        } else {
        	post( R.string.error_pgpkeyring_version_not_supported, context );
        }

        return isSuitable;
        
    }
    
    /**
     * Select the key for generating a signature.
     *
     * @param activity
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean selectSecretKey( Activity activity, PgpData pgpData ) {
        
        boolean success = false;
        
        try {
         
            Intent i = new Intent( Intent.ACTION_PICK );
            i.putExtra( EXTRAS_SHOW_KEYID_IN_SINGLE_SELECTION, true );
            i.addCategory( Intent.CATEGORY_DEFAULT );
            i.setData( uriSelectPrivateSigningKey );
            
            activity.startActivityForResult( i, SELECT_SECRET_KEY );
            success = true;
            
        } catch( ActivityNotFoundException e ) {
        	post( R.string.error_activity_not_found, activity );
        }
        
        return success;
        
    }
    
    /**
     * Select keys for use in encrypting.
     *
     * @param activity
     * @param emails The emails that should be used for preselection.
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean selectEncryptionKeys( Activity activity, String emails, PgpData pgpData ) {
        
        boolean success = false;
        
        try {
         
            Intent i = new Intent( Intent.ACTION_PICK );
            i.addCategory( Intent.CATEGORY_DEFAULT );
            i.setData( uriSelectPublicEncKey );
            i.putExtra( EXTRAS_SELECTION_MULTI, true );
            
            long[] preselected = null;
                    
            if( !pgpData.hasEncryptionKeys() ) {
               
            	/*
                if( pgpData.hasSignatureKey() ) {        
                    preselected = new long[] { pgpData.getSignatureKeyId() };
                }
                */
            	
                if( emails != null && emails.length() > 0 ) {
                    i.putExtra( EXTRAS_EMAIL_ADDRESSES, emails.split( "," ) );
                }
                
            } else {
                preselected = pgpData.getEncryptionKeys();
            }
            
            if( preselected != null ) {
                i.putExtra( EXTRAS_PRESELECTED, preselected );
            }
            
            activity.startActivityForResult( i, SELECT_PUBLIC_KEYS );
            success = true;
            
        } catch( ActivityNotFoundException e ) {
        	post( R.string.error_activity_not_found, activity );
        }
        
        return success;
        
    }
    
    /**
     * Get key ids in secret key rings based on a given email. For signing keys.
     *
     * @param context
     * @param email The email in question.
     * @return key ids
     */
    @Override
    public long[] getSecretKeyIdsFromEmail( Context context, String email ) {
    
        String[] projection = new String[] { PROVIDER_KEYID };
        
        long[] keyids = null;
        Uri.Builder builder = uriSelectPrivateKeysByEmail.buildUpon();
        builder.appendPath( email );
        Uri uri = builder.build();
               
        Cursor c = null;
        try {
                
            c = context.getContentResolver().query( uri, projection, null, null, null );
            if( c != null ) {
                
                keyids = new long[ c.getCount() ];
            
                while( c.moveToNext() ) {
                    
                    String keyId = c.getString( 0 );
                    keyids[ c.getPosition() ] = new BigInteger( keyId, 16 ).longValue();
                    
                }
                
            }
            
        } catch( SecurityException e ) {
        	post( R.string.insufficient_pgpkeyring_permissions, context ); 
        } finally {
            if( c != null ) {
                c.close();
            }
        }
        
        return keyids;
            
    }
    
    /**
     * Get key ids in public key rings based on a given email. For encrypting keys.
     *
     * @param context
     * @param email The email in question.
     * @return key ids
     */
    @Override
    public long[] getPublicKeyIdsFromEmail( Context context, String email ) {
    
        String[] projection = new String[] { PROVIDER_KEYID };
        
        long[] keyids = null;
        Uri.Builder builder = uriSelectPublicKeysByEmail.buildUpon();
        builder.appendPath( email );
        Uri uri = builder.build();
               
        Cursor c = null;
        try {
                
            c = context.getContentResolver().query( uri, projection, null, null, null );
            if( c != null ) {
                
                keyids = new long[ c.getCount() ];
            
                while( c.moveToNext() ) {
                    
                    String keyId = c.getString( 0 );
                    keyids[ c.getPosition() ] = new BigInteger( keyId, 16 ).longValue();
                    
                }
                
            }
                
        } catch( SecurityException e ) {
        	post( R.string.insufficient_pgpkeyring_permissions, context );  
        } finally {
            if( c != null ) {
                c.close();
            }
        }
        
        return keyids;
        
    }
    
    /**
     * Find out if a given email has a secret key. For encrypting keys.
     *
     * @param context
     * @param email The email in question.
     * @return true if there is a secret key for this email.
     */
    @Override
    public boolean hasSecretKeyForEmail( Context context, String email ) {
        
        long[] keyids = getSecretKeyIdsFromEmail( context, email );
        return keyids != null && keyids.length > 0;
        
    }
    
    /**
     * Find out if a given email has a public key. For encrypting keys.
     *
     * @param context
     * @param email The email in question.
     * @return true if there is a public key for this email.
     */
    @Override
    public boolean hasPublicKeyForEmail( Context context, String email ) {
        
        long[] keyids = getPublicKeyIdsFromEmail( context, email );
        return keyids != null && keyids.length > 0;
        
    } 
    
    /**
     * Get the user id based on the key id.
     *
     * @param context
     * @param keyId
     * @return user id
     */
    @Override
    public String getUserId( Context context, long keyId ) {
    
        String[] projection = new String[] { PROVIDER_USERID };
        
        Uri.Builder builder = uriSelectPrimaryUserIdByKeyid.buildUpon();
        builder.appendEncodedPath( Long.toHexString( keyId ) );
        Uri uri = builder.build();
        
        Cursor c = null;
        String userId = null;
        try {
            
            c = context.getContentResolver().query( uri, projection, null, null, null );
        
            if( c != null && c.moveToFirst() ) {
                userId = c.getString( 0 );
            }
            
        } finally {
            if( c != null ) {
                c.close();
            }
        }
        
        return userId;
        
    }
    
    /**
     * Start the encrypt activity.
     *
     * @param activity
     * @param data
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean encrypt( Activity activity, String data, PgpData pgpData ) {
        
        boolean success = false;
        
        Intent i = new Intent( PGPKeyRingIntent.ENCRYPT_MSG_AND_RETURN );
        i.addCategory( Intent.CATEGORY_DEFAULT );
        i.setType( "text/plain" );
        i.putExtra( EXTRAS_MSG, data);
        i.putExtra( EXTRAS_ENCRYPTION_KEYIDS, pgpData.getEncryptionKeys() );
        i.putExtra( EXTRAS_SIGNATURE_KEYID, pgpData.getSignatureKeyId() );
        
        try {
            
            activity.startActivityForResult( i, ENCRYPT_MESSAGE );
            success = true;
            
        } catch( ActivityNotFoundException e ) {
        	post( R.string.error_activity_not_found, activity );
        }
        
        return success;
        
    }
    
    @Override
    public boolean encryptFile( Activity activity, String filename, PgpData pgpData ) {
    	
        boolean success = false;
        
        Intent i = new Intent( PGPKeyRingIntent.ENCRYPT_FILE_AND_RETURN );
        i.addCategory( Intent.CATEGORY_DEFAULT );
        i.setType( "text/plain" );
        i.putExtra( EXTRAS_FILENAME, filename );
        i.putExtra( EXTRAS_ENCRYPTION_KEYIDS, pgpData.getEncryptionKeys() );
        i.putExtra( EXTRAS_SIGNATURE_KEYID, pgpData.getSignatureKeyId() );
        i.putExtra( EXTRAS_SIGNATURE_ALG, SIG_ALG );
        
        try {
            
            activity.startActivityForResult( i, ENCRYPT_FILE );
            success = true;
            
        } catch( ActivityNotFoundException e ) {
        	post( R.string.error_activity_not_found, activity );
        }
        
        return success;
        
    }
    
    @Override
    public boolean sign( Activity activity, String filename, PgpData pgpData ) {
    	
    	boolean success = false;
    	
    	if( filename != null ) {
    		
    		Intent i = new Intent( PGPKeyRingIntent.SIGN_AND_RETURN );
    		i.addCategory( Intent.CATEGORY_DEFAULT );
            i.setType( "text/plain" );
            i.putExtra( EXTRAS_FILENAME, filename );
            i.putExtra( EXTRAS_SIGNATURE_KEYID, pgpData.getSignatureKeyId() );
            i.putExtra( EXTRAS_SIGNATURE_ALG, SIG_ALG );
            
            try {
                
                activity.startActivityForResult( i, SIGN );
                success = true;
            
            } catch( ActivityNotFoundException e ) {
            	post( R.string.error_activity_not_found, activity );
            }
            
    	}
    	
    	return success;
    	
    }
    
    /**
     * Start the decrypt activity.
     *
     * @param fragment
     * @param data
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean decrypt( Fragment fragment, String data, String originalCharset, PgpData pgpData ) {
        
        boolean success = false;
        
        if( data != null && data.length() > 0 ) {
            
        	Intent i = new Intent( PGPKeyRingIntent.DECRYPT_MSG_AND_RETURN );
            i.addCategory( Intent.CATEGORY_DEFAULT );
            i.setType( "text/plain" );
            i.putExtra( EXTRAS_MSG, data );
            
            // For inline encrypted messages, the charset is contained in the PGP headers. However, for signed messages,
            // we need to specify the charset as there is no PGP header.
            if( originalCharset != null ) {
            	i.putExtra( EXTRAS_CHARSET, originalCharset );
            }
            
            try {
            
            	fragment.startActivityForResult( i, DECRYPT_MESSAGE );
            	success = true;
            
            } catch( ActivityNotFoundException e ) {
            	post( R.string.error_activity_not_found, fragment.getActivity() );
            }
            
        }
        
        return success;
        
    }
    
    @Override
    public boolean decryptFile( Fragment fragment, String filename, boolean showFile, PgpData pgpData ) {
    	
    	boolean success = false;
        
        if( filename != null && filename.length() > 0 ) {
            
            Intent i = new Intent( PGPKeyRingIntent.DECRYPT_FILE_AND_RETURN );
            i.addCategory( Intent.CATEGORY_DEFAULT );
            i.setType( "text/plain" );
            i.putExtra( EXTRAS_FILENAME, filename );
            i.putExtra( EXTRAS_SHOW_FILE, showFile );
            
            String destFilename = pgpData.getFilename();
            if( destFilename != null ) {
            	i.putExtra( EXTRAS_DEST_FILENAME, destFilename );
            }
            
            try {
            
                fragment.startActivityForResult( i, DECRYPT_FILE );
                success = true;
            
            } catch( ActivityNotFoundException e ) {
            	post( R.string.error_activity_not_found, fragment.getActivity() );
            }
            
        }
        
        return success;
    	
    }
    
    @Override
    public boolean verify( Fragment fragment, String filename, String sig, PgpData pgpData ) {
    	
    	boolean success = false;
    	
    	if( filename != null && sig != null ) {
    		
    		Intent i = new Intent( PGPKeyRingIntent.VERIFY_AND_RETURN );
    		i.addCategory( Intent.CATEGORY_DEFAULT );
            i.setType( "text/plain" );
            i.putExtra( EXTRAS_FILENAME, filename );
            i.putExtra( EXTRAS_SIGNATURE, sig );
            
            try {
                
                fragment.startActivityForResult( i, VERIFY );
                success = true;
            
            } catch( ActivityNotFoundException e ) {
            	post( R.string.error_activity_not_found, fragment.getActivity() );
            }
            
    	}
    	
    	return success;
    	
    }
    
    /**
     * Handle the activity results that concern us.
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @return handled or not
     */
    @Override
    public boolean onActivityResult( Activity activity, int requestCode, int resultCode, Intent data, PgpData pgpData ) {
        
        switch (requestCode) {
        
        case SELECT_SECRET_KEY:
            
            if( resultCode == Activity.RESULT_OK && data != null ) {
          
	            ContentValues values = ( ContentValues )data.getParcelableExtra( EXTRAS_CHOSEN_KEY );
	            if( values != null ) {
	            
	            	pgpData.setSignatureKeyId( new BigInteger( values.getAsString( PROVIDER_KEYID ), 16 ).longValue() );
	            	pgpData.setSignatureUserId( values.getAsString( PROVIDER_IDENTITY ) );
	            
	            }
	            
            }
            
        	( ( MessageCompose )activity ).updateEncryptLayout();
            
            break;

        case SELECT_PUBLIC_KEYS:
            
            if( resultCode != Activity.RESULT_OK || data == null ) {            
                pgpData.setEncryptionKeys( null );
            } else {
                pgpData.setEncryptionKeys( data.getLongArrayExtra( EXTRAS_CHOSEN_KEYIDS ) );
            }
            
            ( ( MessageCompose )activity ).onEncryptionKeySelectionDone();
            
            break;

        case ENCRYPT_MESSAGE:
            
            if( resultCode != Activity.RESULT_OK || data == null ) {       
                pgpData.setEncryptionKeys( null );
            } else {   
                pgpData.setEncryptedData( data.getStringExtra( EXTRAS_MSG ) );
            }
            
            ( ( MessageCompose )activity ).onEncryptDone();
            
            break;
            
        case ENCRYPT_FILE:
        	
        	if( resultCode != Activity.RESULT_OK || data == null ) {
        		
                pgpData.setEncryptionKeys( null );
                pgpData.setFilename( null );
                
            } else {   
                pgpData.setFilename( data.getStringExtra( EXTRAS_FILENAME ) );
            }
            
            ( ( MessageCompose )activity ).onEncryptDone();
            
            break;
            
        case SIGN:
        	
        	if( resultCode != Activity.RESULT_OK || data == null ) { 
        		pgpData.setSignatureKeyId( 0L );
        	} else {	
        		pgpData.setSignature( data.getStringExtra( EXTRAS_SIGNATURE ) );
        	}
        	
        	( ( MessageCompose )activity ).onEncryptDone();
        	
        	break;
        		    

        default:
            return false;
            
        }

        return true;
    }

    @Override
    public boolean onDecryptActivityResult( CryptoDecryptCallback callback, int requestCode, int resultCode, Intent data, PgpData pgpData ) {

        switch( requestCode ) {
        
        case VERIFY:
        	
        	if( resultCode == Activity.RESULT_OK && data != null ) {

                pgpData.setSignatureUserId( data.getStringExtra( EXTRAS_SIGNATURE_IDENTITY ) );
                pgpData.setSignatureKeyId( data.getLongExtra( EXTRAS_SIGNATURE_KEYID, 0 ) );
                pgpData.setSignatureSuccess( data.getBooleanExtra( EXTRAS_SIGNATURE_SUCCESS, false ) );
                pgpData.setSignatureUnknown( data.getBooleanExtra( EXTRAS_SIGNATURE_UNKNOWN, false ) );
    
                //Log.w( NAME, "signature identity: " + pgpData.getSignatureUserId() + ", keyid: " + pgpData.getSignatureKeyId() + ", success: " + pgpData.getSignatureSuccess() + ", sigs unknown: " + pgpData.getSignatureUnknown() );
                callback.onDecryptDone( pgpData );
                
            } 
            

            break;
        	
        case DECRYPT_MESSAGE:
            
            if( resultCode == Activity.RESULT_OK && data != null ) {

                pgpData.setSignatureUserId( data.getStringExtra( EXTRAS_SIGNATURE_IDENTITY ) );
                pgpData.setSignatureKeyId( data.getLongExtra( EXTRAS_SIGNATURE_KEYID, 0 ) );
                pgpData.setSignatureSuccess( data.getBooleanExtra( EXTRAS_SIGNATURE_SUCCESS, false ) );
                pgpData.setSignatureUnknown( data.getBooleanExtra( EXTRAS_SIGNATURE_UNKNOWN, false ) );
    
                String decrypted = data.getStringExtra( EXTRAS_MSG );
                if( decrypted == null ) {
                	
                	String filename = data.getStringExtra( EXTRAS_FILENAME );
                	if( filename != null ) {
                		try {
                			decrypted = IOUtils.toString( new BufferedInputStream( new FileInputStream( filename ) ) );
                		} catch( Exception e ) {
                			Log.e( K9.LOG_TAG, "Unable to read decrypted data from file", e );
                		}
                	}
                	
                }
                
                pgpData.setDecryptedData( decrypted );
            
                //Log.w( NAME, "signature identity: " + pgpData.getSignatureUserId() + ", keyid: " + pgpData.getSignatureKeyId() + ", success: " + pgpData.getSignatureSuccess() + ", sigs unknown: " + pgpData.getSignatureUnknown() );
                callback.onDecryptDone( pgpData );
                
            } 
            

            break;
            
        case DECRYPT_FILE:
        	
        	if( resultCode == Activity.RESULT_OK && data != null ) {
        		
        		pgpData.setSignatureUserId( data.getStringExtra( EXTRAS_SIGNATURE_IDENTITY ) );
                pgpData.setSignatureKeyId( data.getLongExtra( EXTRAS_SIGNATURE_KEYID, 0 ) );
                pgpData.setSignatureSuccess( data.getBooleanExtra( EXTRAS_SIGNATURE_SUCCESS, false ) );
                pgpData.setSignatureUnknown( data.getBooleanExtra( EXTRAS_SIGNATURE_UNKNOWN, false ) );
    
        		pgpData.setFilename( data.getStringExtra( EXTRAS_FILENAME ) );
        		pgpData.setShowFile( data.getBooleanExtra( EXTRAS_SHOW_FILE, false ) );
        		
        		if( pgpData.showFile() ) {
        			callback.onDecryptFileDone( pgpData );
        		} else {
        			callback.onDecryptDone( pgpData );
        		}
        		
        	}
        	
        	break;
        	           
        default:
            return false;

        }

        return true;
    }
    
    @Override
    public boolean isEncrypted( Message message ) {
              
        String data = null;
        try {
            
            Part part = MimeUtility.findFirstPartByMimeType( message, "text/plain" );
            if( part == null ) {
                part = MimeUtility.findFirstPartByMimeType( message, "text/html" );
            }
            
            if( part != null ) {
                data = MimeUtility.getTextFromPart( part );
            }
            
        } catch( MessagingException e ) {
            return false;
        }

        if( data == null ) {
            return false;
        }

        Matcher matcher = PGP_MESSAGE.matcher( data );
       
        return matcher.matches();
        
    }

    @Override
    public boolean isSigned(Message message) {
             
        String data = null;
        try {
            
            Part part = MimeUtility.findFirstPartByMimeType( message, "text/plain" );
            if( part == null ) {
                part = MimeUtility.findFirstPartByMimeType( message, "text/html" );
            }
            
            if( part != null ) {
                data = MimeUtility.getTextFromPart( part );
            }
            
        } catch( MessagingException e ) {
            return false;
        }

        if( data == null ) {
            return false;
        }

        Matcher matcher = PGP_SIGNED_MESSAGE.matcher(data);
        return matcher.matches();
        
    }
    
    @Override
    public boolean supportsAttachments( Context context ) {
    	
    	boolean supportsAttachments = false;
    	
    	if( isAvailable( context ) ) { 
    	
	    	PackageInfo pi = null;
	        PackageManager packageManager = context.getPackageManager();
	        
	        try {
	        	if( isTrialVersion ) {
	        		pi = packageManager.getPackageInfo( PACKAGE_TRIAL, 0 );
	        	} else {
	        		pi = packageManager.getPackageInfo( PACKAGE_PAID, 0 );
	        	}	
	        } catch( NameNotFoundException e ) {
	        }
	        
	        if( pi != null && pi.versionCode >= VERSION_REQUIRED_ATTACHMENTS_MIN ) {
	        	supportsAttachments = true;
	        }
	        
    	}
        
        return supportsAttachments;
        
    }
    
    @Override
    public boolean supportsPgpMimeReceive( Context context ) {
    	
    	/*
    	boolean supportsFloatingSigs = false;
    	
    	if( isAvailable( context ) ) { 
    	
	    	PackageInfo pi = null;
	        PackageManager packageManager = context.getPackageManager();
	        
	        try {
	        	if( isTrialVersion ) {
	        		pi = packageManager.getPackageInfo( PACKAGE_TRIAL, 0 );
	        	} else {
	        		pi = packageManager.getPackageInfo( PACKAGE_PAID, 0 );
	        	}	
	        } catch( NameNotFoundException e ) {
	        }
	        
	        if( pi != null && pi.versionCode >= VERSION_REQUIRED_FLOATING_SIGS_MIN ) {
	        	supportsFloatingSigs = true;
	        }
	        
    	}
        
        return supportsFloatingSigs;
        */
    	return supportsPgpMimeSend( context );
    	
    }
    
    @Override
    public boolean supportsPgpMimeSend( Context context ) {

    	boolean supportsPgpMimeSend = false;
    	
    	if( isAvailable( context ) ) { 
    	
	    	PackageInfo pi = null;
	        PackageManager packageManager = context.getPackageManager();
	        
	        try {
	        	if( isTrialVersion ) {
	        		pi = packageManager.getPackageInfo( PACKAGE_TRIAL, 0 );
	        	} else {
	        		pi = packageManager.getPackageInfo( PACKAGE_PAID, 0 );
	        	}	
	        } catch( NameNotFoundException e ) {
	        }
	        
	        if( pi != null && pi.versionCode >= VERSION_REQUIRED_PGP_MIME_SEND ) {
	        	supportsPgpMimeSend = true;
	        }
	        
    	}
        
        return supportsPgpMimeSend;
        
    }
    
    public String getName() {
        return NAME;
    }
    
    /**
     * Test the PGP KeyRing installation.
     *
     * @return success or failure
     */
    @Override
    public boolean test( Context context ) {
        
        if ( !isAvailable( context ) ) {
            return false;
        }

        try {
            
            Uri.Builder builder = uriSelectPrimaryUserIdByKeyid.buildUpon();
            builder.appendEncodedPath( "1122334455667788" );
            Uri uri = builder.build();
            
            Cursor c = context.getContentResolver().query( uri, new String[] { "user_id" }, null, null, null );
            if( c != null ) {
                c.close();
            }
            
        } catch( SecurityException e ) {
        	post( R.string.insufficient_pgpkeyring_permissions, context );
        }

        return true;
    }
   
    private void setContentUris() {
        
        if( isTrialVersion ) {
            
            uriSelectPrivateSigningKey = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/private/sign" );
            uriSelectPublicEncKey = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/public/encrypt" );
            uriSelectPublicKeysByEmail = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/public/email" );
            uriSelectPrivateKeysByEmail = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/private/email" );
            uriSelectPrimaryUserIdByKeyid = Uri.parse( "content://" + AUTHORITY_TRIAL + "/userid/keyid" );
            
        } else {

            uriSelectPrivateSigningKey = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/private/sign" );
            uriSelectPublicEncKey = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/public/encrypt" );
            uriSelectPublicKeysByEmail = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/public/email" );
            uriSelectPrivateKeysByEmail = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/private/email" );
            uriSelectPrimaryUserIdByKeyid = Uri.parse( "content://" + AUTHORITY_PAID + "/userid/keyid" );
            
        }
        
    }
    
    private void post( final int resourceId, final Context context ) {
    	handler.post( new Runnable() {
    	
    		@Override
    		public void run() {
    			Toast.makeText( context, resourceId, Toast.LENGTH_LONG ).show();
    		}
    	
    	});
    }
    
}
