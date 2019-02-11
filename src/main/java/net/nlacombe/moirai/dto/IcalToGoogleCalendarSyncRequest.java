package net.nlacombe.moirai.dto;

public class IcalToGoogleCalendarSyncRequest {

    private String sourceCalendarIcalUrl;
    private String googleUserAccessToken;
    private String googleUserRefreshToken;

    public String getSourceCalendarIcalUrl() {
        return sourceCalendarIcalUrl;
    }

    public void setSourceCalendarIcalUrl(String sourceCalendarIcalUrl) {
        this.sourceCalendarIcalUrl = sourceCalendarIcalUrl;
    }

    public String getGoogleUserAccessToken() {
        return googleUserAccessToken;
    }

    public void setGoogleUserAccessToken(String googleUserAccessToken) {
        this.googleUserAccessToken = googleUserAccessToken;
    }

    public String getGoogleUserRefreshToken() {
        return googleUserRefreshToken;
    }

    public void setGoogleUserRefreshToken(String googleUserRefreshToken) {
        this.googleUserRefreshToken = googleUserRefreshToken;
    }
}
