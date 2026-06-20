package com.kalicyh.onemate;

import android.app.Application;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class App extends Application implements XposedServiceHelper.OnServiceListener {
    private static final Set<ServiceStateListener> LISTENERS = new CopyOnWriteArraySet<>();
    private static volatile XposedService service;

    static void addServiceStateListener(ServiceStateListener listener) {
        LISTENERS.add(listener);
        listener.onServiceStateChanged(service);
    }

    static void removeServiceStateListener(ServiceStateListener listener) {
        LISTENERS.remove(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(this);
    }

    @Override
    public void onServiceBind(XposedService service) {
        App.service = service;
        notifyServiceStateChanged(service);
    }

    @Override
    public void onServiceDied(XposedService service) {
        if (App.service == service) {
            App.service = null;
        }
        notifyServiceStateChanged(App.service);
    }

    private static void notifyServiceStateChanged(XposedService service) {
        for (ServiceStateListener listener : LISTENERS) {
            listener.onServiceStateChanged(service);
        }
    }

    interface ServiceStateListener {
        void onServiceStateChanged(XposedService service);
    }
}
