package com.fsck.k9.remotecontrol;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;

/**
 * Utillity definitions for Android applications to control the behavior of Squeaky Mail.  All such applications must declare the following permission:
 * <uses-permission android:name="com.imaeses.squeaky.REMOTE_CONTROL"/>
 * in their AndroidManifest.xml  In addition, all applications sending remote control messages to Squeaky Mail must
 *
 * An application that wishes to act on a particular Account in Squeaky Mail needs to fetch the list of configured Accounts by broadcasting an
 * {@link Intent} using SQUEAKY_REQUEST_ACCOUNTS as the Action.  The broadcast must be made using the {@link ContextWrapper}
 * sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver,
 * Handler scheduler, int initialCode, String initialData, Bundle initialExtras).sendOrderedBroadcast}
 * method in order to receive the list of Account UUIDs and descriptions that Squeaky Mail will provide.
 *
 * @author Daniel I. Applebaum
 *
 */
public class K9RemoteControl {
    /**
     * Permission that every application sending a broadcast to Squeaky Mail for Remote Control purposes should send on every broadcast.
     * Prevent other applications from intercepting the broadcasts.
     */
    public final static String SQUEAKY_REMOTE_CONTROL_PERMISSION = "com.imaeses.squeaky.REMOTE_CONTROL";
    /**
     * {@link Intent} Action to be sent to Squeaky Mail using {@link ContextWrapper.sendOrderedBroadcast} in order to fetch the list of configured Accounts.
     * The responseData will contain two String[] with keys SQUEAKY_ACCOUNT_UUIDS and SQUEAKY_ACCOUNT_DESCRIPTIONS
     */
    public final static String SQUEAKY_REQUEST_ACCOUNTS = "com.imaeses.squeaky.K9RemoteControl.requestAccounts";
    public final static String SQUEAKY_ACCOUNT_UUIDS = "com.imaeses.squeaky.K9RemoteControl.accountUuids";
    public final static String SQUEAKY_ACCOUNT_DESCRIPTIONS = "com.imaeses.squeaky.K9RemoteControl.accountDescriptions";

    /**
     * The {@link {@link Intent}} Action to set in order to cause Squeaky Mail to check mail.  (Not yet implemented)
     */
    //public final static String SQUEAKY_CHECK_MAIL = "com.imaeses.squeaky.K9RemoteControl.checkMail";

    /**
     * The {@link {@link Intent}} Action to set when remotely changing Squeaky Mail settings
     */
    public final static String SQUEAKY_SET = "com.imaeses.squeaky.K9RemoteControl.set";
    /**
     * The key of the {@link Intent} Extra to set to hold the UUID of a single Account's settings to change.  Used only if SQUEAKY_ALL_ACCOUNTS
     * is absent or false.
     */
    public final static String SQUEAKY_ACCOUNT_UUID = "com.imaeses.squeaky.K9RemoteControl.accountUuid";
    /**
     * The key of the {@link Intent} Extra to set to control if the settings will apply to all Accounts, or to the one
     * specified with SQUEAKY_ACCOUNT_UUID
     */
    public final static String SQUEAKY_ALL_ACCOUNTS = "com.imaeses.squeaky.K9RemoteControl.allAccounts";

    public final static String SQUEAKY_ENABLED = "true";
    public final static String SQUEAKY_DISABLED = "false";

    /*
     * Key for the {@link Intent} Extra for controlling whether notifications will be generated for new unread mail.
     * Acceptable values are SQUEAKY_ENABLED and SQUEAKY_DISABLED
     */
    public final static String SQUEAKY_NOTIFICATION_ENABLED = "com.imaeses.squeaky.K9RemoteControl.notificationEnabled";
    /*
     * Key for the {@link Intent} Extra for controlling whether Squeaky Mail will sound the ringtone for new unread mail.
     * Acceptable values are SQUEAKY_ENABLED and SQUEAKY_DISABLED
     */
    public final static String SQUEAKY_RING_ENABLED = "com.imaeses.squeaky.K9RemoteControl.ringEnabled";
    /*
     * Key for the {@link Intent} Extra for controlling whether Squeaky Mail will activate the vibrator for new unread mail.
     * Acceptable values are SQUEAKY_ENABLED and SQUEAKY_DISABLED
     */
    public final static String SQUEAKY_VIBRATE_ENABLED = "com.imaeses.squeaky.K9RemoteControl.vibrateEnabled";

    public final static String SQUEAKY_FOLDERS_NONE = "NONE";
    public final static String SQUEAKY_FOLDERS_ALL = "ALL";
    public final static String SQUEAKY_FOLDERS_FIRST_CLASS = "FIRST_CLASS";
    public final static String SQUEAKY_FOLDERS_FIRST_AND_SECOND_CLASS = "FIRST_AND_SECOND_CLASS";
    public final static String SQUEAKY_FOLDERS_NOT_SECOND_CLASS = "NOT_SECOND_CLASS";
    /**
     * Key for the {@link Intent} Extra to set for controlling which folders to be synchronized with Push.
     * Acceptable values are SQUEAKY_FOLDERS_ALL, SQUEAKY_FOLDERS_FIRST_CLASS, SQUEAKY_FOLDERS_FIRST_AND_SECOND_CLASS,
     * SQUEAKY_FOLDERS_NOT_SECOND_CLASS, SQUEAKY_FOLDERS_NONE
     */
    public final static String SQUEAKY_PUSH_CLASSES = "com.imaeses.squeaky.K9RemoteControl.pushClasses";
    /**
     * Key for the {@link Intent} Extra to set for controlling which folders to be synchronized with Poll.
     * Acceptable values are SQUEAKY_FOLDERS_ALL, SQUEAKY_FOLDERS_FIRST_CLASS, SQUEAKY_FOLDERS_FIRST_AND_SECOND_CLASS,
     * SQUEAKY_FOLDERS_NOT_SECOND_CLASS, SQUEAKY_FOLDERS_NONE
     */
    public final static String SQUEAKY_POLL_CLASSES = "com.imaeses.squeaky.K9RemoteControl.pollClasses";

    public final static String[] SQUEAKY_POLL_FREQUENCIES = { "-1", "1", "5", "10", "15", "30", "60", "120", "180", "360", "720", "1440"};
    /**
     * Key for the {@link Intent} Extra to set with the desired poll frequency.  The value is a String representing a number of minutes.
     * Acceptable values are available in SQUEAKY_POLL_FREQUENCIES
     */
    public final static String SQUEAKY_POLL_FREQUENCY = "com.imaeses.squeaky.K9RemoteControl.pollFrequency";

    /**
     * Key for the {@link Intent} Extra to set for controlling Squeaky Mail's global "Background sync" setting.
     * Acceptable values are SQUEAKY_BACKGROUND_OPERATIONS_ALWAYS, SQUEAKY_BACKGROUND_OPERATIONS_NEVER
     * SQUEAKY_BACKGROUND_OPERATIONS_WHEN_CHECKED_AUTO_SYNC
     */
    public final static String SQUEAKY_BACKGROUND_OPERATIONS = "com.imaeses.squeaky.K9RemoteControl.backgroundOperations";
    public final static String SQUEAKY_BACKGROUND_OPERATIONS_ALWAYS = "ALWAYS";
    public final static String SQUEAKY_BACKGROUND_OPERATIONS_NEVER = "NEVER";
    public final static String SQUEAKY_BACKGROUND_OPERATIONS_WHEN_CHECKED_AUTO_SYNC = "WHEN_CHECKED_AUTO_SYNC";

    /**
     * Key for the {@link Intent} Extra to set for controlling which display theme Squeaky Mail will use.  Acceptable values are
     * SQUEAKY_THEME_LIGHT, SQUEAKY_THEME_DARK
     */
    public final static String SQUEAKY_THEME = "com.imaeses.squeaky.K9RemoteControl.theme";
    public final static String SQUEAKY_THEME_LIGHT = "LIGHT";
    public final static String SQUEAKY_THEME_DARK = "DARK";

    protected static String LOG_TAG = "K9RemoteControl";

    public static void set(Context context, Intent broadcastIntent) {
        broadcastIntent.setAction(K9RemoteControl.SQUEAKY_SET);
        context.sendBroadcast(broadcastIntent, K9RemoteControl.SQUEAKY_REMOTE_CONTROL_PERMISSION);
    }

    public static void fetchAccounts(Context context, K9AccountReceptor receptor) {
        Intent accountFetchIntent = new Intent();
        accountFetchIntent.setAction(K9RemoteControl.SQUEAKY_REQUEST_ACCOUNTS);
        AccountReceiver receiver = new AccountReceiver(receptor);
        context.sendOrderedBroadcast(accountFetchIntent, K9RemoteControl.SQUEAKY_REMOTE_CONTROL_PERMISSION, receiver, null, Activity.RESULT_OK, null, null);
    }

}


