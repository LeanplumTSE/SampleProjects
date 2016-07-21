// Copyright 2013, Leanplum, Inc.

package com.leanplum;

/**
 * Leanplum constants.
 * @author Andrew First.
 */
class Constants {
  static String API_HOST_NAME = "www.leanplum.com";
  static String SOCKET_HOST = "dev.leanplum.com";
  static int SOCKET_PORT = 80;
  static boolean API_SSL = true;
  static int NETWORK_TIMEOUT_SECONDS = 10;
  static int NETWORK_TIMEOUT_SECONDS_FOR_DOWNLOADS = 10;

  static String LEANPLUM_VERSION = "1.2.25";
  static String CLIENT = "android";

  static final String INVALID_MAC_ADDRESS = "02:00:00:00:00:00";
  static final String INVALID_MAC_ADDRESS_HASH = "0f607264fc6318a92b9e13c65db7cd3c";
  static final String INVALID_ANDROID_ID = "9774d56d682e549c";
  static final int MAX_DEVICE_ID_LENGTH = 400;
  static final int MAX_USER_ID_LENGTH = 400;

  static String defaultDeviceId = null;
  static boolean hashFilesToDetermineModifications = true;
  static boolean isDevelopmentModeEnabled = false;
  static boolean loggingEnabled = false;
  static boolean isTestMode = false;
  static boolean enableVerboseLoggingInDevelopmentMode = false;
  static boolean enableFileUploadingInDevelopmentMode = true;
  static boolean canDownloadContentMidSessionInProduction = false;
  static boolean isInPermanentFailureState = false;

  static boolean isNoop() {
    return isTestMode || isInPermanentFailureState;
  }

  static String API_SERVLET = "api";

  static class Defaults {
    static final String COUNT_KEY = "__leanplum_unsynced";
    static final String ITEM_KEY = "__leanplum_unsynced_%d";
    static final String VARIABLES_KEY = "__leanplum_variables";
    static final String ATTRIBUTES_KEY = "__leanplum_attributes";
    static final String ACTION_METADATA_KEY = "__leanplum_action_metadata";
    static final String TOKEN_KEY = "__leanplum_token";
    static final String MESSAGES_KEY = "__leanplum_messages";
    static final String REGIONS_KEY = "regions";
    static final String MESSAGE_TRIGGER_OCCURRENCES_KEY = "__leanplum_message_trigger_occurrences_%s";
    static final String MESSAGE_IMPRESSION_OCCURRENCES_KEY = "__leanplum_message_occurrences_%s";
    static final String MESSAGE_MUTED_KEY = "__leanplum_message_muted_%s";
    static final String PUSH_TOKEN_KEY = "__leanplum_push_token_%s-%s-%s";
    static final String LOCAL_NOTIFICATION_KEY = "__leanplum_local_message_%s";
    static final String NEWSFEED_KEY = "__leanplum_newsfeed";
  }

  static class Methods {
    static final String ADVANCE = "advance";
    static final String DELETE_NEWSFEED_MESSAGE = "deleteNewsfeedMessage";
    static final String DOWNLOAD_FILE = "downloadFile";
    static final String GET_NEWSFEED_MESSAGES = "getNewsfeedMessages";
    static final String GET_VARS = "getVars";
    static final String HEARTBEAT = "heartbeat";
    static final String LOG = "log";
    static final String MARK_NEWSFEED_MESSAGE_AS_READ = "markNewsfeedMessageAsRead";
    static final String MULTI = "multi";
    static final String PAUSE_SESSION = "pauseSession";
    static final String PAUSE_STATE = "pauseState";
    static final String REGISTER_FOR_DEVELOPMENT = "registerDevice";
    static final String RESUME_SESSION = "resumeSession";
    static final String RESUME_STATE = "resumeState";
    static final String SET_DEVICE_ATTRIBUTES = "setDeviceAttributes";
    static final String SET_TRAFFIC_SOURCE_INFO = "setTrafficSourceInfo";
    static final String SET_USER_ATTRIBUTES = "setUserAttributes";
    static final String SET_VARS = "setVars";
    static final String START = "start";
    static final String STOP = "stop";
    static final String TRACK = "track";
    static final String UPLOAD_FILE = "uploadFile";
  }

  static class Params {
    static final String ACTION = "action";
    static final String ACTION_DEFINITIONS = "actionDefinitions";
    static final String APP_ID = "appId";
    static final String BACKGROUND = "background";
    static final String CLIENT = "client";
    static final String CLIENT_KEY = "clientKey";
    static final String DATA = "data";
    static final String DEV_MODE = "devMode";
    static final String DEVICE_ID = "deviceId";
    static final String DEVICE_MODEL = "deviceModel";
    static final String DEVICE_NAME = "deviceName";
    static final String DEVICE_PUSH_TOKEN = "gcmRegistrationId";
    static final String DEVICE_SYSTEM_NAME = "systemName";
    static final String DEVICE_SYSTEM_VERSION = "systemVersion";
    static final String EMAIL = "email";
    static final String EVENT = "event";
    static final String FILE = "file";
    static final String FILE_ATTRIBUTES = "fileAttributes";
    static final String GOOGLE_PLAY_PURCHASE_DATA = "googlePlayPurchaseData";
    static final String GOOGLE_PLAY_PURCHASE_DATA_SIGNATURE = "googlePlayPurchaseDataSignature";
    static final String IAP_CURRENCY_CODE = "currencyCode";
    static final String IAP_ITEM = "item";
    static final String INCLUDE_DEFAULTS = "includeDefaults";
    static final String INCLUDE_MESSAGE_ID = "includeMessageId";
    static final String INFO = "info";
    static final String INSTALL_DATE = "installDate";
    static final String KINDS = "kinds";
    static final String LIMIT_TRACKING = "limitTracking";
    static final String MESSAGE_ID = "messageId";
    static final String NEW_USER_ID = "newUserId";
    static final String NEWSFEED_MESSAGE_ID = "newsfeedMessageId";
    static final String NEWSFEED_MESSAGES = "newsfeedMessages";
    static final String PARAMS = "params";
    static final String SDK_VERSION = "sdkVersion";
    static final String STATE = "state";
    static final String TIME = "time";
    static final String TOKEN = "token";
    static final String TRAFFIC_SOURCE = "trafficSource";
    static final String UPDATE_DATE = "updateDate";
    static final String USER_ID = "userId";
    static final String USER_ATTRIBUTES = "userAttributes";
    static final String VALUE = "value";
    static final String VARS = "vars";
    static final String VERSION_CODE = "versionCode";
    static final String VERSION_NAME = "versionName";
  }

  static class Keys {
    static final String ACTION_METADATA = "actionMetadata";
    static final String CITY = "city";
    static final String COUNTRY = "country";
    static final String DELIVERY_TIMESTAMP = "deliveryTimestamp";
    static final String EXPIRATION_TIMESTAMP = "expirationTimestamp";
    static final String FILENAME = "filename";
    static final String HASH = "hash";
    static final String INSTALL_TIME_INITIALIZED = "installTimeInitialized";
    static final String IS_READ = "isRead";
    static final String IS_REGISTERED = "isRegistered";
    static final String IS_REGISTERED_FROM_OTHER_APP = "isRegisteredFromOtherApp";
    static final String LATEST_VERSION = "latestVersion";
    static final String LOCALE = "locale";
    static final String LOCATION = "location";
    static final String MESSAGE_DATA = "messageData";
    static final String MESSAGES = "messages";
    static final String NEWSFEED_MESSAGES = "newsfeedMessages";
    static final String PUSH_MESSAGE_ACTION = "_lpx";
    static final String PUSH_MESSAGE_ACTION_NAME = "_lpa";
    static final String PUSH_MESSAGE_ID_NO_MUTE_WITH_ACTION = "_lpm";
    static final String PUSH_MESSAGE_ID_MUTE_WITH_ACTION = "_lpu";
    static final String PUSH_MESSAGE_ID_NO_MUTE = "_lpn";
    static final String PUSH_MESSAGE_ID_MUTE = "_lpv";
    static final String PUSH_MESSAGE_ID = "lp_messageId";
    static final String PUSH_MESSAGE_TEXT = "lp_message";
    static final String REASON = "reason";
    static final String REGION = "region";
    static final String REGION_STATE = "regionState";
    static final String REGIONS = "regions";
    static final String SIZE = "size";
    static final String STACK_TRACE = "stackTrace";
    static final String SUBTITLE = "Subtitle";
    static final String SYNC_NEWSFEED = "syncNewsfeed";
    static final String TIMEZONE = "timezone";
    static final String TIMEZONE_OFFSET_SECONDS = "timezoneOffsetSeconds";
    static final String TITLE = "Title";
    static final String TOKEN = "token";
    static final String UNREAD_COUNT = "unreadCount";
    static final String USER_INFO = "userInfo";
    static final String VARIANTS = "variants";
    static final String VARS = "vars";
    static final String VARS_FROM_CODE = "varsFromCode";
  }

  static class Kinds {
    static final String INT = "integer";
    static final String FLOAT = "float";
    static final String STRING = "string";
    static final String BOOLEAN = "bool";
    static final String FILE = "file";
    static final String DICTIONARY = "group";
    static final String ARRAY = "list";
    static final String ACTION = "action";
    static final String COLOR = "color";
  }

  static class Files {
    static final int MAX_UPLOAD_BATCH_SIZES = (25 * (1 << 20));
    static final int MAX_UPLOAD_BATCH_FILES = 16;
  }

  static final String EVENT_EXCEPTION = "__exception";

  static class Values {
    static final String DETECT = "(detect)";
    static final String ACTION_PREFIX = "__action__";
    static final String RESOURCES_VARIABLE = "__Android Resources";
    static final String ACTION_ARG = "__name__";
    static final String DEFAULT_PUSH_ACTION = "Open action";
    static final String DEFAULT_PUSH_MESSAGE = "Push message goes here.";
  }

  static class Crypt {
    static final int ITER_COUNT = 1000;
    static final int KEY_LENGTH = 256;
    static final String SALT = "L3@nP1Vm"; // Must have 8 bytes
    static final String IV = "__l3anplum__iv__"; // Must have 16 bytes
  }

  static class Messaging {
    static final int MAX_STORED_OCCURRENCES_PER_MESSAGE = 100;
    static final int DEFAULT_PRIORITY = 1000;
  }
}
