package it.polimi.tiw.dao;

import it.polimi.tiw.beans.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ProjectReportDAO {
    private final Connection connection;

    public ProjectReportDAO(Connection connection) {
        this.connection = connection;
    }

    public Map<WP, List<TaskWithHours>> getProjectReportByProjectAndCreator(int projectId, int creatorId) throws SQLException {
        Map<WP, List<TaskWithHours>> report = new LinkedHashMap<>();

        String query = "SELECT v.* " +
                "FROM project_wp_tasks_summary_view v " +
                "JOIN projects p ON v.project_id = p.id " +
                "WHERE v.project_id = ? AND p.created_by = ? " +
                "ORDER BY v.wp_sequence ASC, v.task_sequence ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, projectId);
            stmt.setInt(2, creatorId);

            try (ResultSet result = stmt.executeQuery()) {
                WP currentWp = null;
                List<TaskWithHours> currentTaskList = null;

                while (result.next()) {
                    int wpId = result.getInt("wp_id");

                    if (currentWp == null || currentWp.getId() != wpId) {
                        currentWp = new WP(
                                wpId,
                                projectId,
                                result.getInt("wp_sequence"),
                                result.getString("wp_title"),
                                result.getInt("wp_start_month"),
                                result.getInt("wp_end_month")
                        );
                        currentTaskList = new ArrayList<>();
                        report.put(currentWp, currentTaskList);
                    }

                    int taskId = result.getInt("task_id");
                    if (result.wasNull()) {
                        continue;
                    }

                    Task task = new Task(
                            taskId,
                            wpId,
                            result.getInt("task_sequence"),
                            result.getString("task_title"),
                            result.getString("task_description"),
                            result.getInt("start_month"),
                            result.getInt("end_month")
                    );

                    TaskWithHours taskWithHours = new TaskWithHours(
                            task,
                            result.getInt("total_expected_hours"),
                            result.getInt("total_worked_hours")
                    );

                    currentTaskList.add(taskWithHours);
                }
            }
        }

        return report;
    }

    public ProjectMonthlyReport getProjectMonthlyReport(Project project, boolean closable) throws SQLException {
        List<WPMonthlyReport> workPackages = new ArrayList<>();
        String query = "SELECT * FROM project_monthly_report_view WHERE project_id = ? ORDER BY wp_sequence ASC, task_sequence ASC, month_index ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, project.getId());

            try (ResultSet result = stmt.executeQuery()) {
                WPMonthlyReport currentWpData = null;
                Task currentTask = null;
                TaskMonthlyReport currentTaskData = null;
                List<TaskMonthlyReport> currentTaskList = null;

                while (result.next()) {
                    int wpId = result.getInt("wp_id");
                    int taskId = result.getInt("task_id");

                    // wp data is instantiated when id changes
                    if (currentWpData == null || currentWpData.getWp().getId() != wpId) {
                        WP wp = new WP(
                                wpId,
                                project.getId(),
                                result.getInt("wp_sequence"),
                                result.getString("wp_title"),
                                result.getInt("wp_start_month"),
                                result.getInt("wp_end_month")
                        );
                        currentTaskList = new ArrayList<>();
                        currentWpData = new WPMonthlyReport(wp, new LinkedHashMap<>(), new LinkedHashMap<>(), currentTaskList);
                        workPackages.add(currentWpData);
                    }

                    // task is instantiated when id changes
                    if (currentTask == null || currentTask.getId() != taskId) {
                        currentTask = new Task(
                                taskId,
                                wpId,
                                result.getInt("task_sequence"),
                                result.getString("task_title"),
                                result.getString("task_description"),
                                result.getInt("task_start_month"),
                                result.getInt("task_end_month")
                        );
                        currentTaskData = new TaskMonthlyReport(
                                currentTask,
                                createEmptyExpectedHoursMap(currentTask.getStartMonth(), currentTask.getEndMonth()),
                                new LinkedHashMap<>()
                        );
                        if (currentTaskList != null) {
                            currentTaskList.add(currentTaskData);
                        }
                    }

                    // monthly data is mapped and aggregated
                    int monthIndex = result.getInt("month_index");
                    if (!result.wasNull() && currentTaskData != null) {
                        int expectedValue = result.getInt("expected_hours");
                        Integer expected = result.wasNull() ? null : expectedValue;
                        int worked = result.getInt("worked_hours");

                        currentTaskData.getExpectedHoursByMonth().put(monthIndex, expected);
                        currentTaskData.getWorkedHoursByMonth().put(monthIndex, worked);

                        if (expected != null) {
                            currentWpData.getTotalExpectedHoursByMonth().merge(monthIndex, expected, Integer::sum);
                        }
                        currentWpData.getTotalWorkedHoursByMonth().merge(monthIndex, worked, Integer::sum);
                    }
                }
            }
        }
        return new ProjectMonthlyReport(project, workPackages, closable);
    }

    public List<ProjectMonthlyReport> getCollaboratorMonthlyReportByManagerAndCollaborator(int managerId, int collaboratorId) throws SQLException {
        List<ProjectMonthlyReport> reportList = new ArrayList<>();
        String query = "SELECT * FROM collaborator_monthly_report_view WHERE manager_id = ? AND collaborator_id = ? ORDER BY project_id DESC, wp_sequence ASC, task_sequence ASC, month_index ASC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, managerId);
            stmt.setInt(2, collaboratorId);

            try (ResultSet result = stmt.executeQuery()) {
                Project currentProject = null;
                List<WPMonthlyReport> currentWpList = null;
                WPMonthlyReport currentWpData = null;
                Task currentTask = null;
                TaskMonthlyReport currentTaskData = null;
                List<TaskMonthlyReport> currentTaskList = null;

                while (result.next()) {
                    int projectId = result.getInt("project_id");
                    int wpId = result.getInt("wp_id");
                    int taskId = result.getInt("task_id");

                    // project is instantiated when id changes
                    if (currentProject == null || currentProject.getId() != projectId) {
                        currentProject = new Project(
                                projectId,
                                result.getString("project_title"),
                                result.getInt("project_duration"),
                                ProjectStatus.valueOf(result.getString("project_status")),
                                result.getInt("project_created_by"),
                                managerId
                        );
                        currentWpList = new ArrayList<>();
                        // closable is set to false as it is not needed for collaborator view
                        reportList.add(new ProjectMonthlyReport(currentProject, currentWpList, false));
                    }

                    // wp data is instantiated when id changes
                    if (currentWpData == null || currentWpData.getWp().getId() != wpId) {
                        WP wp = new WP(
                                wpId,
                                projectId,
                                result.getInt("wp_sequence"),
                                result.getString("wp_title"),
                                0,
                                0
                        );
                        currentTaskList = new ArrayList<>();
                        currentWpData = new WPMonthlyReport(wp, new HashMap<>(), new HashMap<>(), currentTaskList);
                        if (currentWpList != null) {
                            currentWpList.add(currentWpData);
                        }
                    }

                    // task is instantiated when id changes
                    if (currentTask == null || currentTask.getId() != taskId) {
                        currentTask = new Task(
                                taskId,
                                wpId,
                                result.getInt("task_sequence"),
                                result.getString("task_title"),
                                result.getString("task_description"),
                                result.getInt("task_start_month"),
                                result.getInt("task_end_month")
                        );
                        currentTaskData = new TaskMonthlyReport(currentTask, new HashMap<>(), new HashMap<>());
                        if (currentTaskList != null) {
                            currentTaskList.add(currentTaskData);
                        }
                    }

                    // monthly worked hours are mapped and aggregated
                    int monthIndex = result.getInt("month_index");
                    if (!result.wasNull() && currentTaskData != null) {
                        int worked = result.getInt("worked_hours");
                        currentTaskData.getWorkedHoursByMonth().put(monthIndex, worked);
                        currentWpData.getTotalWorkedHoursByMonth().merge(monthIndex, worked, Integer::sum);
                    }
                }
            }
        }
        return reportList;
    }

    private Map<Integer, Integer> createEmptyExpectedHoursMap(int startMonth, int endMonth) {
        Map<Integer, Integer> expectedHours = new LinkedHashMap<>();
        for (int month = startMonth; month <= endMonth; month++) {
            expectedHours.put(month, null);
        }
        return expectedHours;
    }
}
