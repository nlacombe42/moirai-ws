package net.nlacombe.moirai.webservice;

import net.nlacombe.moirai.dto.IcalToGoogleCalendarSyncRequest;
import net.nlacombe.moirai.service.SyncService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@RequestMapping("/api/v1")
public class SyncWebService {

    private SyncService syncService;

    @Inject
    public SyncWebService(SyncService syncService) {
        this.syncService = syncService;
    }

    @RequestMapping(value = "/syncIcalWithGoogleCalendar", method = RequestMethod.POST)
    public void syncIcalWithGoogleCalendar(@RequestBody IcalToGoogleCalendarSyncRequest request) {
        String sourceCalendarIcalUrl = request.getSourceCalendarIcalUrl();
        String googleUserAccessToken = request.getGoogleUserAccessToken();
        String googleUserRefreshToken = request.getGoogleUserRefreshToken();

        syncService.sync(sourceCalendarIcalUrl, googleUserAccessToken, googleUserRefreshToken);
    }

}
