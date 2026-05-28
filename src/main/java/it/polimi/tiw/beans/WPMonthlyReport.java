package it.polimi.tiw.beans;

import java.util.List;
import java.util.Map;

public class WPMonthlyReport {
    private final WP wp;
    private final Map<Integer, Integer> totalExpectedHoursByMonth;
    private final Map<Integer, Integer> totalWorkedHoursByMonth;
    private final List<TaskMonthlyReport> tasks;

    public WPMonthlyReport(WP wp, Map<Integer, Integer> totalExpectedHoursByMonth, Map<Integer, Integer> totalWorkedHoursByMonth, List<TaskMonthlyReport> tasks) {
        this.wp = wp;
        this.totalExpectedHoursByMonth = totalExpectedHoursByMonth;
        this.totalWorkedHoursByMonth = totalWorkedHoursByMonth;
        this.tasks = tasks;
    }

    public WP getWp() {
        return wp;
    }

    public Map<Integer, Integer> getTotalExpectedHoursByMonth() {
        return totalExpectedHoursByMonth;
    }

    public Map<Integer, Integer> getTotalWorkedHoursByMonth() {
        return totalWorkedHoursByMonth;
    }

    public List<TaskMonthlyReport> getTasks() {
        return tasks;
    }
}
