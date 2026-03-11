export interface Job {
  id: number;
  title: string;
  company: string;
  location: string;
  url: string;
  source: string;
  datePosted: string | null;
  dateScraped: string | null;
  jobType: string;
  workplaceType: string;
  department: string | null;
  salaryText: string | null;
  descriptionSnippet: string | null;
  fullDescription: string | null;
  skills: string[] | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
}

export interface JobPage {
  content: Job[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface JobFilters {
  companies: string[];
  sources: string[];
  jobTypes: string[];
  workplaceTypes: string[];
}
