package test.client;

import com.sun.istack.internal.NotNull;
import com.sun.tools.javac.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author addozhang 2017/11/17
 */
public class RequestFuture<Response>{
    private Object lock = new Object();
    private final CountDownLatch latch;
    private volatile AtomicBoolean cancelled = new AtomicBoolean(false);
    private final RequestType type;
    private final long timeout; //Set as final, can NOT be updated once construct
    private final List<SendListener> listeners = new ArrayList<>();
    private Response result;

    public RequestFuture(long timeout, SendListener listener) {
        Assert.check(timeout > 0, "Timeout setting should be greater than 0");
        //count=1 no response future
        //count=2 blocking future
        this.type = listener != null ? RequestType.BLOCKING : RequestType.NO_RESPOSE;
        latch = new CountDownLatch(type.count());
        this.timeout = timeout;
        if(listener != null) {
            listeners.add(listener);
        }
    }

    public boolean cancel() {
        synchronized (lock) {
            cancelled.set(true);
            while(latch.getCount() > 0) {
                latch.countDown(); //Fast fail
            }
        }
        return true;

    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public boolean isDone() {
        return latch.getCount() == 0;
    }

    public Response get() throws InterruptedException, ExecutionException {
        try {
            return get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            //doNothing
        }
        return null;
    }

    public Response get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        latch.await(timeout, TimeUnit.MILLISECONDS);
        int state = (int) latch.getCount();
        if (state != 0 && !cancelled.get()) { // Filter out state change caused by cancel
            fireListener(state);
        }
        return result;
    }

    public void set(Response result) {
        this.result = result;
    }

    public void done() {
        synchronized (lock) {
            latch.countDown();
        }
    }

    private void fireListener(int state) {
        if (state == 0) {
            return;
        }
        if (type == RequestType.BLOCKING && state == 1) {
            onReceiveTimeout();
        } else {
            onSendTimeout();
        }
    }

    private void onSendTimeout() {
        for (SendListener listener : listeners) {
            listener.onSendTimeout();
        }
    }

    private void onReceiveTimeout() {
        for (SendListener listener : listeners) {
            listener.onReceiveTimeout();
        }
    }

    //Used for add system listener
    public void addListener(@NotNull SendListener listener) {
        Assert.checkNonNull(listener, "Listener can not be null");
        listeners.add(listener);
    }
}
