package it.polimi.tiw.beans;

public class WP {
    private final int id;
    private final int projectId;
    private final int sequenceNumber;
    private final String title;
    private final int startMonth;
    private final int endMonth;

    public WP(int id, int projectId, int sequenceNumber, String title, int startMonth, int endMonth) {
        this.id = id;
        this.projectId = projectId;
        this.sequenceNumber = sequenceNumber;
        this.title = title;
        this.startMonth = startMonth;
        this.endMonth = endMonth;
    }

    public int getId() {
        return id;
    }

    public int getProjectId() {
        return projectId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getTitle() {
        return title;
    }

    public int getStartMonth() {
        return startMonth;
    }

    public int getEndMonth() {
        return endMonth;
    }
}
