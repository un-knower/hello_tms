package xyz.sysoul;

import xyz.sysoul.callback.MessageCallback;
import xyz.sysoul.client.AutoTMSClient;

public class Application {
    public static void main(String[] args) {
        AutoTMSClient autoTMSClient = new AutoTMSClient(new MessageCallback() {
            @Override
            public void start(String msg) {

            }

            @Override
            public void stop(String msg) {

            }
        });
    }
}
