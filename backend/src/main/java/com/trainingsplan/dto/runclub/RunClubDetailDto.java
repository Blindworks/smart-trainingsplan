package com.trainingsplan.dto.runclub;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trainingsplan.entity.RunClubMemberRole;
import com.trainingsplan.entity.RunClubMembershipStatus;

/** Extended club view returned for the detail page; adds membership info for the current user. */
public class RunClubDetailDto extends RunClubDto {

    @JsonProperty("isMember")
    private boolean isMember;
    private RunClubMemberRole myRole;
    private RunClubMembershipStatus myMembershipStatus;
    private Long myMembershipId;
    private boolean canManage;
    private RunClubStatsDto stats;

    public RunClubDetailDto() {}

    @JsonProperty("isMember")
    public boolean isMember() { return isMember; }

    @JsonProperty("isMember")
    public void setMember(boolean member) { isMember = member; }

    public RunClubMemberRole getMyRole() { return myRole; }
    public void setMyRole(RunClubMemberRole myRole) { this.myRole = myRole; }

    public RunClubMembershipStatus getMyMembershipStatus() { return myMembershipStatus; }
    public void setMyMembershipStatus(RunClubMembershipStatus myMembershipStatus) { this.myMembershipStatus = myMembershipStatus; }

    public Long getMyMembershipId() { return myMembershipId; }
    public void setMyMembershipId(Long myMembershipId) { this.myMembershipId = myMembershipId; }

    public boolean isCanManage() { return canManage; }
    public void setCanManage(boolean canManage) { this.canManage = canManage; }

    public RunClubStatsDto getStats() { return stats; }
    public void setStats(RunClubStatsDto stats) { this.stats = stats; }
}
