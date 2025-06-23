import axios from 'axios';
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import type { ApiResponse, PaginatedResponse } from '../types';
import { handleApiError, withRetry } from '../utils/errorHandling';
import { toast } from '../components/common/Toast';

// API Configuration
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const API_TIMEOUT = 10000; // 10 seconds

// Create axios instance with default configuration
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
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
            action: userFriendlyError.action ? {
              label: userFriendlyError.action.label,
              onClick: userFriendlyError.action.handler,
            } : undefined,
          }
        );
      }

      // Return structured error response
      if (error.response?.data) {
        throw error.response.data;
      }
      
      throw {
        success: false,
        message: userFriendlyError.message,
        errors: [userFriendlyError.message],
      };
    }
  };

  // Apply retry logic for non-POST requests or when explicitly enabled
  if (enableRetry && (method === 'GET' || method === 'PUT' || method === 'DELETE')) {
    return withRetry(makeRequest, {
      maxRetries: method === 'GET' ? 3 : 1,
      delay: 1000,
      backoffMultiplier: 2,
    });
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
  const response = await api.get<PaginatedResponse<T>>(url, { params });
  return response.data;
}

export default apiClient;