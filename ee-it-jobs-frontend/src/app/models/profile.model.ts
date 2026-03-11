export interface Profile {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  linkedinUrl: string;
  coverLetter: string;
  skills: string[];
  preferences: any;
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
}
