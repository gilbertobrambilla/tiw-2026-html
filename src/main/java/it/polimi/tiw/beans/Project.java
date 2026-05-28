package it.polimi.tiw.beans;

public class Project {
    private final int id;
    private final String title;
    private final int durationMonths;
    private final ProjectStatus status;
    private final int createdBy;
    private final int managedBy;

    public Project(int id, String title, int durationMonths, ProjectStatus status, int createdBy, int managedBy) {
        this.id = id;
        this.title = title;
        this.durationMonths = durationMonths;
        this.status = status;
        this.createdBy = createdBy;
        this.managedBy = managedBy;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public int getManagedBy() {
        return managedBy;
    }
}
