package com.airepublic.bmstoinverter.webserver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.session.SessionHandler;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.Resources;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.service.IWebServerService;
import com.airepublic.bmstoinverter.core.util.InputStreamUtil;
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

        // Setup HTTP Connector
        final HttpConfiguration httpConf = new HttpConfiguration();
        httpConf.setSecurePort(httpsPort);
        httpConf.setSecureScheme("https");

        final ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConf));
        connector.setPort(httpPort);

        server.addConnector(connector);

        // Setup SSL
        final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreResource(findKeyStore(ResourceFactory.of(server)));
        sslContextFactory.setKeyStorePassword("changeit");
        sslContextFactory.setKeyManagerPassword("changeit");
        sslContextFactory.setSniRequired(false);
        sslContextFactory.setWantClientAuth(true); // Turn on
        // javax.net.ssl.SSLEngine.wantClientAuth
        sslContextFactory.setNeedClientAuth(false); // Turn on
        // javax.net.ssl.SSLEngine.needClientAuth

        // Setup HTTPS Configuration
        final HttpConfiguration httpsConf = new HttpConfiguration();
        httpsConf.setSecureScheme("https");
        httpsConf.setSecurePort(httpsPort);
        final SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        customizer.setSniHostCheck(false);
        httpsConf.addCustomizer(customizer);

        // Establish the HTTPS ServerConnector
        final ServerConnector httpsConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(httpsConf));
        httpsConnector.setPort(httpsPort);

        server.addConnector(httpsConnector);

        // Add a Handlers for requests
        final Handler.Sequence handlers = new Handler.Sequence();
        Handler finalHandler = handlers;

        final SessionHandler sessionHandler = new SessionHandler();

        final String username = System.getProperty("webserver.username", "");
        final String password = System.getProperty("webserver.password", "");

        if (!username.trim().isEmpty() && !password.trim().isEmpty()) {
            // Set up security and wrap all other handlers with security
            final SecurityHandler securityHandler = createSecurityHandler(username, password);
            securityHandler.setHandler(handlers);
            finalHandler = securityHandler;
        }

        handlers.addHandler(new SecuredRedirectHandler());

        final ResourceFactory resourceFactory = ResourceFactory.of(server);
        final Resource rootResourceDir = resourceFactory.newClassLoaderResource("/static/");

        if (!Resources.isReadableDirectory(rootResourceDir)) {
            LOG.error("Unable to find /static/ classloader directory!");
            stop();
            return;
        }

        final ResourceHandler rootResourceHandler = new ResourceHandler();
        rootResourceHandler.setBaseResource(rootResourceDir);
        rootResourceHandler.setDirAllowed(false);
        rootResourceHandler.setWelcomeFiles("index.html");

        handlers.addHandler(rootResourceHandler);

        handlers.addHandler(new Handler.Abstract() {
            @Override
            public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
                final String path = request.getHttpURI().getPath();

                // if (path.trim().isEmpty() || path.equals("/") || path.equals("/index.html")) {
                // final Path indexHtml =
                // ResourceFactory.of(server).newClassLoaderResource("static/index.html").getPath();
                // final String content = Files.readString(indexHtml, StandardCharsets.UTF_8);
                // response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=utf-8");
                // response.getHeaders().put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN,
                // "http://localhost, https://localhost");
                // response.write(true, BufferUtil.toBuffer(content, StandardCharsets.UTF_8),
                // callback);
                // return true;
                // } else if (path.contains("/login.html")) {
                // final Path indexHtml =
                // ResourceFactory.of(server).newClassLoaderResource("static/login.html").getPath();
                // final String content = Files.readString(indexHtml, StandardCharsets.UTF_8);
                // response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=utf-8");
                // response.getHeaders().put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN,
                // "http://localhost, https://localhost");
                // response.write(true, BufferUtil.toBuffer(content, StandardCharsets.UTF_8),
                // callback);
                // return true;
                // } else if (path.contains("/styles.css")) {
                // final Path indexHtml =
                // ResourceFactory.of(server).newClassLoaderResource("static/styles.css").getPath();
                // final String content = Files.readString(indexHtml, StandardCharsets.UTF_8);
                // response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/css; charset=utf-8");
                // response.getHeaders().put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN,
                // "http://localhost, https://localhost");
                // response.write(true, BufferUtil.toBuffer(content, StandardCharsets.UTF_8),
                // callback);
                // return true;
                // } else if (path.contains("/favicon.ico")) {
                // return true;
                // } else
                if (path.equals("/favicon.ico")) {
                    final Resource favicon = ResourceFactory.of(server).newClassLoaderResource("static/favicon.ico");
                    if (favicon.exists()) {
                        try (InputStream is = favicon.newInputStream()) {
                            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "image/x-icon");
                            response.write(true, BufferUtil.toBuffer(InputStreamUtil.readAllBytes(is)), callback);
                        }
                        return true;
                    }
                } else if (path.contains("/data")) {
                    final String content = energyStorage.toJson();
                    response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/json; charset=utf-8");
                    response.getHeaders().put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost, https://localhost");
                    response.write(true, BufferUtil.toBuffer(content, StandardCharsets.UTF_8), callback);
                    return true;
                } else if (path.contains("/alarmMessages")) {
                    response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/json; charset=utf-8");
                    response.getHeaders().put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost, https://localhost");
                    response.write(true, BufferUtil.toBuffer(alarmMessages, StandardCharsets.UTF_8),
                            callback);
                    return true;
                }

                return false;
            }
        });

        sessionHandler.setHandler(finalHandler);
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
        // Create a UserStore
        final UserStore userStore = new UserStore();
        userStore.addUser(username, Credential.getCredential(password), new String[] { "user" });

        // Create a LoginService and associate it with the UserStore
        final HashLoginService loginService = new HashLoginService();
        loginService.setName("MyRealm");
        loginService.setUserStore(userStore);

        // Create a ConstraintSecurityHandler and set authenticator
        final SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();
        // Set up constraint mapping for all paths
        securityHandler.put("/*", Constraint.from("user"));
        securityHandler.put("/login.html", Constraint.ALLOWED_ANY_TRANSPORT);
        securityHandler.put("/styles.css", Constraint.ALLOWED_ANY_TRANSPORT);
        securityHandler.put("/.favicon", Constraint.ALLOWED_ANY_TRANSPORT);
        securityHandler.put("/favicon-16x16.png", Constraint.ALLOWED_ANY_TRANSPORT);
        securityHandler.put("/favicon-32x32.png", Constraint.ALLOWED_ANY_TRANSPORT);
        securityHandler.put("/site.webmanifest", Constraint.ALLOWED_ANY_TRANSPORT);
        securityHandler.put("/apple-touch-icon.png", Constraint.ALLOWED_ANY_TRANSPORT);
        securityHandler.put("/android-chrome-192x192.png", Constraint.ALLOWED_ANY_TRANSPORT);
        securityHandler.put("/android-chrome-512x512.png", Constraint.ALLOWED_ANY_TRANSPORT);
        securityHandler.setAuthenticator(new FormAuthenticator("/login.html", "/login.html", true));
        securityHandler.setLoginService(loginService);

        return securityHandler;
    }


    private Resource findKeyStore(final ResourceFactory resourceFactory) {
        final String resourceName = "ssl/keystore.jks";
        final Resource resource = resourceFactory.newClassLoaderResource(resourceName);

        if (!Resources.isReadableFile(resource)) {
            throw new RuntimeException("Unable to read " + resourceName);
        }

        return resource;
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
