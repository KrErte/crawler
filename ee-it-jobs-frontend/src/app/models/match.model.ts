export interface MatchResult {
  job: import('./job.model').Job;
  matchPercentage: number;
  matchedSkills: string[];
  matchExplanation: string | null;
}

export interface JobMatchScore {
  jobId: number;
  matchPercentage: number;
  matchedSkills: string[];
}
