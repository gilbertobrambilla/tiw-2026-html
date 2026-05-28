package it.polimi.tiw.beans;

import java.util.List;

public class ProjectMonthlyReport {
    private final Project project;
    private final List<WPMonthlyReport> workPackages;
    private final boolean closable;

    public ProjectMonthlyReport(Project project, List<WPMonthlyReport> workPackages, boolean closable) {
        this.project = project;
        this.workPackages = workPackages;
        this.closable = closable;
    }

    public Project getProject() {
        return project;
    }

    public List<WPMonthlyReport> getWorkPackages() {
        return workPackages;
    }

    public boolean isClosable() {
        return closable;
    }
}
