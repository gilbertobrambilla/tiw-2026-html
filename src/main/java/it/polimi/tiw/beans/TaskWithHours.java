package it.polimi.tiw.beans;

public class TaskWithHours {
    private final Task task;
    private final int totalPlannedHours;
    private final int totalWorkedHours;

    public TaskWithHours(Task task, int totalPlannedHours, int totalWorkedHours) {
        this.task = task;
        this.totalPlannedHours = totalPlannedHours;
        this.totalWorkedHours = totalWorkedHours;
    }

    public Task getTask() {
        return task;
    }

    public int getTotalPlannedHours() {
        return totalPlannedHours;
    }

    public int getTotalWorkedHours() {
        return totalWorkedHours;
    }
}
