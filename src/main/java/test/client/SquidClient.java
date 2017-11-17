package test.client;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RetriableException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * @author addozhang 2017/11/17
 */
public class SquidClient<Request, Response> {

    private KafkaProducer<RequestKey, Request> sender;
    private KafkaConsumer<RequestKey, Response> receiver;
    private SendAccumulator sendAccumulator;
    private long timeout;
    private long deadline;

    public SquidClient(long timeout, long deadline) {//use builder to accept more config
        this.timeout = timeout;
        this.deadline = deadline;
        sender = new KafkaProducer<>(new HashMap<>()); //
        receiver = new KafkaConsumer<>(new HashMap<>()); //TODO
        sendAccumulator = new SendAccumulator();

        startConsumer();
        //TODO start watcher to watch timeout and deadline update.
    }

    public Response send(Request request) {
        return send(request, null);
    }

    public Response send(Request request, SendListener listener) {
        String uuid = UUID.randomUUID().toString();
        RequestFuture<Response> future = new RequestFuture<>(timeout, listener);
        sendAccumulator.put(uuid, future);
        RequestKey key = new RequestKey(uuid, listener != null ? RequestType.BLOCKING : RequestType.NO_RESPOSE);
        sender.send(new ProducerRecord<>("topic", key, request), (metadata, exception) -> {
            if (exception == null) {
                future.done();
            } else if (!(exception instanceof RetriableException)) { // if non-RetriableException, fail fast;
                if (!future.isCancelled() && !future.isDone()) {
                    future.cancel();
                }
            }
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            //TODO actually no InterruptedException will be thrown
        } catch (ExecutionException e) {
            e.printStackTrace();
            //TODO actually no ExecutionException will be thrown
        } finally {
            //no matter what send timeout, receive timeout or success, future will be removed
            sendAccumulator.remove(uuid);
        }
        return null;
    }

    private void startConsumer() {
        while (true) {
            ConsumerRecords<RequestKey, Response> records = receiver.poll(300);
            Iterator<ConsumerRecord<RequestKey, Response>> iterator = records.iterator();
            while (iterator.hasNext()) {
                ConsumerRecord<RequestKey, Response> record = iterator.next();
                RequestFuture future = sendAccumulator.get(record.key().getUuid());//send uuid back in response
                if (future != null) {
                    future.set(record.value());
                    future.done();
                } else {
                    /**
                     * TODO two cases, just do logging
                     * 1. No response mode, but server responds
                     * 2. Client timeout, future deleted. But server responds
                     */

                }
            }
        }
    }
}
