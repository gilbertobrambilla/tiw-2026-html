package it.polimi.tiw.beans;

public class UserLogin {
    private final int id;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final UserRole role;

    public UserLogin(int id, String username, String firstName, String lastName, UserRole role) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserRole getRole() {
        return role;
    }
}
