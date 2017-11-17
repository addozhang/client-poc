package test.client;

/**
 * @author addozhang 2017/11/17
 */
public interface SendListener {
    void onSendTimeout();
    void onReceiveTimeout();
}
