// Package ok?
package net.marvk.fs.vatsim.map.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.jcm.discordgamesdk.ActivityManager;
import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import net.marvk.fs.vatsim.map.data.DependencyRepository;
import net.marvk.fs.vatsim.map.data.Preferences;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
@Singleton
public class DiscordClient {

    // TODO: Replace trough real id
    private static final long APPLICATION_ID = 921611981629308949L;
    private static final String PREFERENCE_KEY = "general.discord_rich_presence";

    private final Preferences preferences;

    private boolean cancelRun = false;
    private final String activityTitle = "Loading VATprism...";
    private boolean startedActivity = false;
    private ActivityManager activityManager = null;

    @Inject()
    public DiscordClient(final Preferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Prepares the discord client and tries to download the native library!
     */
    public void prepareClient() {
        if(!this.preferences.booleanProperty(DiscordClient.PREFERENCE_KEY).get()) {
            return;
        }

        File discordLibrary;
        try {
            discordLibrary = DiscordClient.downloadDiscordLibrary();
        } catch (IOException e) {
            log.error("Cannot load discord native library!", e);
            return;
        }

        Core.init(discordLibrary);
    }

    /**
     * Creates the discord rich presence and set the state
     * @param title The title in the rich presence
     */
    public void startActivity(String title) {
        if(!this.preferences.booleanProperty(DiscordClient.PREFERENCE_KEY).get()) {
            return;
        }

        try(CreateParams params = new CreateParams()) {
            params.setClientID(DiscordClient.APPLICATION_ID);
            params.setFlags(CreateParams.getDefaultFlags());

            try(Core core = new Core(params)) {

                this.activityManager = core.activityManager();
                try(Activity activity = new Activity()) {
                    activity.setDetails("VATPrism");
                    activity.setState(title);

                    activity.timestamps().setStart(Instant.now());

                    this.activityManager.updateActivity(activity);
                }

                this.startedActivity = true;

                while (!this.cancelRun) {
                    core.runCallbacks();

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        log.warn("Thread sleep failed", e);
                    }
                }
            }
        }


    }

    /**
     * Updates the state in the discord rich presence!
     * @param title The title in the rich presence
     * @deprecated Don't use it! Not usable at the moment!
     */
    @Deprecated(since = "ever")
    public void updateActivity(String title) {
        if(!this.preferences.booleanProperty(DiscordClient.PREFERENCE_KEY).get()) {
            return;
        }

        if(!this.startedActivity) {
            log.error("Discord Activity didn't started!");
        }

        try (Activity activity = new Activity()) {
            activity.setDetails("VATPrism");
            activity.setState(title);

            activity.timestamps().setStart(Instant.now());

            this.activityManager.updateActivity(activity);
        }
    }

    public void shutdownActivity() {
        if(!this.preferences.booleanProperty(DiscordClient.PREFERENCE_KEY).get()) {
            return;
        }

        this.activityManager.clearActivity();

        this.cancelRun = true;
    }


    // Copied from the example. Should we move it into another class?
    @NonNull
    private static File downloadDiscordLibrary() throws IOException
    {
        // Find out which name Discord's library has (.dll for Windows, .so for Linux)
        String name = "discord_game_sdk";
        String suffix;

        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        if(osName.contains("windows"))
        {
            suffix = ".dll";
        }
        else if(osName.contains("linux"))
        {
            suffix = ".so";
        }
        else if(osName.contains("mac os"))
        {
            suffix = ".dylib";
        }
        else
        {
            throw new RuntimeException("cannot determine OS type: "+osName);
        }

		/*
		Some systems report "amd64" (e.g. Windows and Linux), some "x86_64" (e.g. Mac OS).
		At this point we need the "x86_64" version, as this one is used in the ZIP.
		 */
        if(arch.equals("amd64"))
            arch = "x86_64";

        // Path of Discord's library inside the ZIP
        String zipPath = "lib/"+arch+"/"+name+suffix;

        // Open the URL as a ZipInputStream
        URL downloadUrl = new URL("https://dl-game-sdk.discordapp.net/2.5.6/discord_game_sdk.zip");
        ZipInputStream zin = new ZipInputStream(downloadUrl.openStream());

        // Search for the right file inside the ZIP
        ZipEntry entry;
        while((entry = zin.getNextEntry())!=null)
        {
            if(entry.getName().equals(zipPath))
            {
                // Create a new temporary directory
                // We need to do this, because we may not change the filename on Windows
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "java-"+name+System.nanoTime());
                if(!tempDir.mkdir())
                    throw new IOException("Cannot create temporary directory");
                tempDir.deleteOnExit();

                // Create a temporary file inside our directory (with a "normal" name)
                File temp = new File(tempDir, name+suffix);
                temp.deleteOnExit();

                // Copy the file in the ZIP to our temporary file
                Files.copy(zin, temp.toPath());

                // We are done, so close the input stream
                zin.close();

                // Return our temporary file
                return temp;
            }
            // next entry
            zin.closeEntry();
        }
        zin.close();
        // We couldn't find the library inside the ZIP
        throw new RuntimeException("We couldn't find the library inside the ZIP!");
    }
}
