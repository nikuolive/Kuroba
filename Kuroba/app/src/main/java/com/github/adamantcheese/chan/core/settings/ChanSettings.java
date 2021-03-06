/*
 * Kuroba - *chan browser https://github.com/Adamantcheese/Kuroba/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.adamantcheese.chan.core.settings;

import android.net.ConnectivityManager;
import android.net.Uri;

import com.github.adamantcheese.chan.BuildConfig;
import com.github.adamantcheese.chan.R;
import com.github.adamantcheese.chan.core.settings.base_dir.LocalThreadsBaseDirSetting;
import com.github.adamantcheese.chan.core.settings.base_dir.SavedFilesBaseDirSetting;
import com.github.adamantcheese.chan.core.settings.state.PersistableChanState;
import com.github.adamantcheese.chan.ui.adapter.PostsFilter;
import com.github.adamantcheese.chan.ui.controller.settings.captcha.JsCaptchaCookiesJar;
import com.github.adamantcheese.chan.utils.Logger;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import static com.github.adamantcheese.chan.utils.AndroidUtils.getAppDir;
import static com.github.adamantcheese.chan.utils.AndroidUtils.getPreferences;
import static com.github.adamantcheese.chan.utils.AndroidUtils.getRes;
import static com.github.adamantcheese.chan.utils.AndroidUtils.isConnected;
import static com.github.adamantcheese.chan.utils.AndroidUtils.postToEventBus;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * This settings class is for all persistable settings that should be saved as preferences. Note that all settings in here
 * will be exported when a backup is exported; for persistable application data that SHOULDN'T be exported, use
 * {@link PersistableChanState} to store that data.
 */

public class ChanSettings {
    public static final String EMPTY_JSON = "{}";
    public static final String NOTIFY_ALL_POSTS = "all";
    public static final String NOTIFY_ONLY_QUOTES = "quotes";
    public static final String NO_HASH_SET = "NO_HASH_SET";

    public enum MediaAutoLoadMode
            implements OptionSettingItem {
        // ALways auto load, either wifi or mobile
        ALL("all"),
        // Only auto load if on wifi
        WIFI("wifi"),
        // Never auto load
        NONE("none");

        String name;

        MediaAutoLoadMode(String name) {
            this.name = name;
        }

        @Override
        public String getKey() {
            return name;
        }

        public static boolean shouldLoadForNetworkType(ChanSettings.MediaAutoLoadMode networkType) {
            if (networkType == ChanSettings.MediaAutoLoadMode.NONE) {
                return false;
            } else if (networkType == ChanSettings.MediaAutoLoadMode.WIFI) {
                return isConnected(ConnectivityManager.TYPE_WIFI);
            } else {
                return networkType == ChanSettings.MediaAutoLoadMode.ALL;
            }
        }
    }

    public enum PostViewMode
            implements OptionSettingItem {
        LIST("list"),
        CARD("grid");

        String name;

        PostViewMode(String name) {
            this.name = name;
        }

        @Override
        public String getKey() {
            return name;
        }
    }

    public enum LayoutMode
            implements OptionSettingItem {
        AUTO("auto"),
        PHONE("phone"),
        SLIDE("slide"),
        SPLIT("split");

        String name;

        LayoutMode(String name) {
            this.name = name;
        }

        @Override
        public String getKey() {
            return name;
        }
    }

    public enum ConcurrentFileDownloadingChunks
            implements OptionSettingItem {
        One("One chunk"),
        Two("Two chunks"),
        Four("Four chunks");

        String name;

        ConcurrentFileDownloadingChunks(String name) {
            this.name = name;
        }

        @Override
        public String getKey() {
            return name;
        }

        public int toInt() {
            return (int) Math.pow(2, ordinal());
        }
    }

    public enum ImageClickPreloadStrategy
            implements OptionSettingItem {
        PreloadNext("Preload next image"),
        PreloadPrevious("Preload previous image"),
        PreloadBoth("Preload next and previous images"),
        PreloadNeither("Do not preload any images");

        String name;

        ImageClickPreloadStrategy(String name) {
            this.name = name;
        }

        @Override
        public String getKey() {
            return name;
        }
    }

    private static Proxy proxy;
    private static final String sharedPrefsFile = "shared_prefs/" + BuildConfig.APPLICATION_ID + "_preferences.xml";

    //region Declarations
    //region THREAD WATCHER
    public static final BooleanSetting watchEnabled;
    public static final BooleanSetting shortPinInfo;
    public static final BooleanSetting watchBackground;
    public static final IntegerSetting watchBackgroundInterval;
    public static final BooleanSetting removeWatchedFromCatalog;
    public static final BooleanSetting watchLastPageNotify;
    public static final StringSetting watchNotifyMode;
    public static final StringSetting watchSound;
    public static final BooleanSetting watchPeek;
    //endregion

    //region APPEARANCE
    // Theme
    public static final StringSetting theme;

    // Layout
    public static final OptionsSetting<LayoutMode> layoutMode;
    public static final IntegerSetting boardGridSpanCount;
    public static final BooleanSetting neverHideToolbar;
    public static final BooleanSetting enableReplyFab;
    public static final BooleanSetting moveInputToBottom;
    public static final BooleanSetting captchaOnBottom;
    public static final BooleanSetting useImmersiveModeForGallery;
    public static final BooleanSetting moveSortToToolbar;
    public static final BooleanSetting neverShowPages;

    //Post
    public static final StringSetting fontSize;
    public static final BooleanSetting fontAlternate;
    public static final BooleanSetting shiftPostFormat;
    public static final BooleanSetting accessibleInfo;
    public static final BooleanSetting postFullDate;
    public static final BooleanSetting postFileInfo;
    public static final BooleanSetting postFilename;
    public static final BooleanSetting textOnly;
    public static final BooleanSetting revealTextSpoilers;
    public static final BooleanSetting anonymize;
    public static final BooleanSetting showAnonymousName;
    public static final BooleanSetting anonymizeIds;
    public static final BooleanSetting addDubs;
    public static final BooleanSetting parseYoutubeTitles;
    public static final BooleanSetting parseYoutubeDuration;
    public static final BooleanSetting enableEmoji;

    // Images
    public static final BooleanSetting hideImages;
    public static final BooleanSetting removeImageSpoilers;
    public static final BooleanSetting revealimageSpoilers;
    public static final BooleanSetting highResCells;
    public static final BooleanSetting parsePostImageLinks;
    public static final BooleanSetting transparencyOn;
    public static final BooleanSetting neverShowWebmControls;

    // Set elsewhere in the application
    public static final OptionsSetting<PostViewMode> boardViewMode;
    public static final StringSetting boardOrder;
    //endregion

    //region BEHAVIOUR
    // General
    public static final BooleanSetting autoRefreshThread;
    public static final BooleanSetting controllerSwipeable;
    public static final BooleanSetting openLinkConfirmation;
    public static final BooleanSetting openLinkBrowser;
    public static final BooleanSetting imageViewerGestures;
    public static final BooleanSetting alwaysOpenDrawer;
    public static final StringSetting jsCaptchaCookies;

    // Reply
    public static final BooleanSetting postPinThread;
    public static final StringSetting postDefaultName;

    // Post
    public static final BooleanSetting repliesButtonsBottom;
    public static final BooleanSetting volumeKeysScrolling;
    public static final BooleanSetting tapNoReply;
    public static final BooleanSetting enableLongPressURLCopy;
    public static final BooleanSetting shareUrl;

    // Other options
    public static final StringSetting parseYoutubeAPIKey;
    public static final BooleanSetting fullUserRotationEnable;
    public static final BooleanSetting allowFilePickChooser;
    public static final BooleanSetting allowMediaScannerToScanLocalThreads;
    public static final BooleanSetting showCopyApkUpdateDialog;

    // Proxy
    public static final BooleanSetting proxyEnabled;
    public static final StringSetting proxyAddress;
    public static final IntegerSetting proxyPort;
    //endregion

    //region MEDIA
    // Saving
    public static final SavedFilesBaseDirSetting saveLocation;
    public static final LocalThreadsBaseDirSetting localThreadLocation;
    public static final BooleanSetting saveBoardFolder;
    public static final BooleanSetting saveThreadFolder;
    public static final BooleanSetting saveServerFilename;
    public static final BooleanSetting incrementalThreadDownloadingEnabled;

    // Video settings
    public static final BooleanSetting videoAutoLoop;
    public static final BooleanSetting videoDefaultMuted;
    public static final BooleanSetting headsetDefaultMuted;
    public static final BooleanSetting videoOpenExternal;
    public static final BooleanSetting videoStream;

    // Media loading
    public static final OptionsSetting<MediaAutoLoadMode> imageAutoLoadNetwork;
    public static final OptionsSetting<MediaAutoLoadMode> videoAutoLoadNetwork;
    public static final OptionsSetting<ImageClickPreloadStrategy> imageClickPreloadStrategy;
    public static final BooleanSetting autoLoadThreadImages;
    //endregion

    //region EXPERIMENTAL
    public static final OptionsSetting<ConcurrentFileDownloadingChunks> concurrentDownloadChunkCount;
    public static final StringSetting androidTenGestureZones;
    public static final BooleanSetting okHttpAllowHttp2;
    public static final BooleanSetting okHttpAllowIpv6;
    //endregion

    //region OTHER
    public static final BooleanSetting historyEnabled;
    public static final BooleanSetting collectCrashLogs;
    //endregion

    //region DEVELOPER
    public static final BooleanSetting crashOnSafeThrow;
    public static final BooleanSetting verboseLogs;
    //endregion

    //region DATA
    // While not a setting, the last image options selected should be persisted even after import.
    public static final StringSetting lastImageOptions;

    // While these are not "settings", they are here instead of in PersistableChanState because they control the appearance of hints.
    // Hints should not be shown if re-imported.
    public static final CounterSetting historyOpenCounter;
    public static final CounterSetting threadOpenCounter;
    public static final IntegerSetting drawerAutoOpenCount;
    public static final BooleanSetting reencodeHintShown;
    //endregion
    //endregion

    static {
        try {
            SettingProvider p = new SharedPreferencesSettingProvider(getPreferences());

            //region THREAD WATCHER
            watchEnabled = new BooleanSetting(p, "preference_watch_enabled", false);
            watchEnabled.addCallback((setting, value) -> postToEventBus(new SettingChanged<>(watchEnabled)));
            shortPinInfo = new BooleanSetting(p, "preference_short_pin_info", true);
            watchBackground = new BooleanSetting(p, "preference_watch_background_enabled", false);
            watchBackground.addCallback((setting, value) -> postToEventBus(new SettingChanged<>(watchBackground)));
            watchBackgroundInterval =
                    new IntegerSetting(p, "preference_watch_background_interval", (int) MINUTES.toMillis(15));
            watchBackgroundInterval.addCallback((setting, value) -> postToEventBus(new SettingChanged<>(
                    watchBackgroundInterval)));
            removeWatchedFromCatalog = new BooleanSetting(p, "remove_catalog_watch", false);
            watchLastPageNotify = new BooleanSetting(p, "preference_watch_last_page_notify", false);
            watchNotifyMode = new StringSetting(p, "preference_watch_notify_mode", NOTIFY_ALL_POSTS);
            watchSound = new StringSetting(p, "preference_watch_sound", "quotes");
            watchPeek = new BooleanSetting(p, "preference_watch_peek", true);
            //endregion

            //region APPEARANCE
            // Theme
            theme = new StringSetting(p, "preference_theme", "Yotsuba,RED,RED");

            //Layout
            layoutMode = new OptionsSetting<>(p, "preference_layout_mode", LayoutMode.class, LayoutMode.AUTO);
            boardGridSpanCount = new IntegerSetting(p, "preference_board_grid_span_count", 0);
            neverHideToolbar = new BooleanSetting(p, "preference_never_hide_toolbar", false);
            enableReplyFab = new BooleanSetting(p, "preference_enable_reply_fab", true);
            moveInputToBottom = new BooleanSetting(p, "move_input_bottom", false);
            captchaOnBottom = new BooleanSetting(p, "captcha_on_bottom", true);
            useImmersiveModeForGallery = new BooleanSetting(p, "use_immersive_mode_for_gallery", false);
            moveSortToToolbar = new BooleanSetting(p, "move_sort_to_toolbar", false);
            neverShowPages = new BooleanSetting(p, "never_show_page_number", false);

            // Post
            fontSize = new StringSetting(p, "preference_font", getRes().getBoolean(R.bool.is_tablet) ? "16" : "14");
            fontAlternate = new BooleanSetting(p, "preference_font_alternate", false);
            shiftPostFormat = new BooleanSetting(p, "shift_post_format", true);
            accessibleInfo = new BooleanSetting(p, "preference_enable_accessible_info", false);
            postFullDate = new BooleanSetting(p, "preference_post_full_date", false);
            postFileInfo = new BooleanSetting(p, "preference_post_file_info", true);
            postFilename = new BooleanSetting(p, "preference_post_filename", true);
            textOnly = new BooleanSetting(p, "preference_text_only", false);
            revealTextSpoilers = new BooleanSetting(p, "preference_reveal_text_spoilers", false);
            anonymize = new BooleanSetting(p, "preference_anonymize", false);
            showAnonymousName = new BooleanSetting(p, "preference_show_anonymous_name", false);
            anonymizeIds = new BooleanSetting(p, "preference_anonymize_ids", false);
            addDubs = new BooleanSetting(p, "add_dubs", false);
            parseYoutubeTitles = new BooleanSetting(p, "parse_youtube_titles", true);
            parseYoutubeDuration = new BooleanSetting(p, "parse_youtube_duration", false);
            enableEmoji = new BooleanSetting(p, "enable_emoji", false);

            // Images
            hideImages = new BooleanSetting(p, "preference_hide_images", false);
            removeImageSpoilers = new BooleanSetting(p, "preference_reveal_image_spoilers", false);
            revealimageSpoilers = new BooleanSetting(p, "preference_auto_unspoil_images", true);
            highResCells = new BooleanSetting(p, "high_res_cells", false);
            parsePostImageLinks = new BooleanSetting(p, "parse_post_image_links", true);
            transparencyOn = new BooleanSetting(p, "image_transparency_on", false);
            neverShowWebmControls = new BooleanSetting(p, "never_show_webm_controls", false);

            //Elsewhere
            boardViewMode =
                    new OptionsSetting<>(p, "preference_board_view_mode", PostViewMode.class, PostViewMode.LIST);
            boardOrder = new StringSetting(p, "preference_board_order", PostsFilter.Order.BUMP.name);
            //endregion

            //region BEHAVIOUR
            // General
            autoRefreshThread = new BooleanSetting(p, "preference_auto_refresh_thread", true);
            controllerSwipeable = new BooleanSetting(p, "preference_controller_swipeable", true);
            openLinkConfirmation = new BooleanSetting(p, "preference_open_link_confirmation", false);
            openLinkBrowser = new BooleanSetting(p, "preference_open_link_browser", false);
            imageViewerGestures = new BooleanSetting(p, "image_viewer_gestures", true);
            alwaysOpenDrawer = new BooleanSetting(p, "drawer_auto_open_always", false);
            jsCaptchaCookies = new StringSetting(p, "js_captcha_cookies", EMPTY_JSON);

            // Reply
            postPinThread = new BooleanSetting(p, "preference_pin_on_post", false);
            postDefaultName = new StringSetting(p, "preference_default_name", "");

            // Post
            repliesButtonsBottom = new BooleanSetting(p, "preference_buttons_bottom", false);
            volumeKeysScrolling = new BooleanSetting(p, "preference_volume_key_scrolling", false);
            tapNoReply = new BooleanSetting(p, "preference_tap_no_reply", false);
            enableLongPressURLCopy = new BooleanSetting(p, "long_press_image_url_copy", true);
            shareUrl = new BooleanSetting(p, "preference_image_share_url", false);

            // Other options
            // this is 4chanX's key, but it is recommended that you use your own
            parseYoutubeAPIKey =
                    new StringSetting(p, "parse_youtube_API_key", "AIzaSyB5_zaen_-46Uhz1xGR-lz1YoUMHqCD6CE");
            fullUserRotationEnable = new BooleanSetting(p, "full_user_rotation_enable", true);
            allowFilePickChooser = new BooleanSetting(p, "allow_file_picker_chooser", false);
            allowMediaScannerToScanLocalThreads =
                    new BooleanSetting(p, "allow_media_scanner_to_scan_local_threads", false);
            showCopyApkUpdateDialog = new BooleanSetting(p, "show_copy_apk_update_dialog", true);

            // Proxy
            proxyEnabled = new BooleanSetting(p, "preference_proxy_enabled", false);
            proxyAddress = new StringSetting(p, "preference_proxy_address", "");
            proxyPort = new IntegerSetting(p, "preference_proxy_port", 80);
            proxyEnabled.addCallback((setting, value) -> loadProxy());
            proxyAddress.addCallback((setting, value) -> loadProxy());
            proxyPort.addCallback((setting, value) -> loadProxy());
            loadProxy();
            //endregion

            //region MEDIA
            // Saving
            saveLocation = new SavedFilesBaseDirSetting(p);
            localThreadLocation = new LocalThreadsBaseDirSetting(p);
            saveBoardFolder = new BooleanSetting(p, "preference_save_subboard", false);
            saveThreadFolder = new BooleanSetting(p, "preference_save_subthread", false);
            saveServerFilename = new BooleanSetting(p, "preference_image_save_original", false);
            incrementalThreadDownloadingEnabled = new BooleanSetting(p, "incremental_thread_downloading", true);

            // Video Settings
            videoAutoLoop = new BooleanSetting(p, "preference_video_loop", true);
            videoDefaultMuted = new BooleanSetting(p, "preference_video_default_muted", true);
            headsetDefaultMuted = new BooleanSetting(p, "preference_headset_default_muted", true);
            videoOpenExternal = new BooleanSetting(p, "preference_video_external", false);
            videoStream = new BooleanSetting(p, "preference_video_stream", false);

            // Media loading
            imageAutoLoadNetwork = new OptionsSetting<>(p,
                    "preference_image_auto_load_network",
                    MediaAutoLoadMode.class,
                    MediaAutoLoadMode.WIFI
            );
            videoAutoLoadNetwork = new OptionsSetting<>(p,
                    "preference_video_auto_load_network",
                    MediaAutoLoadMode.class,
                    MediaAutoLoadMode.WIFI
            );
            imageClickPreloadStrategy = new OptionsSetting<>(p,
                    "image_click_preload_strategy",
                    ImageClickPreloadStrategy.class,
                    ImageClickPreloadStrategy.PreloadNext
            );
            autoLoadThreadImages = new BooleanSetting(p, "preference_auto_load_thread", false);
            //endregion

            //region EXPERIMENTAL
            concurrentDownloadChunkCount = new OptionsSetting<>(p,
                    "concurrent_file_downloading_chunks_count",
                    ConcurrentFileDownloadingChunks.class,
                    ConcurrentFileDownloadingChunks.Two
            );
            androidTenGestureZones = new StringSetting(p, "android_ten_gesture_zones", EMPTY_JSON);
            okHttpAllowHttp2 = new BooleanSetting(p, "ok_http_allow_http_2", true);
            okHttpAllowIpv6 = new BooleanSetting(p, "ok_http_allow_ipv6", true);
            //endregion

            //region OTHER
            historyEnabled = new BooleanSetting(p, "preference_history_enabled", true);
            collectCrashLogs = new BooleanSetting(p, "collect_crash_logs", true);
            //endregion

            //region DEVELOPER
            crashOnSafeThrow = new BooleanSetting(p, "crash_on_safe_throw", true);
            verboseLogs = new BooleanSetting(p, "verbose_logs", false);
            //endregion

            //region DATA
            lastImageOptions = new StringSetting(p, "last_image_options", "");
            historyOpenCounter = new CounterSetting(p, "counter_history_open");
            threadOpenCounter = new CounterSetting(p, "counter_thread_open");
            drawerAutoOpenCount = new IntegerSetting(p, "drawer_auto_open_count", 0);
            reencodeHintShown = new BooleanSetting(p, "preference_reencode_hint_already_shown", false);
            //endregion

        } catch (Throwable error) {
            // If something crashes while the settings are initializing we at least will have the
            // stacktrace. Otherwise we won't because of Feather.
            Logger.e("ChanSettings", "Error while initializing the settings", error);
            throw error;
        }
    }

    public static JsCaptchaCookiesJar getJsCaptchaCookieJar(Gson gson) {
        try {
            return gson.fromJson(ChanSettings.jsCaptchaCookies.get(), JsCaptchaCookiesJar.class);
        } catch (Throwable error) {
            Logger.e("ChanSettings", "Error while trying to deserialize JsCaptchaCookiesJar", error);
            return JsCaptchaCookiesJar.empty();
        }
    }

    /**
     * Returns a {@link Proxy} if a proxy is enabled, <tt>null</tt> otherwise.
     *
     * @return a proxy or null
     */
    public static Proxy getProxy() {
        return proxy;
    }

    private static void loadProxy() {
        if (proxyEnabled.get()) {
            proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyAddress.get(), proxyPort.get()));
        } else {
            proxy = null;
        }
    }

    /**
     * Reads setting from the shared preferences file to a string.
     * Called on the Database thread.
     */
    public static String serializeToString()
            throws IOException {
        String prevSaveLocationUri = null;
        String prevLocalThreadsLocationUri = null;

        /*
         We need to check if the user has any of the location settings set to a SAF directory.
         We can't export them because if the user reinstalls the app and then imports a location
         setting that point to a SAF directory that directory won't be valid for the app because
         after clearing settings all permissions for that directory will be lost. So in case the
         user tries to export SAF directory paths we don't export them and instead export default
         locations. But we also don't wont to change the paths for the current app so we need to
         save the previous paths, patch the sharedPrefs file read it to string and then restore
         the current paths back to what they were before exporting.

         We also need to reset the active dir setting in case of resetting the base dir (and then
         restore back) so that the user won't see empty paths to files when importing settings
         back.
        */
        if (saveLocation.isSafDirActive()) {
            // Save the saveLocationUri
            prevSaveLocationUri = saveLocation.getSafBaseDir().get();

            saveLocation.getSafBaseDir().remove();
            saveLocation.resetFileDir();
            saveLocation.resetActiveDir();
        }

        if (localThreadLocation.isSafDirActive()) {
            // Save the localThreadsLocationUri
            prevLocalThreadsLocationUri = localThreadLocation.getSafBaseDir().get();

            localThreadLocation.getSafBaseDir().remove();
            localThreadLocation.resetFileDir();
            localThreadLocation.resetActiveDir();
        }

        File file = new File(getAppDir(), sharedPrefsFile);

        if (!file.exists()) {
            throw new IOException("Shared preferences file does not exist! (" + file.getAbsolutePath() + ")");
        }

        if (!file.canRead()) {
            throw new IOException("Cannot read from shared preferences file! (" + file.getAbsolutePath() + ")");
        }

        byte[] buffer = new byte[(int) file.length()];

        try (FileInputStream inputStream = new FileInputStream(file)) {
            int readAmount = inputStream.read(buffer);

            if (readAmount != file.length()) {
                throw new IOException("Could not read shared prefs file readAmount != fileLength " + readAmount + ", "
                        + file.length());
            }
        }

        // Restore back the previous paths
        if (prevSaveLocationUri != null) {
            ChanSettings.saveLocation.resetFileDir();
            ChanSettings.saveLocation.setSafBaseDir(Uri.parse(prevSaveLocationUri));
        }

        if (prevLocalThreadsLocationUri != null) {
            ChanSettings.localThreadLocation.resetFileDir();
            ChanSettings.localThreadLocation.setSafBaseDir(Uri.parse(prevLocalThreadsLocationUri));
        }

        return new String(buffer);
    }

    /**
     * Reads settings from string and writes them to the shared preferences file.
     * Called on the Database thread.
     */
    public static void deserializeFromString(String settings)
            throws IOException {
        File file = new File(getAppDir(), sharedPrefsFile);

        if (!file.exists()) {
            // Hack to create the shared_prefs file when it does not exist so that we don't cancel
            // settings importing because shared_prefs file does not exist
            String fontSize = ChanSettings.fontSize.get();
            ChanSettings.fontSize.setSyncNoCheck(fontSize);
        }

        if (!file.canWrite()) {
            throw new IOException("Cannot write to shared preferences file! (" + file.getAbsolutePath() + ")");
        }

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(settings.getBytes());
            outputStream.flush();
        }
    }

    public static class SettingChanged<T> {
        public final Setting<T> setting;

        public SettingChanged(Setting<T> setting) {
            this.setting = setting;
        }
    }
}
