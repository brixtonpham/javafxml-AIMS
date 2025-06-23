import { api, paginatedRequest } from './api';
import type { 
  Product, 
  ProductSearchParams, 
  PaginatedResponse, 
  ProductFormData,
  ProductType 
} from '../types';

export const productService = {
  // Customer-facing product operations
  async getProducts(params: ProductSearchParams = {}): Promise<PaginatedResponse<Product>> {
    const queryParams = {
      page: params.page || 1,
      pageSize: params.pageSize || 20,
      keyword: params.keyword,
      category: params.category,
      productType: params.productType,
      sortBy: params.sortBy || 'entryDate',
      sortOrder: params.sortOrder || 'DESC',
    };

    return paginatedRequest<Product>('/products', queryParams);
  },

  async getProductById(id: string): Promise<Product> {
    const response = await api.get<Product>(`/products/${id}`);
    return response.data;
  },

  async searchProducts(searchParams: ProductSearchParams): Promise<PaginatedResponse<Product>> {
    return paginatedRequest<Product>('/products/search', searchParams);
  },

  async advancedSearch(searchParams: ProductSearchParams): Promise<PaginatedResponse<Product>> {
    return paginatedRequest<Product>('/products/advanced-search', searchParams);
  },

  async searchByProductType(
    productType: ProductType,
    searchParams: Omit<ProductSearchParams, 'productType'>
  ): Promise<PaginatedResponse<Product>> {
    return paginatedRequest<Product>('/products/by-type', {
      ...searchParams,
      productType,
    });
  },

  async getCategories(): Promise<string[]> {
    const response = await api.get<string[]>('/products/categories');
    return response.data;
  },

  async getProductTypes(): Promise<string[]> {
    const response = await api.get<string[]>('/products/types');
    return response.data;
  },

  // Admin/Product Manager operations
  async createProduct(productData: ProductFormData): Promise<Product> {
    const response = await api.post<Product>('/admin/products', productData);
    return response.data;
  },

  async updateProduct(id: string, productData: Partial<ProductFormData>): Promise<Product> {
    const response = await api.put<Product>(`/admin/products/${id}`, productData);
    return response.data;
  },

  async deleteProduct(id: string): Promise<void> {
    await api.delete(`/admin/products/${id}`);
  },

  async deleteProducts(ids: string[]): Promise<void> {
    await api.delete('/admin/products/bulk', {
      data: { productIds: ids }
    });
  },

  async updateProductPrice(id: string, newPrice: number): Promise<Product> {
    const response = await api.put<Product>(`/admin/products/${id}/price`, {
      price: newPrice
    });
    return response.data;
  },

  async updateProductStock(id: string, quantityChange: number): Promise<Product> {
    const response = await api.put<Product>(`/admin/products/${id}/stock`, {
      quantityChange
    });
    return response.data;
  },

  // Admin product list with different permissions
  async getProductsForAdmin(params: ProductSearchParams = {}): Promise<PaginatedResponse<Product>> {
    const queryParams = {
      page: params.page || 1,
      pageSize: params.pageSize || 20,
      keyword: params.keyword,
      category: params.category,
      productType: params.productType,
      sortBy: params.sortBy || 'entryDate',
      sortOrder: params.sortOrder || 'DESC',
    };

    return paginatedRequest<Product>('/admin/products', queryParams);
  },
};