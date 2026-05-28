package it.polimi.tiw.beans;

public class TaskWithHours {
    private final Task task;
    private final int totalExpectedHours;
    private final int totalWorkedHours;

    public TaskWithHours(Task task, int totalExpectedHours, int totalWorkedHours) {
        this.task = task;
        this.totalExpectedHours = totalExpectedHours;
        this.totalWorkedHours = totalWorkedHours;
    }

    public Task getTask() {
        return task;
    }

    public int getTotalExpectedHours() {
        return totalExpectedHours;
    }

    public int getTotalWorkedHours() {
        return totalWorkedHours;
    }
}
