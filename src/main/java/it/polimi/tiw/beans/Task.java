package it.polimi.tiw.beans;

public class Task {
    private final int id;
    private final int wpId;
    private final int sequenceNumber;
    private final String title;
    private final String description;
    private final int startMonth;
    private final int endMonth;

    public Task(int id, int wpId, int sequenceNumber, String title, String description, int startMonth, int endMonth) {
        this.id = id;
        this.wpId = wpId;
        this.sequenceNumber = sequenceNumber;
        this.title = title;
        this.description = description;
        this.startMonth = startMonth;
        this.endMonth = endMonth;
    }

    public int getId() {
        return id;
    }

    public int getWpId() {
        return wpId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getStartMonth() {
        return startMonth;
    }

    public int getEndMonth() {
        return endMonth;
    }
}
