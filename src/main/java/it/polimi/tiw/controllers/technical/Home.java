package it.polimi.tiw.controllers.technical;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;

/**
 * Controller used for the technical user home page rendering.
 */
@WebServlet("/technical/home")
public class Home extends HttpServlet {
    private TemplateEngine templateEngine;
    private static final String TEMPLATE_PATH = "/WEB-INF/technical/home.html";

    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();

        // thymeleaf web application and template resolver are initialized
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);

        // template engine is configured for html processing
        templateResolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
        templateResolver.setSuffix(".html");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // thymeleaf web application context is initialized
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());

        // response content type is set and template is processed
        response.setContentType("text/html;charset=UTF-8");
        templateEngine.process(TEMPLATE_PATH, ctx, response.getWriter());
    }
}
