import { api, paginatedRequest, productsRequest } from './api';
import type { 
  Product, 
  ProductSearchParams, 
  PaginatedResponse, 
  ProductFormData,
  ProductType 
} from '../types';

// Backend product interface (raw response from API)
interface BackendProduct {
  productId: string;
  title: string;
  description: string;
  category: string;
  price: number;
  valueAmount: number;
  quantityInStock: number;
  imageUrl?: string;
  productType: ProductType;
  entryDate: string;
  // Additional backend fields
  barcode?: string;
  dimensionsCm?: string;
  weightKg?: number;
  version?: number;
  orderItems?: any;
  cartItems?: any;
  // Type-specific fields
  author?: string;
  artists?: string;
  director?: string;
  publisher?: string;
  language?: string;
  genre?: string;
  runtime?: number;
  runtimeMinutes?: number;
  studio?: string;
  dvdLanguage?: string;
  subtitles?: string;
  dvdReleaseDate?: string;
  dvdGenre?: string;
  [key: string]: any; // For other dynamic fields
}

// Transform backend product to frontend product
function transformProduct(backendProduct: BackendProduct): Product {
  return {
    id: backendProduct.productId, // Map productId to id
    productId: backendProduct.productId,
    title: backendProduct.title,
    description: backendProduct.description,
    category: backendProduct.category,
    price: backendProduct.price,
    valueAmount: backendProduct.valueAmount,
    quantityInStock: backendProduct.quantityInStock,
    quantity: backendProduct.quantityInStock, // Map for compatibility
    imageUrl: backendProduct.imageUrl,
    productType: backendProduct.productType,
    entryDate: backendProduct.entryDate,
    // Type-specific fields
    author: backendProduct.author,
    artists: backendProduct.artists,
    director: backendProduct.director,
    publisher: backendProduct.publisher,
    language: backendProduct.language || backendProduct.dvdLanguage,
    genre: backendProduct.genre || backendProduct.dvdGenre,
    runtime: backendProduct.runtime || backendProduct.runtimeMinutes,
    studio: backendProduct.studio,
    // Additional fields from backend
    barcode: backendProduct.barcode,
    dimensionsCm: backendProduct.dimensionsCm,
    weightKg: backendProduct.weightKg,
  };
}

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

    const response = await productsRequest<BackendProduct>('/products', queryParams);
    
    // Transform backend products to frontend products
    return {
      items: response.items.map(transformProduct),
      pagination: response.pagination,
    };
  },

  async getProductById(id: string): Promise<Product> {
    const response = await api.get<BackendProduct>(`/products/${id}`);
    return transformProduct(response.data);
  },

  async searchProducts(searchParams: ProductSearchParams): Promise<PaginatedResponse<Product>> {
    const response = await productsRequest<BackendProduct>('/products/search', searchParams);
    return {
      items: response.items.map(transformProduct),
      pagination: response.pagination,
    };
  },

  async advancedSearch(searchParams: ProductSearchParams): Promise<PaginatedResponse<Product>> {
    const response = await productsRequest<BackendProduct>('/products/advanced-search', searchParams);
    return {
      items: response.items.map(transformProduct),
      pagination: response.pagination,
    };
  },

  async searchByProductType(
    productType: ProductType,
    searchParams: Omit<ProductSearchParams, 'productType'>
  ): Promise<PaginatedResponse<Product>> {
    const response = await productsRequest<BackendProduct>('/products/by-type', {
      ...searchParams,
      productType,
    });
    return {
      items: response.items.map(transformProduct),
      pagination: response.pagination,
    };
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
    // The backend expects type-specific endpoints
    const endpoint = `/admin/products/${productData.productType.toLowerCase()}s`;
    const response = await api.post<BackendProduct>(endpoint, productData);
    return transformProduct(response.data);
  },

  async updateProduct(id: string, productData: Partial<ProductFormData>): Promise<Product> {
    // For updates, we need the product type to use the correct endpoint
    const productType = productData.productType?.toLowerCase() || 'product';
    const endpoint = `/admin/products/${productType}s/${id}`;
    const response = await api.put<BackendProduct>(endpoint, productData);
    return transformProduct(response.data);
  },

  async deleteProduct(id: string, managerId?: string): Promise<void> {
    const params = managerId ? { managerId } : {};
    await api.delete(`/admin/products/${id}`, { params });
  },

  async deleteProducts(ids: string[], managerId?: string): Promise<void> {
    await api.post('/admin/products/bulk-delete', {
      productIds: ids,
      managerId: managerId
    });
  },

  async updateProductPrice(id: string, newPrice: number, managerId?: string): Promise<Product> {
    const response = await api.put<BackendProduct>(`/admin/products/${id}/price`, {
      newPrice: newPrice,
      managerId: managerId
    });
    return transformProduct(response.data);
  },

  async updateProductStock(id: string, quantityChange: number): Promise<Product> {
    const response = await api.put<BackendProduct>(`/admin/products/${id}/stock`, {
      quantityChange
    });
    return transformProduct(response.data);
  },

  // Admin product list with different permissions
  async getProductsForAdmin(params: ProductSearchParams = {}): Promise<PaginatedResponse<Product>> {
    const queryParams = {
      page: params.page || 1,
      limit: params.pageSize || 20, // Backend uses 'limit' instead of 'pageSize'
      keyword: params.keyword,
      category: params.category,
      sortBy: params.sortBy || 'title',
      sortOrder: params.sortOrder || 'ASC',
    };

    const response = await paginatedRequest<BackendProduct>('/admin/products/list', queryParams);
    return {
      items: response.items.map(transformProduct),
      pagination: response.pagination,
    };
  },
};