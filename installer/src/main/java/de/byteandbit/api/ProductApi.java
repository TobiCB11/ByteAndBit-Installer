package de.byteandbit.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.byteandbit.Constants;
import de.byteandbit.data.GameInstance;
import de.byteandbit.data.License;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static de.byteandbit.Util.getJsonResponse;

/**
 * API responsible for BAB product license management and download links.
 */
public class ProductApi {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static ProductApi INSTANCE;
    HashMap<String, DownloadResponse> downloadCache = new HashMap<>();
    private License license = null;

    public static ProductApi getInstance() {
        if (INSTANCE == null) INSTANCE = new ProductApi();
        return INSTANCE;
    }

    public DownloadResponse getDownloadLink(GameInstance instance, String scope) throws IOException {
        String key = license.getKey() + scope + instance.getMcVersion();
        if (downloadCache.containsKey(key)) return downloadCache.get(key);
        scope = scope.toLowerCase();
        String version = instance.getMcVersion();
        String[] version_parts = version.split("\\.");
        String babVersion = version_parts[0] + version_parts[1];
        String reqUrl = String.format(Constants.PRODUCT_DOWNLOAD_URL, license.getKey(), license.getProduct().getIdentifier().getKey(), babVersion, scope);
        DownloadResponse res = getJsonResponse(reqUrl, DownloadResponse.class, objectMapper);
        downloadCache.put(key, res);
        return res;
    }


    public List<String> getAvailableScopes() {
        return this.license.getScope();
    }

    public boolean canInstallFor(GameInstance instance, String scope) {
        try {
            return getDownloadLink(instance, scope).success;
        } catch (Exception e) {
            return false;
        }
    }

    public void updateLicense() throws IOException {
        this.license = this.fetchLicense(license.getKey());
    }

    private License fetchLicense(String licenseKey) throws IOException {
        return getJsonResponse(String.format(Constants.LICENSE_CHECK_URL, licenseKey), License.class, objectMapper);
    }

    public String getLicenseDownloadUrl() {
        if (this.license == null) return null;
        return String.format(Constants.LICENSE_DOWNLOAD_URL, this.license.getKey(), this.license.getOwner().getLink());
    }


    public boolean setLicense(String licenseKey) {
        try {
            this.license = fetchLicense(licenseKey);
            System.out.println(this.license);
            return license.isActive();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getProductName() {
        if (this.license == null) return null;
        return TranslationApi.getInstance().get(license.getProduct().getIdentifier().getTranslationKey());
    }

    public boolean noUsersRegistered() {
        return this.license == null || this.license.getUsers().isEmpty();
    }

    public String getLicenseControlPanelUrl() {
        return String.format(Constants.LICENSE_CONTROL_PANEL_URL, license.getId());
    }

    public boolean isLegacyLicense() {
        return this.license.getProduct().getIdentifier().getKey().equals(Constants.LEGACY_SPINOSAURUS_IDENTIFIER);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @lombok.Data
    public static class DownloadResponse {
        public boolean success;

        @com.fasterxml.jackson.annotation.JsonProperty()
        public String link = "no-link-provided";
    }
}
