package it.polimi.tiw.controllers.admin;

import it.polimi.tiw.beans.Project;
import it.polimi.tiw.beans.ProjectStatus;
import it.polimi.tiw.beans.UserLogin;
import it.polimi.tiw.dao.ProjectDAO;
import it.polimi.tiw.dao.WPDAO;
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
import java.util.List;

@WebServlet("/admin/create-wp")
public class CreateWP extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String projectIdStr = request.getParameter("projectId");
        String title = request.getParameter("title");
        String startMonthStr = request.getParameter("startMonth");
        String endMonthStr = request.getParameter("endMonth");

        List<String> wpFormErrors = new ArrayList<>();

        // title validation is performed
        if (title == null || title.trim().isEmpty()) {
            wpFormErrors.add("Title cannot be empty");
        } else if (title.length() > 100) {
            wpFormErrors.add("Title cannot exceed 100 characters");
        }

        // project validation is performed
        int projectId = 0;
        if (projectIdStr == null || projectIdStr.trim().isEmpty()) {
            wpFormErrors.add("Project must be selected");
        } else {
            try {
                projectId = Integer.parseInt(projectIdStr);
            } catch (NumberFormatException e) {
                wpFormErrors.add("Invalid project identification");
            }
        }

        // duration validation is performed
        int startMonth = 0;
        int endMonth = 0;
        try {
            startMonth = Integer.parseInt(startMonthStr);
            endMonth = Integer.parseInt(endMonthStr);
            if (startMonth <= 0 || endMonth <= 0) {
                wpFormErrors.add("Months must be positive");
            } else if (startMonth > endMonth) {
                wpFormErrors.add("End month cannot precede start month");
            }
        } catch (NumberFormatException e) {
            wpFormErrors.add("Valid numeric months are required");
        }

        // session parameters are extracted
        HttpSession session = request.getSession();
        UserLogin loggedInUser = (UserLogin) session.getAttribute("user");

        // transactional database operation is executed
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {

            connection.setAutoCommit(false);

            try {
                ProjectDAO projectDAO = new ProjectDAO(connection);
                WPDAO wpDAO = new WPDAO(connection);

                if (wpFormErrors.isEmpty()) {
                    Project project = projectDAO.getProjectByIdAndCreator(projectId, loggedInUser.getId());
                    if (project == null) {
                        wpFormErrors.add("Project not found or authorization denied");
                    } else if (project.getStatus() != ProjectStatus.created) {
                        wpFormErrors.add("WP can only be added to projects in 'created' status");
                    } else if (endMonth > project.getDurationMonths()) {
                        wpFormErrors.add("Duration exceeds project length (Max: " + project.getDurationMonths() + ")");
                    }
                }

                // request is forwarded on error detection
                if (!wpFormErrors.isEmpty()) {
                    connection.rollback();
                    request.setAttribute("wpFormErrors", wpFormErrors);
                    request.setAttribute("wpProjectIdValue", projectIdStr);
                    request.setAttribute("wpTitleValue", title);
                    request.setAttribute("wpStartMonthValue", startMonthStr);
                    request.setAttribute("wpEndMonthValue", endMonthStr);

                    request.getRequestDispatcher("/admin/home").forward(request, response);
                    return;
                }

                wpDAO.createWP(projectId, title, startMonth, endMonth);
                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred during creation");
                return;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failure");
            return;
        }

        // redirect is performed on success
        response.sendRedirect(getServletContext().getContextPath() + "/admin/home");
    }
}
