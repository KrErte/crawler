export interface Application {
  id: number;
  jobId: number;
  jobTitle: string;
  company: string;
  jobUrl: string;
  source: string;
  status: string;
  notes: string;
  appliedAt: string;
  updatedAt: string;
}

export interface CreateApplicationRequest {
  jobId: number;
  notes?: string;
}

export interface UpdateApplicationRequest {
  status?: string;
  notes?: string;
}
