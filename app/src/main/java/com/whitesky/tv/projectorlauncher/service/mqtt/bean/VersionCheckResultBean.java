package com.whitesky.tv.projectorlauncher.service.mqtt.bean;

/**
 * Created by jeff on 18-3-5.
 */

public class VersionCheckResultBean {

    public class Result {

        private String sysVer = "V4.4";
        private String appVer = "V0.0";
        private int size = 0;
        private String url;
        private int createdAt = 0;
        public void setSysVer(String ver) {
            this.sysVer = ver;
        }
        public String getSysVer() {
            return sysVer;
        }

        public void setAppVer(String ver) {
            this.appVer = ver;
        }
        public String getAppVer() {
            return appVer;
        }

        public void setSize(int size) {
            this.size = size;
        }
        public int getSize() {
            return size;
        }

        public void setUrl(String url) {
            this.url = url;
        }
        public String getUrl() {
            return url;
        }

        public void setCreatedAt(int createdAt) {
            this.createdAt = createdAt;
        }
        public int getCreatedAt() {
            return createdAt;
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
