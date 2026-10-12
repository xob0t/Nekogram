package tw.nekomimi.nekogram;

import org.telegram.messenger.BuildConfig;

import tw.nekomimi.nekogram.helpers.UserHelper;

public class Extra {
    // https://core.telegram.org/api/obtaining_api_id
    public static final int APP_ID = 0;
    public static final String APP_HASH = "";

    public static final String PLAYSTORE_APP_URL = "";

    public static String TWPIC_BOT_USERNAME = null;

    public static boolean isDirectApp() {
        return "release".equals(BuildConfig.BUILD_TYPE) || "debug".equals(BuildConfig.BUILD_TYPE);
    }

    public static UserHelper.BotInfo getHelperBot() {
        return null;
    }

    public static UserHelper.UserInfoBot getUserInfoBot(boolean fallback) {
        return null;
    }
}
