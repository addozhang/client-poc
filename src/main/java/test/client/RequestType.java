package test.client;

/**
 * @author addozhang 2017/11/17
 */
public enum RequestType {
    NO_RESPOSE(1),
    BLOCKING(2);
    private int count;
    RequestType(int count) {
        this.count = count;
    }

    public int count() {
        return count;
    }
}
