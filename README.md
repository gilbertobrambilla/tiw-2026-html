<h1 align="center">
    TIW 2026 - HTML Version
    <br>
</h1>
<h4 align="center">Project 1: Gestione e rendicontazione progetti</h4>
<h4 align="center">Web Technologies Project at Politecnico di Milano, A.Y. 2025-2026</h4>
<p align="center">
    <strong>Final Grade: 30/30 cum laude</strong>
</p>

## About the project

Java web application for project management and worked-hour reporting, developed for Project 1 of the Web Technologies course.

This version follows the HTML/server-side rendering specification: servlets handle the application flow and generate dynamic pages through Thymeleaf templates.

## Documentation

Project documentation is available under the `documentation` directory:

- [Project requirements](documentation/TIW2025-26.pdf)
- [Implementation documentation](documentation/TIW_documentazione.pptx)

## Requirements

- Java JDK 21
- Maven
- MySQL server
- Jakarta Servlet compatible application server

Before deploying the application, import the database schema:

```bash
mysql -u root -p < src/main/resources/tiw-html.sql
```

If needed, update the database connection parameters in:

```text
src/main/webapp/WEB-INF/web.xml
```

## Build

From the project root, run:

```bash
./mvnw clean package
```

On Windows:

```bash
mvnw.cmd clean package
```

The build generates the deployable WAR under the `target` directory.

## How to Run

Deploy the generated WAR file to a Jakarta Servlet compatible application server, then open the deployed web application in the browser.

## Tools and Technologies

- Java 21
- Jakarta Servlet
- Thymeleaf
- MySQL
- Maven
- HTML
- CSS

## Other Version

The RIA version of this project is available [here](https://github.com/gilbertobrambilla/tiw-2026-ria).
