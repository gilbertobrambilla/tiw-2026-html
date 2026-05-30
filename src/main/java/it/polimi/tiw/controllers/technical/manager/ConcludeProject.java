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
import java.util.List;

@WebServlet("/technical/manager/conclude-project")
public class ConcludeProject extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // session parameters are extracted
        HttpSession session = request.getSession();
        UserLogin loggedInUser = (UserLogin) session.getAttribute("user");

        // request parameters are parsed
        String projectIdParam = request.getParameter("projectId");

        // syntax checks for identifiers are performed
        if (projectIdParam == null || projectIdParam.trim().isEmpty()) {
            request.setAttribute("monitorProjectsGlobalError", "Missing project parameter");
            request.getRequestDispatcher("/technical/manager/monitor-projects").forward(request, response);
            return;
        }

        int projectId;
        try {
            projectId = Integer.parseInt(projectIdParam);
        } catch (NumberFormatException e) {
            request.setAttribute("monitorProjectsGlobalError", "Invalid project parameter");
            request.getRequestDispatcher("/technical/manager/monitor-projects").forward(request, response);
            return;
        }

        // database connection is established
        try (Connection connection = DatabaseConnection.getInstance(getServletContext()).getConnection()) {
            ProjectDAO projectDAO = new ProjectDAO(connection);

            // project ownership and authorization are verified
            Project project = projectDAO.getProjectByIdAndManager(projectId, loggedInUser.getId());
            if (project == null) {
                request.setAttribute("monitorProjectsGlobalError", "Project not found or authorization denied");
                request.getRequestDispatcher("/technical/manager/monitor-projects").forward(request, response);
                return;
            }

            // project status is verified
            if (project.getStatus() != ProjectStatus.assigned) {
                request.setAttribute("monitorProjectsGlobalError", "Only assigned projects can be concluded");
                request.getRequestDispatcher("/technical/manager/monitor-projects").forward(request, response);
                return;
            }

            // project-closable condition is verified
            if (projectDAO.isProjectClosable(projectId)) {
                projectDAO.updateProjectStatus(projectId, ProjectStatus.completed);

                response.sendRedirect(request.getContextPath() + "/technical/manager/monitor-projects?projectId=" + projectId);
                return;
            }

            List<String> formErrors = new ArrayList<>();
            formErrors.add("Project cannot be concluded yet due to missing worked hours");

            request.setAttribute("formErrors", formErrors);
            request.getRequestDispatcher("/technical/manager/monitor-projects").forward(request, response);

        } catch (SQLException e) {
            // database error is handled
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failure");
        }
    }
}
