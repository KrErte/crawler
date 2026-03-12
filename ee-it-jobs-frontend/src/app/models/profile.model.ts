export interface NotificationPreferences {
  workplaceTypes: string[];
  jobTypes: string[];
  minSalary: number | null;
}

export interface Profile {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  linkedinUrl: string;
  coverLetter: string;
  skills: string[];
  preferences: any;
  notificationPreferences: NotificationPreferences | null;
  cvRawText: string;
  yearsExperience: number | null;
  roleLevel: string | null;
  hasCv: boolean;
  emailAlerts: boolean;
  alertThreshold: number;
}

export interface CvAnalysis {
  completenessScore: number;
  detectedSkills: string[];
  missingInDemandSkills: string[];
  suggestions: string[];
  yearsExperience: number | null;
  roleLevel: string | null;
  totalActiveJobs: number;
  matchingJobs: number;
}

export interface ProfileUpdateRequest {
  firstName: string;
  lastName: string;
  phone: string;
  linkedinUrl: string;
  coverLetter: string;
  emailAlerts?: boolean;
  alertThreshold?: number;
  notificationPreferences?: NotificationPreferences;
}
