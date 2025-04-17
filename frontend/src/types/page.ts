// src/types/page.ts

// Corresponds to the Page schema in OpenAPI
export interface Page<T> {
  records: T[];       // The actual data items for the current page
  total: number;        // Total number of records across all pages
  size: number;         // Number of records per page
  current: number;      // Current page number
  pages: number;        // Total number of pages
  // orders, optimizeCountSql, searchCount, etc., are often backend-specific
  // and might not be needed directly in the frontend type unless used.
}
