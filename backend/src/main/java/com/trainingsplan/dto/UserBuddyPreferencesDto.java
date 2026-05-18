package com.trainingsplan.dto;

import com.trainingsplan.entity.UserBuddyPreferences;

public class UserBuddyPreferencesDto {
    public boolean buddyDiscoverable;
    public int searchRadiusKm;
    public int paceTolerancePercent;
    public String availableWeekdays;
    public String availableTimeRanges;
    public boolean autoMatchEnabled;
    public Integer targetPaceSecPerKm;

    public static UserBuddyPreferencesDto fromEntity(UserBuddyPreferences p) {
        UserBuddyPreferencesDto dto = new UserBuddyPreferencesDto();
        dto.buddyDiscoverable = p.isBuddyDiscoverable();
        dto.searchRadiusKm = p.getSearchRadiusKm();
        dto.paceTolerancePercent = p.getPaceTolerancePercent();
        dto.availableWeekdays = p.getAvailableWeekdays();
        dto.availableTimeRanges = p.getAvailableTimeRanges();
        dto.autoMatchEnabled = p.isAutoMatchEnabled();
        dto.targetPaceSecPerKm = p.getTargetPaceSecPerKm();
        return dto;
    }
}
