package com.fsck.k9.fragment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.CryptoProvider;
import com.fsck.k9.crypto.CryptoProvider.CryptoDecryptCallback;
import com.fsck.k9.crypto.PGPKeyRing;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.view.AttachmentView;
import com.fsck.k9.view.AttachmentView.AttachmentFileDownloadCallback;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.SingleMessageView;
import com.imaeses.keyring.remote.CryptoService;
import com.imaeses.squeaky.K9;
import com.imaeses.squeaky.R;

public class MessageViewFragment extends Fragment implements OnClickListener,
        CryptoDecryptCallback, ConfirmationDialogFragmentListener {

    private static final String ARG_REFERENCE = "reference";

    private static final String STATE_MESSAGE_REFERENCE = "reference";
    private static final String STATE_PGP_DATA = "pgpData";

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private static final int ACTIVITY_CHOOSE_DIRECTORY = 3;


    public static MessageViewFragment newInstance(MessageReference reference) {
        MessageViewFragment fragment = new MessageViewFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_REFERENCE, reference);
        fragment.setArguments(args);

        return fragment;
    }

    private ServiceConnection cryptoServiceConn;
    private SingleMessageView mMessageView;
    private PgpData mPgpData;
    private Account mAccount;
    private MessageReference mMessageReference;
    private Message mMessage;
    private String mPgpSignedMessage;
    private MessagingController mController;
    private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();
    private LayoutInflater mLayoutInflater;

    /** this variable is used to save the calling AttachmentView
     *  until the onActivityResult is called.
     *  => with this reference we can identity the caller
     */
    private AttachmentView attachmentTmpStore;

    /**
     * Used to temporarily store the destination folder for refile operations if a confirmation
     * dialog is shown.
     */
    private String mDstFolder;

    private MessageViewFragmentListener mFragmentListener;

    /**
     * {@code true} after {@link #onCreate(Bundle)} has been executed. This is used by
     * {@code MessageList.configureMenu()} to make sure the fragment has been initialized before
     * it is used.
     */
    private boolean mInitialized = false;

    private Context mContext;


    class MessageViewHandler extends Handler {

        public void progress(final boolean progress) {
            post(new Runnable() {
                @Override
                public void run() {
                    setProgress(progress);
                }
            });
        }

        public void addAttachment(final View attachmentView) {
            post(new Runnable() {
                @Override
                public void run() {
                    mMessageView.addAttachment(attachmentView);
                }
            });
        }

        /* A helper for a set of "show a toast" methods */
        private void showToast(final String message, final int toastLength)  {
            post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), message, toastLength).show();
                }
            });
        }

        public void networkError() {
            // FIXME: This is a hack. Fix the Handler madness!
            Context context = getActivity();
            if (context == null) {
                return;
            }

            showToast(context.getString(R.string.status_network_error), Toast.LENGTH_LONG);
        }

        public void invalidIdError() {
            Context context = getActivity();
            if (context == null) {
                return;
            }

            showToast(context.getString(R.string.status_invalid_id_error), Toast.LENGTH_LONG);
        }


        public void fetchingAttachment() {
            Context context = getActivity();
            if (context == null) {
                return;
            }

            showToast(context.getString(R.string.message_view_fetching_attachment_toast), Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity.getApplicationContext();

        try {
            mFragmentListener = (MessageViewFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() +
                    " must implement MessageViewFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This fragments adds options to the action bar
        setHasOptionsMenu(true);

        mController = MessagingController.getInstance(getActivity().getApplication());
        mInitialized = true;
    }
    
    @Override
    public void onStart() {
        
        super.onStart();
        
        cryptoServiceConn = new ServiceConnection() {

            public void onServiceConnected( ComponentName className, IBinder service ) {
                
                PGPKeyRing keyring = ( PGPKeyRing )mAccount.getCryptoProvider(); 
                keyring.setCryptoService( CryptoService.Stub.asInterface( service ) );
                
            }

            public void onServiceDisconnected( ComponentName className ) {
                
                PGPKeyRing keyring = ( PGPKeyRing )mAccount.getCryptoProvider(); 
                keyring.setCryptoService( null );
                
            }
            
        };
        
        if( mAccount != null && mAccount.getCryptoProvider() instanceof PGPKeyRing ) {
            getActivity().bindService( new Intent( CryptoService.class.getName() ), cryptoServiceConn, Context.BIND_AUTO_CREATE );
        }
        
    }
    
    @Override
    public void onStop() {
        
        super.onStop();
        
        try {
            getActivity().unbindService( cryptoServiceConn );
        } catch( Exception e ) {
            Log.w( K9.LOG_TAG, "Error unbinding from remote crypto service", e );
        }
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context context = new ContextThemeWrapper(inflater.getContext(),
                K9.getK9ThemeResourceId(K9.getK9MessageViewTheme()));
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mLayoutInflater.inflate(R.layout.message, container, false);


        mMessageView = (SingleMessageView) view.findViewById(R.id.message_view);

        //set a callback for the attachment view. With this callback the attachmentview
        //request the start of a filebrowser activity.
        mMessageView.setAttachmentCallback(new AttachmentFileDownloadCallback() {

            @Override
            public void showFileBrowser(final AttachmentView caller) {
                FileBrowserHelper.getInstance()
                .showFileBrowserActivity(MessageViewFragment.this,
                                         null,
                                         ACTIVITY_CHOOSE_DIRECTORY,
                                         callback);
                attachmentTmpStore = caller;
            }

            FileBrowserFailOverCallback callback = new FileBrowserFailOverCallback() {

                @Override
                public void onPathEntered(String path) {
                    attachmentTmpStore.writeFile(new File(path));
                }

                @Override
                public void onCancel() {
                    // canceled, do nothing
                }
            };
        });

        mMessageView.initialize(this);
        mMessageView.downloadRemainderButton().setOnClickListener(this);

        mFragmentListener.messageHeaderViewAvailable(mMessageView.getMessageHeaderView());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MessageReference messageReference;
        if (savedInstanceState != null) {
            mPgpData = (PgpData) savedInstanceState.get(STATE_PGP_DATA);
            messageReference = (MessageReference) savedInstanceState.get(STATE_MESSAGE_REFERENCE);
        } else {
            Bundle args = getArguments();
            messageReference = (MessageReference) args.getParcelable(ARG_REFERENCE);
        }

        displayMessage(messageReference, (mPgpData == null));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_MESSAGE_REFERENCE, mMessageReference);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
    }

    public void displayMessage(MessageReference ref) {
        displayMessage(ref, true);
    }

    private void displayMessage(MessageReference ref, boolean resetPgpData) {
        mMessageReference = ref;
        
        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "MessageView displaying message " + mMessageReference);
        }

        Context appContext = getActivity().getApplicationContext();
        mAccount = Preferences.getPreferences(appContext).getAccount(mMessageReference.accountUuid);

        if (resetPgpData) {
            // start with fresh, empty PGP data
            mPgpData = new PgpData();
        }

        // Clear previous message
        mMessageView.resetView();
        mMessageView.resetHeaderView();

        mController.loadMessageForView(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);

        mFragmentListener.updateMenu();
    }
    
    public void setAttachmentView( AttachmentView attachmentView ) {
    	attachmentTmpStore = attachmentView;
    }

    /**
     * Called from UI thread when user select Delete
     */
    public void onDelete() {
        if (K9.confirmDelete() || (K9.confirmDeleteStarred() && mMessage.isSet(Flag.FLAGGED))) {
            showDialog(R.id.dialog_confirm_delete);
        } else {
            delete();
        }
    }

    public void onToggleAllHeadersView() {
        mMessageView.getMessageHeaderView().onShowAdditionalHeaders();
    }

    public boolean allHeadersVisible() {
        return mMessageView.getMessageHeaderView().additionalHeadersVisible();
    }

    private void delete() {
        if (mMessage != null) {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            mFragmentListener.disableDeleteAction();
            Message messageToDelete = mMessage;
            mFragmentListener.showNextMessageOrReturn();
            mController.deleteMessages(Collections.singletonList(messageToDelete), null);
        }
    }

    public void onRefile(String dstFolder) {
        if (!mController.isMoveCapable(mAccount)) {
            return;
        }
        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder)) {
            return;
        }

        if (mAccount.getSpamFolderName().equals(dstFolder) && K9.confirmSpam()) {
            mDstFolder = dstFolder;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            refileMessage(dstFolder);
        }
    }

    private void refileMessage(String dstFolder) {
        String srcFolder = mMessageReference.folderName;
        Message messageToMove = mMessage;
        mFragmentListener.showNextMessageOrReturn();
        mController.moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
    }

    public void onReply() {
        if (mMessage != null) {
            mFragmentListener.onReply(mMessage, mPgpData);
        }
    }

    public void onReplyAll() {
        if (mMessage != null) {
            mFragmentListener.onReplyAll(mMessage, mPgpData);
        }
    }

    public void onForward() {
        if (mMessage != null) {
            mFragmentListener.onForward(mMessage, mPgpData);
        }
    }

    public void onToggleFlagged() {
        if (mMessage != null) {
            boolean newState = !mMessage.isSet(Flag.FLAGGED);
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.FLAGGED, newState);
            mMessageView.setHeaders(mMessage, mAccount);
        }
    }

    public void onMove() {
        if ((!mController.isMoveCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_MOVE);

    }

    public void onCopy() {
        if ((!mController.isCopyCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isCopyCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    public void onArchive() {
        onRefile(mAccount.getArchiveFolderName());
    }

    public void onSpam() {
        onRefile(mAccount.getSpamFolderName());
    }

    public void onSelectText() {
        mMessageView.beginSelectingText();
    }

    private void startRefileActivity(int activity) {
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.folderName);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, activity);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        if (mAccount.getCryptoProvider().onDecryptActivityResult(this, requestCode, resultCode, data, mPgpData)) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case ACTIVITY_CHOOSE_DIRECTORY: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        if (filePath != null) {
                            attachmentTmpStore.writeFile(new File(filePath));
                        }
                    }
                }
                break;
            }
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY: {
                if (data == null) {
                    return;
                }

                String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
                MessageReference ref = data.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);
                if (mMessageReference.equals(ref)) {
                    mAccount.setLastSelectedFolderName(destFolderName);
                    switch (requestCode) {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE: {
                            mFragmentListener.showNextMessageOrReturn();
                            moveMessage(ref, destFolderName);
                            break;
                        }
                        case ACTIVITY_CHOOSE_FOLDER_COPY: {
                            copyMessage(ref, destFolderName);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    public void onSendAlternate() {
        if (mMessage != null) {
            mController.sendAlternate(getActivity(), mAccount, mMessage);
        }
    }

    public void onToggleRead() {
        if (mMessage != null) {
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.SEEN, !mMessage.isSet(Flag.SEEN));
            mMessageView.setHeaders(mMessage, mAccount);
            String subject = mMessage.getSubject();
            displayMessageSubject(subject);
            mFragmentListener.updateMenu();
        }
    }

    private void onDownloadRemainder() {
        if (mMessage.isSet(Flag.X_DOWNLOADED_FULL)) {
            return;
        }
        mMessageView.downloadRemainderButton().setEnabled(false);
        mController.loadMessageForViewRemote(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.download: {
                ((AttachmentView)view).saveFile();
                break;
            }
            case R.id.download_remainder: {
                onDownloadRemainder();
                break;
            }
        }
    }

    private void setProgress(boolean enable) {
        if (mFragmentListener != null) {
            mFragmentListener.setProgress(enable);
        }
    }

    private void displayMessageSubject(String subject) {
        if (mFragmentListener != null) {
            mFragmentListener.displayMessageSubject(subject);
        }
    }

    public void moveMessage(MessageReference reference, String destFolderName) {
        mController.moveMessage(mAccount, mMessageReference.folderName, mMessage,
                destFolderName, null);
    }

    public void copyMessage(MessageReference reference, String destFolderName) {
        mController.copyMessage(mAccount, mMessageReference.folderName, mMessage,
                destFolderName, null);
    }
    
    private boolean handlePgpMimeEncrypted( Account account, Multipart mp ) {
    	
    	boolean isPgpMime = false;
    	
    	if( !mMessageView.haveHandledPgpMimeEncrypted() ) {
    		
        	try {
        	
        		//ByteArrayOutputStream baos = new ByteArrayOutputStream();
        		//mp.writeTo( baos );
        		//Log.e( K9.LOG_TAG, new String( baos.toByteArray() ) );
        		
        		Part p = mp.getBodyPart( 0 );
        		if( p != null && p.getMimeType().equals( "application/pgp-encrypted" ) ) {
        			
        			p = mp.getBodyPart( 1 );
        			if( p != null && p.getBody() != null && p.getMimeType().equals( "application/octet-stream" ) ) {
        				
        				InputStream is = p.getBody().getInputStream();
        				File inputFile = File.createTempFile( "enc", ".tmp", getActivity().getExternalCacheDir() );
        				inputFile.deleteOnExit();
        				File outputFile = File.createTempFile( "decr", ".tmp", getActivity().getExternalCacheDir() );
        				outputFile.deleteOnExit();
        				
        				BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream( inputFile ) );
        				IOUtils.copy( is, out );
        				out.close();
        				is.close();
        				
        				mPgpData.setPgpEncrypted( true );
        				mPgpData.setFilename( Uri.fromFile( outputFile ).toString() );
        				
        				isPgpMime = mMessageView.handlePgpEncrypted( MessageViewFragment.this, account, inputFile, mPgpData );
        				if( isPgpMime ) {
        					mMessageView.showStatusMessage( mContext.getString( R.string.pgp_mime_encrypted_msg ) );
        				}
        						
        			}
        			
        		}
        		
        	} catch( Exception e ) {
        		Log.e( K9.LOG_TAG, "Error processing message parts", e );
        	}
        	
    	}
    	
    	return isPgpMime;
    	
    }
    
    private boolean handlePgpMimeSigned( Account account, MimeMultipart mp ) {
    	
    	boolean isPgpMime = false;
 
    	int count = mp.getCount();
    	if( count != 2 ) {
			Log.w( K9.LOG_TAG, "I have a multipart/signed with " + mp.getCount() + " parts; ignoring" );
		}
		
    	if( !mMessageView.haveHandledPgpMimeSigned() && count == 2 ) {
 
    		MimeUtility.rawBodies( mp, true );
    		
        	try {
        		
        		Part msgPart = null;
        		Part sigPart = null;
        		       		
    			msgPart = mp.getBodyPart( 0 );
    			sigPart = mp.getBodyPart( 1 );
    			
        		if( sigPart == null ) {
        			
        			Log.w( K9.LOG_TAG, "I have a multipart/signed with more than two parts, or I cannot locate the signature of a MIME message with a signed multipart/mixed" );
        			return false;
        			
        		}
        		
        		if( sigPart != null && msgPart != null ) {
        				
        			ByteArrayOutputStream baos = new ByteArrayOutputStream();
        			msgPart.writeTo( baos );
        			String signedData = new String( baos.toByteArray() );
        			
        			/*
        			Log.w( K9.LOG_TAG, "Signed data:\n" + signedData );
        			if( signedData.length() > 2000 ) {
        				Log.w( K9.LOG_TAG, "End of signed data:\n" + signedData.substring( signedData.length() - 2000 ) );
        			}
        			*/
        			
        			InputStream is = sigPart.getBody().getInputStream();
        			String sig = IOUtils.toString( is, "US-ASCII" );
        			is.close();

        			mPgpData.setPgpSigned( true );
        				
        			isPgpMime = mMessageView.handlePgpSigned( MessageViewFragment.this, account, signedData.getBytes(), sig, mPgpData ); 
        				
        		}
        		
        	} catch( Exception e ) {
        		Log.e( K9.LOG_TAG, "Error processing message parts", e );
        	}
        	
        	MimeUtility.rawBodies( mp, false );
        	
    	}
    	
    	return isPgpMime;
    	
    }
    
    class Listener extends MessagingListener {
        @Override
        public void loadMessageForViewHeadersAvailable(final Account account, String folder, String uid,
                final Message message) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }

            /*
             * Clone the message object because the original could be modified by
             * MessagingController later. This could lead to a ConcurrentModificationException
             * when that same object is accessed by the UI thread (below).
             *
             * See issue 3953
             *
             * This is just an ugly hack to get rid of the most pressing problem. A proper way to
             * fix this is to make Message thread-safe. Or, even better, rewriting the UI code to
             * access messages via a ContentProvider.
             *
             */
            final Message clonedMessage = message.clone();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!clonedMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                            !clonedMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        String text = mContext.getString(R.string.message_view_downloading);
                        mMessageView.showStatusMessage(text);
                    }
                    mMessageView.setHeaders(clonedMessage, account);
                    final String subject = clonedMessage.getSubject();
                    if (subject == null || subject.equals("")) {
                        displayMessageSubject(mContext.getString(R.string.general_no_subject));
                    } else {
                        displayMessageSubject(clonedMessage.getSubject());
                    }
                    mMessageView.setOnFlagListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onToggleFlagged();
                        }
                    });
                }
            });
        }

        @Override
        public void loadMessageForViewBodyAvailable(final Account account, String folder,
                String uid, final Message message) {
            if (!mMessageReference.uid.equals(uid) ||
                    !mMessageReference.folderName.equals(folder) ||
                    !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mMessage = message;
                        
                    	mFragmentListener.updateMenu();
                    	
                    	boolean isPgpMime = false;
                    	
                    	if( message.getBody() instanceof MimeMultipart && mMessage.isSet( Flag.X_DOWNLOADED_FULL ) ) {
                    		
                            MimeMultipart mp = ( MimeMultipart )message.getBody();
                            
                            if( mp.getContentType().contains( "multipart/encrypted" ) &&
                            		MimeUtility.findFirstPartByMimeType( message, "application/pgp-encrypted" ) != null ) {
                    			isPgpMime = handlePgpMimeEncrypted( account, mp );
                    		} else if( mp.getContentType().contains( "multipart/signed" ) &&
                    				MimeUtility.findFirstPartByMimeType( message, "application/pgp-signature" ) != null ) {
                    	      
                    			MimeMultipart signedMultipart = message.getSignedMultipart();
                    			isPgpMime = handlePgpMimeSigned( account, signedMultipart != null ? signedMultipart : mp );
                    			
                    		}
                    		
                    	}
                        
                        if( !isPgpMime ) {
                       
                        	//Log.w( K9.LOG_TAG, "Not PGP/MIME message, so I'm just going to display it as normal" );
                        	mMessageView.setMessage(account, (LocalMessage) message, mPgpData,
                        			mController, mListener);
                        	
                        }

                    } catch (Exception e) {
                        Log.v(K9.LOG_TAG, "loadMessageForViewBodyAvailable", e);
                    }
                }
                
            });
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid, final Throwable t) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(false);
                    if (t instanceof IllegalArgumentException) {
                        mHandler.invalidIdError();
                    } else {
                        mHandler.networkError();
                    }
                    if (mMessage == null || mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        mMessageView.showStatusMessage(
                                mContext.getString(R.string.webview_empty_message));
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid, final Message message) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(false);
                    mMessageView.setShowDownloadButton(message);
                }
            });
        }

        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(true);
                }
            });
        }

        @Override
        public void loadAttachmentStarted(Account account, Message message, Part part, Object tag, final boolean requiresDownload) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMessageView.setAttachmentsEnabled(false);
                    showDialog(R.id.dialog_attachment_progress);
                    if (requiresDownload) {
                        mHandler.fetchingAttachment();
                    }
                }
            });
        }

        @Override
        public void loadAttachmentFinished(final Account account, Message message, Part part, final Object tag) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMessageView.setAttachmentsEnabled(true);
                    removeDialog(R.id.dialog_attachment_progress);
                    Object[] params = (Object[]) tag;
                    boolean download = (Boolean) params[0];
                    AttachmentView attachment = (AttachmentView) params[1];

                    if( attachment.decrypt.isChecked() ) {
                    	
                    	attachment.writeFile();
                    	CryptoProvider cryptoProvider = account.getCryptoProvider();
                        File file = new File( attachment.savedName );
                        
                        mPgpData.setFilename( null );
                        mPgpData.setShowFile( !download );
                        
                    	cryptoProvider.decryptFile( MessageViewFragment.this, Uri.fromFile( file ).toString(), mPgpData );
                    	
                    } else {
                    	if (download) {
                    		attachment.writeFile();
                    	} else {
                    		attachment.showFile();
                    	}
                    }
                    
                }
            });
        }

        @Override
        public void loadAttachmentFailed(Account account, Message message, Part part, Object tag, String reason) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMessageView.setAttachmentsEnabled(true);
                    removeDialog(R.id.dialog_attachment_progress);
                    mHandler.networkError();
                }
            });
        }
    }

    @Override
    public void requestCryptoPassword( CryptoProvider.CryptoRetry retry ) {
    
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( getActivity() );
        dialogBuilder.setTitle( getResources().getString( R.string.account_setup_basics_password_hint ) );
        final EditText input = new EditText( getActivity() );
        input.setTag( retry );
        dialogBuilder.setView( input );
        
        dialogBuilder.setPositiveButton( R.string.okay_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick( DialogInterface dialog, int whichButton ) {
                    
                    String password = input.getText().toString();
                    CryptoProvider.CryptoRetry retry = ( CryptoProvider.CryptoRetry )input.getTag();
                    PGPKeyRing crypto = ( PGPKeyRing )mAccount.getCryptoProvider();
                    
                    Object[] retryParams = retry.getRetryParams();
                    Object[] params = new Object[ retryParams.length + 2 ];
                    int i;
                    for( i=0; i<retryParams.length; i++ ) {
                        params[ i ] = retryParams[ i ];
                    }
                    params[ ++i ] = password;
                    params[ ++i ] = mPgpData;
                    
                    try {
                        retry.getRetryMethod().invoke( crypto, params );
                    } catch( Exception e ) {
                        Log.e( K9.LOG_TAG, "Unable to call crypto provider via reflection", e );
                    }
                    
                }
            });
        
        dialogBuilder.setNegativeButton( R.string.cancel_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick( DialogInterface dialog, int whichButton ) {
                    // do nothing
                }
            });
            
        dialogBuilder.show();
        
    }
    
    @Override
    public void onDecryptDone(PgpData pgpData) {
        
    	if( mMessage == null ) {
    		return;
    	}
    	
        Account account = mAccount;
        LocalMessage message = (LocalMessage) mMessage;
        MessagingController controller = mController;
        Listener listener = mListener;
        MimeMessage replacement = null;
        
    	if( pgpData.isPgpSigned() ) {

        	Message m = new MimeMessage();
            
            // Did we just verify a signed message that was also decrypted?
            if( mPgpSignedMessage != null ) {
            	
        		try {
        			
        			ByteArrayInputStream bais = new ByteArrayInputStream( mPgpSignedMessage.getBytes() );
        			replacement = new MimeMessage( bais );
        			//m = replacement;
        			
        		} catch( Throwable e ) {
        			
        			Log.e( K9.LOG_TAG, "Cannot parse previously saved PGP/MIME signed message???", e );
        			return;
        			
        		}
        		
            } 
          
            try {
            	
            	if( replacement == null ) {
            		m.setBody( mMessage.getSignedMultipart() );
            	} else {
            		m.setBody( replacement.getSignedMultipart() );
            	}
            	
            } catch( Exception e ) {
            	Log.w( K9.LOG_TAG, "Unable to set body", e );
            }
            	
            try {
            	
    			boolean isPlainText = false;
            	Part msgPart = MimeUtility.findFirstPartByMimeType( m, "text/html" );
            	if( msgPart == null ) {
            		
            		msgPart = MimeUtility.findFirstPartByMimeType( m, "text/plain" );
            		isPlainText = true;
            		
            	}
            	
            	if( msgPart != null ) {

	            	String[] header = msgPart.getHeader( MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING );
	            	if( header != null ) {
	            	    
	            	    InputStream is = msgPart.getBody().getInputStream();
	            		msgPart.setBody( MimeUtility.decodeBody( is, header[ 0 ], msgPart.getMimeType() ) );
	            		is.close();
	            		
	            	}
            		
            		String text = MimeUtility.getTextFromPart( msgPart );
            		
                	if( isPlainText && text != null ) {
            			text = HtmlConverter.textToHtml( text );
            		}
                	
            		//Log.w( K9.LOG_TAG, "SIGNED:\n" + text );
                	pgpData.setDecryptedData( text );
                	
            	}
            	
            } catch( Exception e ) {
            	Log.e( K9.LOG_TAG, "Unable to decode plain text now that PGP/MIME signature has been checked", e );
            }
            	
            mMessageView.setFilterPgpAttachments( true );
        	
        } else if( pgpData.isPgpEncrypted() ) {
    	
            String filename = pgpData.getFilename();
            if( filename != null && filename.length() > 0 ) {
            	
            	try {
            		
            		File f = new File( filename );
            		InputStream is = new BufferedInputStream( new FileInputStream( f ) );
            		String decryptedMsg = IOUtils.toString( is );
            		is.close();
            		f.delete();
            		
            		//Log.w( K9.LOG_TAG, "Decrypted msg: " + decryptedMsg );
            		
            		if( decryptedMsg != null && decryptedMsg.length() > 0 ) {
	    		
            			pgpData.setDecryptedData( decryptedMsg );

            			ByteArrayInputStream bais = new ByteArrayInputStream( decryptedMsg.getBytes() );
            			MimeMessage mimeMsg = new MimeMessage( bais );
            			Body body = mimeMsg.getBody();
            			if( body instanceof BinaryTempFileBody ) {
            					
            				Log.d( K9.LOG_TAG, "Decrypted data is a BinaryTempFileBody" );
            				String text = MimeUtility.getTextFromPart( mimeMsg );
            				String header[] = mimeMsg.getHeader( MimeHeader.HEADER_CONTENT_TYPE );
            				if( header != null && header[ 0 ].contains( "text/plain" ) ) {
            					text = HtmlConverter.textToHtml( text );
            				}
            				
            				pgpData.setDecryptedData( text );
            				
            			} else {
            				
            				Log.d( K9.LOG_TAG, "Decrypted data is a mime multipart" );
            				
	            			MimeMultipart mp = ( MimeMultipart )mimeMsg.getBody();
	            			
	            			// in case a decrypted PGP/MIME message revealed a signed message
	            			if( mp.getContentType().contains( "multipart/signed" ) && handlePgpMimeSigned( mAccount, mp ) ) {
		    				
	            				mPgpSignedMessage = pgpData.getDecryptedData();
	            				return;
		    				
	            			}
	            			
	            			// useful when we've already decrypted a message with more than one part and don't need to again
	            			// i.e. forwarding or screen reorientation
            				pgpData.setFullDecryptedMimeMessage( decryptedMsg );
		    			
	            			boolean isPlainText = false;
	            			Part p = MimeUtility.findFirstPartByMimeType( mimeMsg, "text/html" );
	            			if( p == null ) {
	            				
	            				p = MimeUtility.findFirstPartByMimeType( mimeMsg, "text/plain" );
	            				isPlainText = true;
	            				
	            			}
		    			
	            			if( p != null ) {
	            				
	                    		String text = MimeUtility.getTextFromPart( p );
	                    		
	                    		if( isPlainText && text != null ) {
	                    			text = HtmlConverter.textToHtml( text );
	                    		}
	                    		
	                    		//Log.w( K9.LOG_TAG, "DECRYPTED:\n" + text );
	            				pgpData.setDecryptedData( text );
	            				
	            			}
	            			
	            			replacement = mimeMsg;
	            			mMessage.copyInternals( replacement );
	            			mMessageView.setFilterPgpAttachments( true );
	            			
            			}
            			
            		}
	    			
            	} catch( Throwable e ) {
            		Log.i( K9.LOG_TAG, "Decrypted PGP/MIME message is not signed or encountered a problem parsing signed message", e );
            	}
            	
                mMessageView.setFilterPgpAttachments( true );
            		
            }
            
    	// This is a decrypted PGP inline message 
    	} else if( pgpData.getDecryptedData() != null ) {
    		
    		String decrypted = pgpData.getDecryptedData().trim();
    		//Log.w( K9.LOG_TAG, "Decrypted inline msg:\n" + decrypted );
    		
    		if( !decrypted.startsWith( "<html>" ) && !decrypted.startsWith( "<HTML>" ) ) {
    			pgpData.setDecryptedData( HtmlConverter.textToHtml( decrypted ) );
    		} 
    		
    	}
        
        try {
        	mMessageView.setMessage(account, message, pgpData, controller, listener, replacement);
        } catch (MessagingException e) {
        	Log.e(K9.LOG_TAG, "displayMessageBody failed", e);
        }
        
    }
    
    // Handles decryption of file attachments
    @Override
    public void onDecryptFileDone(PgpData pgpData) {
    	
    	if( mMessage == null ) {
    		return;
    	}
    	
        if( pgpData.showFile() ) {
    		
    		String filename = pgpData.getFilename();
    		String extension = MimeTypeMap.getFileExtensionFromUrl( filename );
    		if( extension != null ) {
    			
    			MimeTypeMap mime = MimeTypeMap.getSingleton();
    	        String mimeType = mime.getMimeTypeFromExtension( extension );
    	        
    	        if( mimeType != null ) {
    	        	
    	        	Uri uri = Uri.parse( filename );
    	            Intent intent = new Intent(Intent.ACTION_VIEW);
    	            // We explicitly set the ContentType in addition to the URI because some attachment viewers (such as Polaris office 3.0.x) choke on documents without a mime type
    	            intent.setDataAndType(uri, mimeType);
    	            intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET );
    	          
    	            try {
    	                getActivity().startActivity( Intent.createChooser( intent, null ).addFlags( Intent.FLAG_ACTIVITY_NEW_TASK ) );
    	            } catch (Exception e) {
    	                Log.e(K9.LOG_TAG, "Could not display attachment of type " + mimeType, e);
    	                Toast toast = Toast.makeText(mContext, mContext.getString(R.string.message_view_no_viewer, mimeType), Toast.LENGTH_LONG);
    	                toast.show();
    	            }
    	            
    	        } else {
    	        	
    	        	Toast toast = Toast.makeText(mContext, R.string.cannot_handle_file_extension, Toast.LENGTH_LONG);
    	            toast.show();
    	            
    	        }
    	        
    		} else {
    			
    			Toast toast = Toast.makeText(mContext, R.string.must_supply_file_extension, Toast.LENGTH_LONG);
	            toast.show();
	            
    		}
    		
    	}
    	
    }

    private void showDialog(int dialogId) {
        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                String title = getString(R.string.dialog_confirm_delete_title);
                String message = getString(R.string.dialog_confirm_delete_message);
                String confirmText = getString(R.string.dialog_confirm_delete_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_delete_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_confirm_spam: {
                String title = getString(R.string.dialog_confirm_spam_title);
                String message = getResources().getQuantityString(R.plurals.dialog_confirm_spam_message, 1);
                String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_attachment_progress: {
                String message = getString(R.string.dialog_attachment_progress_title);
                fragment = ProgressDialogFragment.newInstance(null, message);
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));
    }

    private void removeDialog(int dialogId) {
        FragmentManager fm = getFragmentManager();

        if (fm == null || isRemoving() || isDetached()) {
            return;
        }

        // Make sure the "show dialog" transaction has been processed when we call
        // findFragmentByTag() below. Otherwise the fragment won't be found and the dialog will
        // never be dismissed.
        fm.executePendingTransactions();

        DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(getDialogTag(dialogId));

        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    public void zoom(KeyEvent event) {
        mMessageView.zoom(event);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                delete();
                break;
            }
            case R.id.dialog_confirm_spam: {
                refileMessage(mDstFolder);
                mDstFolder = null;
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        /* do nothing */
    }

    @Override
    public void dialogCancelled(int dialogId) {
        /* do nothing */
    }

    /**
     * Get the {@link MessageReference} of the currently displayed message.
     */
    public MessageReference getMessageReference() {
        return mMessageReference;
    }

    public boolean isMessageRead() {
        return (mMessage != null) ? mMessage.isSet(Flag.SEEN) : false;
    }

    public boolean isCopyCapable() {
        return mController.isCopyCapable(mAccount);
    }

    public boolean isMoveCapable() {
        return mController.isMoveCapable(mAccount);
    }

    public boolean canMessageBeArchived() {
        return (!mMessageReference.folderName.equals(mAccount.getArchiveFolderName())
                && mAccount.hasArchiveFolder());
    }

    public boolean canMessageBeMovedToSpam() {
        return (!mMessageReference.folderName.equals(mAccount.getSpamFolderName())
                && mAccount.hasSpamFolder());
    }

    public void updateTitle() {
        if (mMessage != null) {
            displayMessageSubject(mMessage.getSubject());
        }
    }

    public interface MessageViewFragmentListener {
        public void onForward(Message mMessage, PgpData mPgpData);
        public void disableDeleteAction();
        public void onReplyAll(Message mMessage, PgpData mPgpData);
        public void onReply(Message mMessage, PgpData mPgpData);
        public void displayMessageSubject(String title);
        public void setProgress(boolean b);
        public void showNextMessageOrReturn();
        public void messageHeaderViewAvailable(MessageHeader messageHeaderView);
        public void updateMenu();
    }

    public boolean isInitialized() {
        return mInitialized ;
    }

    public LayoutInflater getFragmentLayoutInflater() {
        return mLayoutInflater;
    }
}
