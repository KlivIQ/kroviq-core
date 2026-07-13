package kroviq.wrapper.desktop.session;

public class DesktopSessionConfig {

    private String applicationPath;
    private String applicationArguments;
    private int launchTimeoutSeconds = 30;
    private boolean reuseSession = true;

    public DesktopSessionConfig() {}

    public String getApplicationPath() { return applicationPath; }

    public void setApplicationPath(String applicationPath) { this.applicationPath = applicationPath; }

    public String getApplicationArguments() { return applicationArguments; }

    public void setApplicationArguments(String applicationArguments) { this.applicationArguments = applicationArguments; }

    public int getLaunchTimeoutSeconds() { return launchTimeoutSeconds; }

    public void setLaunchTimeoutSeconds(int launchTimeoutSeconds) { this.launchTimeoutSeconds = launchTimeoutSeconds; }

    public boolean isReuseSession() { return reuseSession; }

    public void setReuseSession(boolean reuseSession) { this.reuseSession = reuseSession; }

    public static DesktopSessionConfig fromRunManager(
            String applicationPath, int launchTimeoutSeconds, boolean reuseSession) {
        DesktopSessionConfig config = new DesktopSessionConfig();
        config.setApplicationPath(applicationPath);
        config.setLaunchTimeoutSeconds(launchTimeoutSeconds);
        config.setReuseSession(reuseSession);
        return config;
    }
}
