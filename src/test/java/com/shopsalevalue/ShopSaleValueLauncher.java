package com.shopsalevalue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import javax.swing.JOptionPane;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ShopSaleValueLauncher
{
    public static void main(String[] args) throws Exception
    {
        try
        {
            validateIsolation(System.getProperty("shopsalevalue.devHome"), System.getProperty("user.home"),
                System.getenv(), args);
        }
        catch (IllegalStateException ex)
        {
            if (Arrays.asList(args).contains("--verify-isolation")) throw ex;
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Shop Sale Value - isolated launcher required",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (Arrays.asList(args).contains("--verify-isolation"))
        {
            File expected = new File(System.getProperty("shopsalevalue.devHome"), ".runelite").getCanonicalFile();
            if (!RuneLite.RUNELITE_DIR.getCanonicalFile().equals(expected))
                throw new IllegalStateException("RuneLite did not select the isolated directory.");
            System.out.println("Isolation verified: separate RuneLite settings, session, cache and plugin directories.");
            System.out.println("No inherited login or Java override environment variables. No game client launched.");
            return;
        }
        ExternalPluginManager.loadBuiltin(ShopSaleValuePlugin.class);
        RuneLite.main(args);
    }

    // Development bootstrap only; not part of the Plugin Hub plugin JAR.
    static void validateIsolation(String requestedHome, String javaHome, Map<String,String> environment, String[] args)
    {
        if (requestedHome == null || requestedHome.isBlank() || javaHome == null)
            throw new IllegalStateException("Use the Shop Sale Value - Isolated desktop shortcut. Shared-profile startup is disabled.");
        Path profile = Paths.get(requestedHome).toAbsolutePath().normalize();
        if (!Paths.get(requestedHome).isAbsolute() || !profile.equals(Paths.get(javaHome).toAbsolutePath().normalize())
            || !Files.isRegularFile(profile.resolve(".shopsalevalue-isolated-home")))
            throw new IllegalStateException("The dedicated development home is missing or mismatched. Use the isolated desktop shortcut.");
        String normalHome = environment.get("USERPROFILE");
        if (normalHome != null && profile.equals(Paths.get(normalHome).toAbsolutePath().normalize()))
            throw new IllegalStateException("The normal Windows home cannot be used for this development client.");
        for (String name : environment.keySet())
        {
            String key = name.toUpperCase(java.util.Locale.ROOT);
            if (key.startsWith("JX_") || key.equals("JAVA_TOOL_OPTIONS") || key.equals("_JAVA_OPTIONS")
                || key.equals("JDK_JAVA_OPTIONS") || key.equals("CLASSPATH"))
            {
                if (environment.get(name) != null && !environment.get(name).isBlank())
                    throw new IllegalStateException("Inherited login or Java overrides detected. Use the isolated desktop shortcut.");
            }
        }
        for (String arg : args)
            if (!arg.equals("--developer-mode") && !arg.equals("--disable-telemetry")
                && !arg.equals("--verify-isolation") && !arg.equals("--help"))
                throw new IllegalStateException("This isolated launcher does not accept custom session or client configuration arguments.");
    }
}
