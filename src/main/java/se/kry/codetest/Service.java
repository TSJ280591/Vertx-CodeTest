package se.kry.codetest;

public class Service {

    private String name;
    private String url;
    private String insertTm;
    private String status;

    public Service() {
    }

    public Service(String name, String url, String insertTm, String status) {
        this.name = name;
        this.url = url;
        this.insertTm = insertTm;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInsertTm() {
        return insertTm;
    }

    public void setInsertTm(String insertTm) {
        this.insertTm = insertTm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Service{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", insertTm='" + insertTm + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

