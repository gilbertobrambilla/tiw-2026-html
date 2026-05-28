package it.polimi.tiw.beans;

import java.util.Map;

public class TaskMonthlyReport {
    private final Task task;
    private final Map<Integer, Integer> expectedHoursByMonth;
    private final Map<Integer, Integer> workedHoursByMonth;

    public TaskMonthlyReport(Task task, Map<Integer, Integer> expectedHoursByMonth, Map<Integer, Integer> workedHoursByMonth) {
        // collections and objects are assigned to final fields
        this.task = task;
        this.expectedHoursByMonth = expectedHoursByMonth;
        this.workedHoursByMonth = workedHoursByMonth;
    }

    public Task getTask() {
        return task;
    }

    public Map<Integer, Integer> getExpectedHoursByMonth() {
        return expectedHoursByMonth;
    }

    public Map<Integer, Integer> getWorkedHoursByMonth() {
        return workedHoursByMonth;
    }
}
