package com.linlin.lpa2;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.euicc.EuiccManager;

public class MyUicc extends Service {
    public MyUicc() {
        EuiccManager mgr = (EuiccManager) getSystemService(EUICC_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
