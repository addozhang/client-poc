package test.client;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author addozhang 2017/11/17
 */
public class SendAccumulator<Response> {
    private ConcurrentHashMap<String, RequestFuture<Response>> map = new ConcurrentHashMap<>();

    public void put(String uuid, RequestFuture future) {
        map.put(uuid, future);
    }

    public RequestFuture<Response> get(String uuid) {
        return map.get(uuid);
    }

    public void remove(String uuid) {
        map.remove(uuid);
    }
}
