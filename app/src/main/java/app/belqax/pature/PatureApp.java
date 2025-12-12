package app.belqax.pature;

import android.app.Application;

import app.belqax.pature.data.network.ApiClient;
import app.belqax.pature.data.storage.AuthStorage;

public class PatureApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AuthStorage authStorage = new AuthStorage(this);
        ApiClient.init(this, authStorage);
    }
}
