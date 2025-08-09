package com.airepublic.bmstoinverter.webserver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.service.IWebServerService;
import com.google.gson.Gson;

public class WebServer implements IWebServerService {
    private static Logger LOG = LoggerFactory.getLogger(WebServer.class);
    private Server server;
    private final String alarmMessages;

    public WebServer() {
        final ResourceBundle bundle = ResourceBundle.getBundle("alarms");
        final Map<String, String> map = new LinkedHashMap<>();

        for (final String key : bundle.keySet()) {
            map.put(key, bundle.getString(key));
        }

        alarmMessages = new Gson().toJson(map);

    }


    @Override
    public void start(final int httpPort, final int httpsPort, final EnergyStorage energyStorage) {
        server = new Server();

        // Setup HTTP Configuration with security settings
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecurePort(httpsPort);
        httpConfig.setSecureScheme("https");
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(false);

        // Setup HTTP Connector
        final ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        httpConnector.setPort(httpPort);
        server.addConnector(httpConnector);

        // Setup SSL Configuration
        final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(findKeyStore());
        sslContextFactory.setKeyStorePassword("changeit");
        sslContextFactory.setKeyManagerPassword("changeit");
        sslContextFactory.setWantClientAuth(true);
        sslContextFactory.setNeedClientAuth(false);

        // Setup HTTPS Configuration
        final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        // Setup HTTPS Connector
        final ServerConnector httpsConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(httpsConfig));
        httpsConnector.setPort(httpsPort);
        server.addConnector(httpsConnector);

        // Create a proper session handler first
        final SessionHandler sessionHandler = new SessionHandler();
        sessionHandler.setSessionCookie("JSESSIONID");
        sessionHandler.setSessionIdPathParameterName("none"); // Don't use URL rewriting
        sessionHandler.setHttpOnly(true); // Better security
        sessionHandler.setSecureRequestOnly(true); // For HTTPS
        
        // Create the main handlers
        HandlerList handlers = new HandlerList();

        // Add SecuredRedirectHandler first
        handlers.addHandler(new org.eclipse.jetty.server.handler.SecuredRedirectHandler());

        // ResourceHandler for static files
        final ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(Resource.newClassPathResource("/static/"));
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[] { "index.html" });

        // API Handler
        AbstractHandler apiHandler = new AbstractHandler() {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, 
                    javax.servlet.http.HttpServletRequest request,
                    javax.servlet.http.HttpServletResponse response) throws java.io.IOException, javax.servlet.ServletException {
                final String path = request.getRequestURI();
                if (path.equals("/favicon.ico")) {
                    Resource favicon = Resource.newClassPathResource("static/favicon.ico");
                    if (favicon != null && favicon.exists()) {
                        response.setContentType("image/x-icon");
                        org.eclipse.jetty.util.IO.copy(favicon.getInputStream(), response.getOutputStream());
                        baseRequest.setHandled(true);
                    }
                } else if (path.contains("/data")) {
                    String content = energyStorage.toJson();
                    response.setContentType("application/json; charset=utf-8");
                    response.setHeader("Access-Control-Allow-Origin", "http://localhost, https://localhost");
                    response.getWriter().write(content);
                    baseRequest.setHandled(true);
                } else if (path.contains("/alarmMessages")) {
                    response.setContentType("application/json; charset=utf-8");
                    response.setHeader("Access-Control-Allow-Origin", "http://localhost, https://localhost");
                    response.getWriter().write(alarmMessages);
                    baseRequest.setHandled(true);
                }
            }
        };

        // Create content handlers
        HandlerList contentHandlers = new HandlerList();
        contentHandlers.addHandler(resourceHandler);
        contentHandlers.addHandler(apiHandler);

        final String username = System.getProperty("webserver.username", "");
        final String password = System.getProperty("webserver.password", "");

        if (!username.trim().isEmpty() && !password.trim().isEmpty()) {
            // Set up security
            final SecurityHandler securityHandler = createSecurityHandler(username, password);
            securityHandler.setHandler(contentHandlers);
            handlers.addHandler(securityHandler);
        } else {
            handlers.addHandler(contentHandlers);
        }

        // Set the session handler as the top-level handler
        sessionHandler.setHandler(handlers);
        server.setHandler(sessionHandler);

        LOG.info("Starting webserver on ports " + httpPort + ":" + httpsPort);
        try {
            server.start();
            LOG.info("Started webserver on ports " + httpPort + ":" + httpsPort + " successfully!");
        } catch (final Exception e) {
            LOG.error("FAILED to start webserver on ports " + httpPort + ":" + httpsPort + "!", e);
        }
    }

    private static SecurityHandler createSecurityHandler(final String username, final String password) {
        // Create a UserStore and add the user
        final UserStore userStore = new UserStore();
        userStore.addUser(username, Credential.getCredential(password), new String[] { "user" });

        // Create a LoginService
        final HashLoginService loginService = new HashLoginService();
        loginService.setName("MyRealm");
        loginService.setUserStore(userStore);

        // Create security handler
        final ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        security.setLoginService(loginService);

        // Setup form authentication
        FormAuthenticator formAuthenticator = new FormAuthenticator("/login.html", "/login.html?error=true", false);
        security.setAuthenticator(formAuthenticator);
        
        // Configure the security constraint
        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[] { "user" });

        // Create mappings for different URL patterns
        ConstraintMapping cmLogin = new ConstraintMapping();
        cmLogin.setPathSpec("/login.html");
        cmLogin.setConstraint(constraint);
        constraint.setAuthenticate(false);

        ConstraintMapping cmStatic = new ConstraintMapping();
        cmStatic.setPathSpec("/styles.css");
        cmStatic.setConstraint(constraint);
        constraint.setAuthenticate(false);

        constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[] { "user" });

        ConstraintMapping cmDefault = new ConstraintMapping();
        cmDefault.setPathSpec("/*");
        cmDefault.setConstraint(constraint);

        security.setConstraintMappings(java.util.Arrays.asList(cmLogin, cmStatic, cmDefault));
        
        return security;
    }


    private String findKeyStore() {
        try {
            // Create a temporary file from the keystore resource
            java.io.InputStream keystoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("ssl/keystore.jks");
            if (keystoreStream == null) {
                throw new RuntimeException("Unable to read keystore.jks from resources");
            }

            java.io.File tempFile = java.io.File.createTempFile("keystore", ".jks");
            tempFile.deleteOnExit();

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = keystoreStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            return tempFile.getAbsolutePath();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to create temporary keystore file", e);
        }
    }


    @Override
    public void stop() {
        try {
            server.stop();
        } catch (final Exception e) {
            LOG.error("Errors stopping webserver: ", e);
        }
    }


    public static void main(final String[] args) throws Exception {
        System.setProperty("webserver.username", "username");
        System.setProperty("webserver.password", "password");
        final EnergyStorage energyStorage = new EnergyStorage();
        energyStorage.getBatteryPacks().add(new BatteryPack());
        energyStorage.getBatteryPacks().add(new BatteryPack());
        energyStorage.getBatteryPacks().add(new BatteryPack());
        energyStorage.getBatteryPacks().add(new BatteryPack());
        energyStorage.getBatteryPacks().add(new BatteryPack());
        new WebServer().start(8080, 8443, energyStorage);
    }
}
