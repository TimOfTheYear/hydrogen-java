package com.hydrogen;

/**
 * Created by nathan on 1/13/16.
 */
public interface IHydrogen {
    void onConnected();
    void onDisconnected();
    void onError(Exception e);
    void onDataReceived(byte[] buffer);
}
