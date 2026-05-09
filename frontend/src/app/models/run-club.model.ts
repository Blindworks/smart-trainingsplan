export type RunClubStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type RunClubJoinPolicy = 'OPEN' | 'REQUEST';
export type RunClubMemberRole = 'ADMIN' | 'MEMBER';
export type RunClubMembershipStatus = 'PENDING' | 'ACTIVE' | 'LEFT';
export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export const ALL_DAYS_OF_WEEK: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY'
];

export interface RunClub {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  locationCity: string | null;
  locationCountry: string | null;
  latitude: number | null;
  longitude: number | null;
  meetingPointName: string | null;
  meetingSchedule: string | null;
  meetingTime: string | null;
  meetingDays: DayOfWeek[];
  joinPolicy: RunClubJoinPolicy;
  status: RunClubStatus;
  verified: boolean;
  socialInstagram: string | null;
  socialWebsite: string | null;
  socialStrava: string | null;
  hasLogo: boolean;
  hasCover: boolean;
  memberCount: number;
  createdAt: string;
  approvedAt: string | null;
}

export interface RunClubDetail extends RunClub {
  rejectionReason: string | null;
  isMember: boolean;
  myRole: RunClubMemberRole | null;
  myMembershipStatus: RunClubMembershipStatus | null;
  canManage: boolean;
}

export interface RunClubStats {
  memberCount: number;
  totalKmLifetime: number;
  totalKmLast30d: number;
  totalActivities: number;
  totalEventKm: number;
  totalEventCount: number;
}

export interface RunClubMembership {
  id: number;
  userId: number;
  username: string;
  userFirstName: string | null;
  userLastName: string | null;
  userProfileImageUrl?: string | null;
  hasProfileImage: boolean;
  creator: boolean;
  role: RunClubMemberRole;
  status: RunClubMembershipStatus;
  joinedAt: string | null;
  requestedAt: string | null;
}

export interface RunClubPost {
  id: number;
  clubId: number;
  authorId: number | null;
  authorName: string;
  authorProfileImageUrl: string | null;
  content: string;
  hasImage: boolean;
  linkedActivityId: number | null;
  linkedCommunityRouteId: number | null;
  linkedGroupEventId: number | null;
  likeCount: number;
  commentCount: number;
  likedByMe: boolean;
  createdAt: string;
}

export interface RunClubComment {
  id: number;
  postId: number;
  authorId: number;
  authorName: string;
  content: string;
  createdAt: string;
}

export interface CreateRunClubRequest {
  name: string;
  description?: string | null;
  locationCity?: string | null;
  locationCountry?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  meetingPointName?: string | null;
  meetingSchedule?: string | null;
  meetingTime?: string | null;
  meetingDays?: DayOfWeek[];
  joinPolicy?: RunClubJoinPolicy;
  socialInstagram?: string | null;
  socialWebsite?: string | null;
  socialStrava?: string | null;
}

export interface UpdateRunClubRequest extends CreateRunClubRequest {}

export interface CreatePostRequest {
  content: string;
  linkedActivityId?: number | null;
  linkedCommunityRouteId?: number | null;
  linkedGroupEventId?: number | null;
}

export interface MyRunClubEntry {
  club: RunClub;
  role: RunClubMemberRole;
  status: RunClubMembershipStatus;
}
