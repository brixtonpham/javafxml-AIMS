import axios from 'axios';
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import type { ApiResponse, PaginatedResponse } from '../types';
import { handleApiError, withRetry, withServerErrorRetry } from '../utils/errorHandling';
import { toast } from '../components/common/Toast';

// API Configuration
// In development, use proxy '/api' (handled by Vite proxy)
// In production, use the full API URL
export const API_BASE_URL = import.meta.env.DEV 
  ? '/api'  // Use proxy in development
  : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api');

const API_TIMEOUT = 10000; // 10 seconds

// Create axios instance with default configuration
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  withCredentials: true, // Enable credentials for CORS
});

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      // Clear auth data and redirect to login
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user_data');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Generic API request function with retry and enhanced error handling
export async function apiRequest<T>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  url: string,
  data?: any,
  config?: AxiosRequestConfig & { enableRetry?: boolean; showToast?: boolean }
): Promise<ApiResponse<T>> {
  const { enableRetry = true, showToast = true, ...axiosConfig } = config || {};

  const makeRequest = async (): Promise<ApiResponse<T>> => {
    try {
      const response = await apiClient.request<ApiResponse<T>>({
        method,
        url,
        data,
        ...axiosConfig,
      });
      
      // Show success toast for non-GET requests if enabled
      if (showToast && method !== 'GET' && response.data.success) {
        toast.success('Success', response.data.message || 'Operation completed successfully');
      }
      
      return response.data;
    } catch (error: any) {
      // Use enhanced error handling
      const userFriendlyError = handleApiError(error, {
        method,
        url,
        data: method !== 'GET' ? data : undefined,
      });

      // Show error toast if enabled
      if (showToast) {
        toast.error(
          userFriendlyError.title,
          userFriendlyError.message,
          {
            duration: 8000,
          }
        );
      }

      // Return structured error response
      if (error.response?.data) {
        throw error.response.data;
      }
      
      const apiError = new Error(userFriendlyError.message);
      (apiError as any).success = false;
      (apiError as any).errors = [userFriendlyError.message];
      throw apiError;
    }
  };

  // Enhanced retry logic with server error handling
  if (enableRetry && (method === 'GET' || method === 'PUT' || method === 'DELETE')) {
    // First, try with server error retry for better 500 handling
    try {
      return await withServerErrorRetry(makeRequest, {
        maxRetries: method === 'GET' ? 5 : 3, // More retries for server errors
      });
    } catch (error: any) {
      // If server error retry fails and it's not a 500-level error,
      // fall back to regular retry for other error types
      if (!error.response || error.response.status < 500) {
        return withRetry(makeRequest, {
          maxRetries: method === 'GET' ? 2 : 1,
          delay: 1000,
          backoffMultiplier: 2,
        });
      }
      throw error;
    }
  }

  return makeRequest();
}

// Specific HTTP method functions
export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    apiRequest<T>('GET', url, undefined, config),
  
  post: <T>(url: string, data?: any, config?: AxiosRequestConfig) =>
    apiRequest<T>('POST', url, data, config),
  
  put: <T>(url: string, data?: any, config?: AxiosRequestConfig) =>
    apiRequest<T>('PUT', url, data, config),
  
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    apiRequest<T>('DELETE', url, undefined, config),
};

// Paginated request function
export async function paginatedRequest<T>(
  url: string,
  params?: Record<string, any>
): Promise<PaginatedResponse<T>> {
  const response = await apiClient.get<PaginatedResponse<T>>(url, { params });
  return response.data;
}

// Products API specific request function (different response structure)
export async function productsRequest<T>(
  url: string,
  params?: Record<string, any>
): Promise<PaginatedResponse<T>> {
  console.log(`[${new Date().toISOString()}] productsRequest called with:`, { url, params });
  try {
    const response = await apiClient.get<import('../types').ProductsApiResponse<T>>(url, { params });
    console.log(`[${new Date().toISOString()}] productsRequest response:`, response.data);
    
    if (!response.data.success) {
      throw new Error(response.data.message || 'API request failed');
    }
    
    const result = {
      items: response.data.items,
      pagination: response.data.pagination,
    };
    
    console.log(`[${new Date().toISOString()}] productsRequest returning:`, result);
    return result;
  } catch (error) {
    console.error(`[${new Date().toISOString()}] productsRequest error:`, error);
    throw error;
  }
}

export default apiClient;