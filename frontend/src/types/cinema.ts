export interface Cinema {
  id: number;
  name: string;
  address: string;
  phone?: string;
  logo?: string;
  description?: string;
  adminUserId?: number;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'DISABLED';
  createTime?: string; // ISO 8601 date-time string
  updateTime?: string; // ISO 8601 date-time string
}
