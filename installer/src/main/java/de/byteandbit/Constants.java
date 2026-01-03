package de.byteandbit;

import java.awt.*;
import java.net.URL;
import java.util.Objects;

/**
 * description missing.
 */
public class Constants {

    public static final String PROGRAM_TITLE = "ByteAndBit-Installer";

    public static final int COMMUNICATION_PORT = 61320;
    public static final int JVM_SEARCH_INTERVAL_MS = 1000;
    public static final Rectangle PROGRAM_GEOMETRY = new Rectangle(500, 300);
    public static final URL BAB_LOGO = Objects.requireNonNull(Constants.class.getClassLoader().getResource("bab_logo.png"));
    public static final URL BAB_ICON = Objects.requireNonNull(Constants.class.getClassLoader().getResource("bab_icon.png"));
    public static final URL AGENT = Objects.requireNonNull(Constants.class.getClassLoader().getResource("agent.jar"));
    public static final String i18nPathLocal = "/i18n/%s.json";
    public static final String i18nPathRemote = "https://i18n-data.nbg1.your-objectstorage.com/%s.json"; // language code

    public static final String LICENSE_CHECK_URL = "https://lion-service.byteandbit.cloud/microservice/lionService/api/license/fetch/%s";
    public static final String LICENSE_CONTROL_PANEL_URL = "https://ccp.byteandbit.studio/home/user-data/license/edit/%s"; // licence internal id
    public static final String PRODUCT_DOWNLOAD_URL = "https://lion-service.byteandbit.cloud/microservice/lionService/api/version/download/%s/%s/%s/latest-%s"; // key + product key + version + scope
    public static final String LICENSE_DOWNLOAD_URL = "https://lion-service.byteandbit.cloud/microservice/lionService/api/license/download/%s/%s"; // key + userId

    public static final int UI_ARTIFICIAL_DELAY_MS = 500;

    public static final String LEGACY_SPINOSAURUS_IDENTIFIER = "legacy_spinosaurus"; // identifier to legacy spino, which is not supported
    public static final String LICENSE_PREFIX = "LION-";
    public static final int MAX_LICENSE_LENGTH = 35;

}
