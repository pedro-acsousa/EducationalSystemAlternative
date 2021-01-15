package epic.lms;

import java.util.List;

public class Assessment {

    private String creator;
    private String due;
    private String fileUrl;
    private String spec;
    private boolean submitted;
    private String type;
    private String datesubmitted;
    private int mark;




    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getDue() {
        return due;
    }

    public void setDue(String due) {
        this.due = due;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDatesubmitted() {
        return datesubmitted;
    }

    public void setDatesubmitted(String datesubmitted) {
        this.datesubmitted = datesubmitted;
    }
}
