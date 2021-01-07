package epic.lms;


import java.util.List;

public class Modules {
    private List<String> assignments;
    private String description;
    private String id;
    private List<String> lecturers;
    private List<String> students;
    private String title;


    public List<String> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<String> assignments) {
        this.assignments = assignments;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getLecturers() {
        return lecturers;
    }

    public void setLecturers(List<String> lecturers) {
        this.lecturers = lecturers;
    }

    public List<String> getStudents() {
        return students;
    }

    public void setStudents(List<String> students) {
        this.students = students;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
