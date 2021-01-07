package epic.lms;

import java.util.List;

public class User {
    private String firstname;
    private String password;
    private String role;
    private String username ;
    private String surname;
    private List<String> teaches;
    private List<String> attends;
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getTeaches() {
        return teaches;
    }

    public void setTeaches(List<String> teaches) {
        this.teaches = teaches;
    }

    public List<String> getAttends() {
        return attends;
    }

    public void setAttends(List<String> attends) {
        this.attends = attends;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
