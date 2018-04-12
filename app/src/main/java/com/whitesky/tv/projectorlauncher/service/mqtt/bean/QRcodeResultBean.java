package com.whitesky.tv.projectorlauncher.service.mqtt.bean;

/**
 * Created by jeff on 18-3-5.
 */

public class QRcodeResultBean {

    public class Result {
        private String url;
        public void setUrl(String url) {
            this.url = url;
        }
        public String getUrl() {
            return url;
        }
    }

    private String status = "000000";
    private Result result;
    public void setStatus(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }

    public void setResult(Result result) {
        this.result = result;
    }
    public Result getResult() {
        return result;
    }

}
