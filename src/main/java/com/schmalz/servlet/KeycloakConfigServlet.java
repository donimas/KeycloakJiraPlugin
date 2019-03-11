package com.schmalz.servlet;


import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.sal.api.user.UserRole;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.schmalz.servlet.filter.AdaptedKeycloakOIDCFilter;
import org.keycloak.KeycloakSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Scanned
public class KeycloakConfigServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(KeycloakConfigServlet.class);
    public static final String UPDATED_SETTINGS_KEY = KeycloakConfigServlet.class.getName() + "-keycloakJiraPlugin-settingsUpdatedKey";
    private static List<String> validValues;
    public static final String REALM = "realm";
    public static final String PUBLIC_CLIENT = "public-client";
    public static final String RESOURCE = "resource";
    public static final String AUTH_SERVER_URL = "auth-server-url";
    public static final String SECRET = "secret";
    public static final String REALM_PUBLIC_KEY = "realm-public-key";
    public static final String SSL_REQUIRED = "ssl-required";
    public static final String CONFIDENTIAL_PORT = "confidential-port";
    public static final String USE_RESOURCE_ROLE_MAPPINGS = "use-resource-role-mappings";
    public static final String ENABLE_CORS = "enable-cors";
    public static final String CORS_MAX_AGE = "cors-max-age";
    public static final String CORS_ALLOWED_METHODS = "cors-allowed-methodes";
    public static final String CORS_ALLOWED_HEADERS = "cors-allowed-headers";
    public static final String CORS_EXPOSED_HEADERS = "cors-exposed-header";
    public static final String BEARER_ONLY = "bearer-only";
    public static final String AUTODETECT_BEARER_ONLY = "autodetect-bearer-only";
    public static final String ENABLE_BASIC_AUTH = "enable-basic-auth";
    public static final String EXPOSE_TOKEN = "expose-token";
    public static final String CONNECTION_POOL_SIZE = "connection-pool-size";
    public static final String DISABLE_TRUST_MANAGER = "disable-trust-manager";
    public static final String ALLOW_ANY_HOSTNAME = "allow-any-hostname";
    public static final String PROXY_URL = "proxy-url";
    public static final String TRUSTSTORE = "truststore";
    public static final String TRUSTSTORE_PASSWORD = "truststore-password";
    public static final String CLIENT_KEYSTORE = "client-keystore";
    public static final String CLIENT_KEYSTORE_PASSWORD = "client-keystore-password";
    public static final String CLIENT_KEY_PASSWORD = "client-key-password";
    public static final String ALWAYS_REFRESH_TOKEN = "always-refresh-token";
    public static final String REGISTER_NODE_AT_STARTUP = "register-node-at-startup";
    public static final String REGISTER_NODE_PERIOD = "register-node-period";
    public static final String TOKEN_STORE = "token-store";
    public static final String TOKEN_COOKIE_PATH = "token-cookie-path";
    public static final String PRINCIPAL_ATTRIBUTE = "principal-attribute";
    public static final String TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN = "turn-off-change-session-id-on-login";
    public static final String TOKEN_MINIMUM_TIME_TO_LIVE = "token-minimum-time-to-live";
    public static final String MIN_TIME_BETWEEN_JWKS_REQUEST = "min-time-between-jwks-requests";
    public static final String PUBLIC_KEY_CACHE_TTL = "public-key-cache-ttl";
    public static final String IGNORE_OAUTH_QUERY_PARAM = "ignore-oauth-query-parameter";
    public static final String VERIFY_AUDIENCE = "verify-token-audience";

    private final static String PAGE_VM = "/templates/keycloakJiraPlugin_ConfigPage.vm";

    @ComponentImport
    private final LoginUriProvider loginUriProvider;

    @ComponentImport
    private final UserManager userManager;

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    @ComponentImport
    private TemplateRenderer templateRenderer;

    public KeycloakConfigServlet(PluginSettingsFactory factory, TemplateRenderer renderer, UserManager manager, LoginUriProvider loginUriProvider) {

        pluginSettingsFactory = factory;
        templateRenderer = renderer;
        userManager = manager;
        this.loginUriProvider = loginUriProvider;
        validValues = new ArrayList<>();
        validValues.add(REALM);
        validValues.add(AUTH_SERVER_URL);
        validValues.add(RESOURCE);
        validValues.add(PUBLIC_CLIENT);
        validValues.add(SECRET);
        validValues.add(REALM_PUBLIC_KEY);
        validValues.add(REGISTER_NODE_AT_STARTUP);
        validValues.add(REGISTER_NODE_PERIOD);
        validValues.add(SSL_REQUIRED);
        validValues.add(CONFIDENTIAL_PORT);
        validValues.add(USE_RESOURCE_ROLE_MAPPINGS);
        validValues.add(ENABLE_CORS);
        validValues.add(CORS_MAX_AGE);
        validValues.add(CORS_ALLOWED_HEADERS);
        validValues.add(CORS_ALLOWED_METHODS);
        validValues.add(CORS_EXPOSED_HEADERS);
        validValues.add(BEARER_ONLY);
        validValues.add(AUTODETECT_BEARER_ONLY);
        validValues.add(ENABLE_BASIC_AUTH);
        validValues.add(EXPOSE_TOKEN);
        validValues.add(CONNECTION_POOL_SIZE);
        validValues.add(DISABLE_TRUST_MANAGER);
        validValues.add(ALLOW_ANY_HOSTNAME);
        validValues.add(PROXY_URL);
        validValues.add(TRUSTSTORE);
        validValues.add(TRUSTSTORE_PASSWORD);
        validValues.add(CLIENT_KEYSTORE);
        validValues.add(CLIENT_KEYSTORE_PASSWORD);
        validValues.add(CLIENT_KEY_PASSWORD);
        validValues.add(ALWAYS_REFRESH_TOKEN);
        validValues.add(TOKEN_STORE);
        validValues.add(TOKEN_COOKIE_PATH);
        validValues.add(PRINCIPAL_ATTRIBUTE);
        validValues.add(TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN);
        validValues.add(TOKEN_MINIMUM_TIME_TO_LIVE);
        validValues.add(MIN_TIME_BETWEEN_JWKS_REQUEST);
        validValues.add(PUBLIC_KEY_CACHE_TTL);
        validValues.add(IGNORE_OAUTH_QUERY_PARAM);
        validValues.add(VERIFY_AUDIENCE);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        UserProfile user = userManager.getRemoteUser(request);
        if (user == null) {
            redirectToLogin(request, response);
            return;
        }
        if (!hasAccessRights(user)) {
            handleUnauthorizedAccess(request, response);

            return;
        }

        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();

        Map<String, String> config = (Map<String, String>) settings.get(AdaptedKeycloakOIDCFilter.SETTINGS_KEY);
        Map<String, Object> context = new HashMap<>();
        context.put("map", config);
        context.put("requestUrl", URLDecoder.decode(request.getRequestURL().toString(), StandardCharsets.UTF_8.name()));
        context.put("username", user.getUsername());
        templateRenderer.render(PAGE_VM, context, response.getWriter());
        //https://developer.atlassian.com/server/jira/platform/creating-a-jira-issue-crud-servlet-and-issue-search/
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        Map<String, String> config = (Map<String, String>) retrieveAndRemove(settings, AdaptedKeycloakOIDCFilter.SETTINGS_KEY);
        Enumeration<String> parameters = request.getParameterNames();

        while (parameters.hasMoreElements()) {
            retrieveAndStore(request, parameters.nextElement(), config);
        }
        settings.put(AdaptedKeycloakOIDCFilter.SETTINGS_KEY, config);
        settings.put(UPDATED_SETTINGS_KEY, "True");
        response.sendRedirect(request.getContextPath() + request.getServletPath());

    }


    /**
     * tests, whether a given parameter is a supported/valid configuration key
     *
     * @param param the param to test
     * @return TRUE, if the param was a valid configuration key, false otherwise
     */
    private boolean isValidValue(String param) {

        return validValues.contains(param);
    }

    /**
     * @param profile the user to test
     * @return returns (@code TRUE) if the user is an admin or systemadmin
     */
    private boolean hasAccessRights(UserProfile profile) {

        return userManager.isAdmin(profile.getUserKey()) || userManager.isSystemAdmin(profile.getUserKey());
    }

    /**
     * redirects a user to jira's login page if he was not logged in. wont be called if the user wants to use keycloak
     * since the filter will act prior
     *
     * @param request  the request with the missing user
     * @param response the response to redirect
     * @throws IOException if the redirect fails
     */
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }

    private URI getUri(HttpServletRequest request) {

        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null) {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }


    /**
     * redirects the user, when it was determined that he does not have sufficient privileges. this differs, when a user
     * is authenticated to keycloak or not
     *
     * @param request  the given http-request
     * @param response the response to redirect
     * @throws IOException if the redirect cannot be send
     */
    private void handleUnauthorizedAccess(HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (request.getSession().getAttribute(KeycloakSecurityContext.class.getName()) == null) {
            response.sendRedirect(loginUriProvider.getLoginUriForRole(getUri(request), UserRole.ADMIN).toASCIIString());
        } else
            response.sendRedirect(request.getContextPath());
    }


    /**
     * Retrieves and removes the value of the given key from the settings
     *
     * @param settings the settings to search
     * @param key      the key whose value should be retrieved
     * @return the value of the key, NULL if the key is missing
     */
    private Object retrieveAndRemove(PluginSettings settings, String key) {

        Object returnObject = settings.get(key);
        settings.remove(key);
        return returnObject;
    }


    /**
     * retrieves a given parameter of a request and puts its value into a map
     *
     * @param request      the request holding the parameter
     * @param parameterKey the parameter to retrieve
     * @param storage      the map where the parameter, value pair should be stored
     */
    private void retrieveAndStore(HttpServletRequest request, String parameterKey, Map<String, String> storage) {

        if (isValidValue(parameterKey)) {
            String temp = request.getParameter(parameterKey);
            storage.put(parameterKey, temp);
        }
    }
}
//https://community.atlassian.com/t5/Answers-Developer-Questions/Retrieving-Plug-in-settings-using-PluginSettingsFactory/qaq-p/483804
