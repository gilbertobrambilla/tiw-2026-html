package it.polimi.tiw.controllers.technical.manager;

import it.polimi.tiw.beans.*;
import it.polimi.tiw.dao.*;
import it.polimi.tiw.utils.DatabaseConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet used for processing task hours and collaborator assignments.
 */
@WebServlet("/technical/manager/save-assignment")
public class SaveAssignment extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // session parameters are extracted
        HttpSession session = request.getSession();
        UserLogin loggedInUser = (UserLogin) session.getAttribute("user");

        // request parameters are parsed
        String taskIdParam = request.getParameter("taskId");
        String action = request.getParameter("action");

        // syntax checks for identifiers are performed
        if (taskIdParam == null || taskIdParam.trim().isEmpty() || action == null || action.trim().isEmpty()) {
            request.setAttribute("managerHomeGlobalError", "Missing mandatory parameters");
            request.getRequestDispatcher("/technical/manager/home").forward(request, response);
            return;
        }

        int taskId;
        try {
            taskId = Integer.parseInt(taskIdParam);
        } catch (NumberFormatException e) {
            request.setAttribute("managerHomeGlobalError", "Invalid task parameter");
            request.getRequestDispatcher("/technical/manager/home").forward(request, response);
            return;
        }

        // database connection is established
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {
            connection.setAutoCommit(false);

            try {
                TaskDAO taskDAO = new TaskDAO(connection);
                ProjectDAO projectDAO = new ProjectDAO(connection);
                TaskHoursDAO taskHoursDAO = new TaskHoursDAO(connection);
                UserDAO userDAO = new UserDAO(connection);

                // task ownership and authorization are verified
                Task task = taskDAO.getTaskByIdAndManager(taskId, loggedInUser.getId());
                if (task == null) {
                    connection.rollback();
                    request.setAttribute("managerHomeGlobalError", "Task not found or authorization denied");
                    request.getRequestDispatcher("/technical/manager/home").forward(request, response);
                    return;
                }

                int projectId = taskDAO.getProjectIdByTask(taskId);

                // project ownership and authorization are verified
                Project project = projectDAO.getProjectByIdAndManager(projectId, loggedInUser.getId());
                if (project == null) {
                    connection.rollback();
                    request.setAttribute("managerHomeGlobalError", "Project not found or authorization denied");
                    request.getRequestDispatcher("/technical/manager/home").forward(request, response);
                    return;
                }

                // project status is verified
                if (project.getStatus() != ProjectStatus.created) {
                    connection.rollback();
                    request.setAttribute("managerHomeGlobalError", "Assignments can only be modified for projects in created status");
                    request.getRequestDispatcher("/technical/manager/home").forward(request, response);
                    return;
                }

                List<String> formErrors = new ArrayList<>();
                Map<Integer, Integer> hoursValues = new HashMap<>();

                // task hours validation is performed
                for (int m = task.getStartMonth(); m <= task.getEndMonth(); m++) {
                    String hoursParam = request.getParameter("hours_" + m);

                    if (hoursParam == null || hoursParam.trim().isEmpty()) {
                        formErrors.add("hours field is required for month M" + m);
                    } else {
                        try {
                            int hours = Integer.parseInt(hoursParam);

                            if (hours < 0) {
                                formErrors.add("hours must be non-negative for month M" + m);
                            } else {
                                hoursValues.put(m, hours);
                            }
                        } catch (NumberFormatException e) {
                            formErrors.add("invalid number format for month M" + m);
                        }
                    }
                }

                String[] collaboratorIdsParams = request.getParameterValues("collaboratorIds");
                List<Integer> selectedCollaboratorIds = new ArrayList<>();

                // collaborator assignments validation is performed
                if (collaboratorIdsParams != null) {
                    for (String idParam : collaboratorIdsParams) {
                        try {
                            int collaboratorId = Integer.parseInt(idParam);

                            UserRole collaboratorRole = userDAO.getRoleByUser(collaboratorId);
                            if (collaboratorRole != UserRole.technical || collaboratorId == loggedInUser.getId()) {
                                connection.rollback();
                                request.setAttribute("managerHomeGlobalError", "Invalid collaborator selection");
                                request.getRequestDispatcher("/technical/manager/home").forward(request, response);
                                return;
                            }

                            if (!selectedCollaboratorIds.contains(collaboratorId)) {
                                selectedCollaboratorIds.add(collaboratorId);
                            }
                        } catch (NumberFormatException e) {
                            connection.rollback();
                            request.setAttribute("managerHomeGlobalError", "Invalid collaborator parameter");
                            request.getRequestDispatcher("/technical/manager/home").forward(request, response);
                            return;
                        }
                    }
                }

                if (selectedCollaboratorIds.isEmpty()) {
                    formErrors.add("at least one collaborator must be assigned");
                }

                // request is forwarded on form errors detection
                if (!formErrors.isEmpty()) {
                    connection.rollback();
                    request.setAttribute("formErrors", formErrors);
                    request.setAttribute("hoursValues", hoursValues);
                    request.setAttribute("selectedCollaboratorIdsValue", selectedCollaboratorIds);
                    request.getRequestDispatcher("/technical/manager/home").forward(request, response);
                    return;
                }

                // expected hours insertion is performed
                for (Map.Entry<Integer, Integer> entry : hoursValues.entrySet()) {
                    taskHoursDAO.saveExpectedHours(taskId, entry.getKey(), entry.getValue());
                }

                // task workers assignments are stored
                taskDAO.deleteWorkersByTask(taskId);
                for (int collaboratorId : selectedCollaboratorIds) {
                    taskDAO.addWorkerToTask(taskId, collaboratorId);
                }

                // project state transition logic is evaluated
                if ("assign".equals(action)) {
                    if (projectDAO.isProjectAssignable(projectId)) {
                        projectDAO.updateProjectStatus(projectId, ProjectStatus.assigned);
                        connection.commit();

                        response.sendRedirect(request.getContextPath() + "/technical/manager/home");
                    } else {
                        connection.rollback();

                        formErrors.add("project allocation rules are incomplete for assignment");
                        request.setAttribute("formErrors", formErrors);
                        request.setAttribute("hoursValues", hoursValues);
                        request.setAttribute("selectedCollaboratorIdsValue", selectedCollaboratorIds);
                        request.getRequestDispatcher("/technical/manager/home").forward(request, response);
                    }
                    return;
                }

                connection.commit();

                // successful redirect is performed
                response.sendRedirect(request.getContextPath() + "/technical/manager/home?taskId=" + taskId);

            } catch (SQLException e) {
                connection.rollback();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failure");
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failure");
        }
    }
}
