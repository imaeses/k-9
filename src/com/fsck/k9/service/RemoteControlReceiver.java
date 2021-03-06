
package com.fsck.k9.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.remotecontrol.K9RemoteControl;
import com.fsck.k9.Preferences;
import com.imaeses.squeaky.K9;

import static com.fsck.k9.remotecontrol.K9RemoteControl.*;

public class RemoteControlReceiver extends CoreReceiver {
    @Override
    public Integer receive(Context context, Intent intent, Integer tmpWakeLockId) {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "RemoteControlReceiver.onReceive" + intent);

        if (K9RemoteControl.SQUEAKY_SET.equals(intent.getAction())) {
            RemoteControlService.set(context, intent, tmpWakeLockId);
            tmpWakeLockId = null;
        } else if (K9RemoteControl.SQUEAKY_REQUEST_ACCOUNTS.equals(intent.getAction())) {
            try {
                Preferences preferences = Preferences.getPreferences(context);
                Account[] accounts = preferences.getAccounts();
                String[] uuids = new String[accounts.length];
                String[] descriptions = new String[accounts.length];
                for (int i = 0; i < accounts.length; i++) {
                    //warning: account may not be isAvailable()
                    Account account = accounts[i];

                    uuids[i] = account.getUuid();
                    descriptions[i] = account.getDescription();
                }
                Bundle bundle = getResultExtras(true);
                bundle.putStringArray(SQUEAKY_ACCOUNT_UUIDS, uuids);
                bundle.putStringArray(SQUEAKY_ACCOUNT_DESCRIPTIONS, descriptions);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Could not handle K9_RESPONSE_INTENT", e);
            }

        }

        return tmpWakeLockId;
    }

}
