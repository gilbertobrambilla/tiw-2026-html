package it.polimi.tiw.beans;

import java.util.List;

public class WPWithTasks {
    private final WP wp;
    private final List<Task> tasks;

    public WPWithTasks(WP wp, List<Task> tasks) {
        this.wp = wp;
        this.tasks = tasks;
    }

    public WP getWp() {
        return wp;
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
