package com.shopsalevalue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LauncherIsolationTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private String home() throws Exception
    {
        Path dir = temporary.newFolder().toPath();
        Files.writeString(dir.resolve(".shopsalevalue-isolated-home"), "isolated test profile");
        return dir.toAbsolutePath().toString();
    }

    @Test public void acceptsExplicitCleanProfile() throws Exception
    {
        String path = home();
        ShopSaleValueLauncher.validateIsolation(path,path,Collections.emptyMap(),new String[]{"--developer-mode"});
    }

    @Test public void rejectsImplicitDefaultHome()
    {
        assertThrows(IllegalStateException.class, () -> ShopSaleValueLauncher.validateIsolation(null,
            System.getProperty("user.home"),Collections.emptyMap(),new String[0]));
    }

    @Test public void rejectsMismatchedHome() throws Exception
    {
        String path = home();
        assertThrows(IllegalStateException.class, () -> ShopSaleValueLauncher.validateIsolation(path,
            temporary.newFolder().getAbsolutePath(),Collections.emptyMap(),new String[0]));
    }

    @Test public void rejectsWindowsUserProfile() throws Exception
    {
        String path = home();
        assertThrows(IllegalStateException.class, () -> ShopSaleValueLauncher.validateIsolation(path,path,
            Collections.singletonMap("USERPROFILE",path),new String[0]));
    }

    @Test public void rejectsInheritedCredentialsAndInjection() throws Exception
    {
        String path = home();
        for (String name : new String[]{"JX_SESSION_ID","JX_CHARACTER_ID","JX_ACCESS_TOKEN","JAVA_TOOL_OPTIONS",
            "_JAVA_OPTIONS","JDK_JAVA_OPTIONS","CLASSPATH"})
        {
            Map<String,String> env = new HashMap<>(); env.put(name,"test-placeholder");
            assertThrows(name, IllegalStateException.class, () -> ShopSaleValueLauncher.validateIsolation(path,path,env,new String[0]));
        }
    }

    @Test public void rejectsCustomSessionArguments() throws Exception
    {
        String path = home();
        assertThrows(IllegalStateException.class, () -> ShopSaleValueLauncher.validateIsolation(path,path,
            Collections.emptyMap(),new String[]{"--sessionfile=other-session"}));
    }
}
