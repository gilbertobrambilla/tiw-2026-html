package it.polimi.tiw.beans;

import java.util.List;

public class ProjectStructure {
    private final Project project;
    private final List<WPWithTasks> workPackages;

    public ProjectStructure(Project project, List<WPWithTasks> workPackages) {
        this.project = project;
        this.workPackages = workPackages;
    }

    public Project getProject() {
        return project;
    }

    public List<WPWithTasks> getWorkPackages() {
        return workPackages;
    }
}
