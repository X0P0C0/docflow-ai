/**
 * API Response Helper
 * Handles different response formats from the backend
 */

type ApiResponse<T = any> = {
  code: number;
  message?: string;
  data: T;
};

/**
 * Extract array data from API response
 * Handles: array, {records: []}, {content: []}, {list: []}
 */
export function extractArray<T>(data: any): T[] {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (data.records && Array.isArray(data.records)) return data.records;
  if (data.content && Array.isArray(data.content)) return data.content;
  if (data.list && Array.isArray(data.list)) return data.list;
  return [];
}

/**
 * Extract paginated data from API response
 * Returns { items: [], total: number }
 */
export function extractPaginated<T>(data: any): { items: T[]; total: number } {
  if (!data) return { items: [], total: 0 };
  
  // Direct array
  if (Array.isArray(data)) {
    return { items: data, total: data.length };
  }
  
  // Standard paginated response
  if (data.records) {
    return {
      items: data.records,
      total: data.total ?? data.records.length
    };
  }
  
  // Spring Data format
  if (data.content) {
    return {
      items: data.content,
      total: data.totalElements ?? data.content.length
    };
  }
  
  // Custom format
  if (data.list) {
    return {
      items: data.list,
      total: data.total ?? data.list.length
    };
  }
  
  return { items: [], total: 0 };
}

/**
 * Check if API response is successful
 */
export function isSuccess(response: ApiResponse): boolean {
  return response?.code === 200 || response?.code === 0;
}
