package test.client;

/**
 * @author addozhang 2017/11/17
 */
public class RequestKey {
    private String uuid;
    private RequestType requestType;
    //more property here

    public RequestKey(String uuid, RequestType requestType) {
        this.uuid = uuid;
        this.requestType = requestType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }
}
