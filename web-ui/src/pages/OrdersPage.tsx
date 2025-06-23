import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import { Button } from '../components/ui';
import { OrderCard, OrderCancellation } from '../components/orders';
import { useOrderContext } from '../contexts/OrderContext';
import { useAuth } from '../components/auth';
import type { OrderStatus } from '../types';
import type { OrderSearchFilters } from '../types/order';
import { ORDER_STATUS_LABELS } from '../types/order';

const STATUS_FILTER_OPTIONS: Array<{ value: OrderStatus | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'SHIPPED', label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'REJECTED', label: 'Rejected' }
];

export const OrdersPage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  const {
    orders,
    filters,
    stats,
    isLoading,
    error,
    updateFilters,
    cancelOrder,
    fetchUserOrders,
    ordersQuery
  } = useOrderContext();

  const [searchParams, setSearchParams] = useSearchParams();
  const [searchQuery, setSearchQuery] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [showCancellation, setShowCancellation] = useState<string | null>(null);

  // Initialize filters from URL params
  useEffect(() => {
    const status = searchParams.get('status') as OrderStatus | 'ALL' || 'ALL';
    const page = parseInt(searchParams.get('page') || '1');
    const search = searchParams.get('search') || '';
    const from = searchParams.get('from') || '';
    const to = searchParams.get('to') || '';

    updateFilters({
      status: status,
      page,
      orderId: search,
      dateFrom: from,
      dateTo: to
    });

    setSearchQuery(search);
    setDateFrom(from);
    setDateTo(to);
  }, [searchParams, updateFilters]);

  // Fetch orders when filters change
  useEffect(() => {
    if (isAuthenticated) {
      fetchUserOrders();
    }
  }, [filters, isAuthenticated, fetchUserOrders]);

  const handleStatusFilter = (status: OrderStatus | 'ALL') => {
    const newParams = new URLSearchParams(searchParams);
    if (status === 'ALL') {
      newParams.delete('status');
    } else {
      newParams.set('status', status);
    }
    newParams.set('page', '1');
    setSearchParams(newParams);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const newParams = new URLSearchParams(searchParams);
    
    if (searchQuery.trim()) {
      newParams.set('search', searchQuery.trim());
    } else {
      newParams.delete('search');
    }
    
    if (dateFrom) {
      newParams.set('from', dateFrom);
    } else {
      newParams.delete('from');
    }
    
    if (dateTo) {
      newParams.set('to', dateTo);
    } else {
      newParams.delete('to');
    }
    
    newParams.set('page', '1');
    setSearchParams(newParams);
  };

  const handleClearFilters = () => {
    setSearchQuery('');
    setDateFrom('');
    setDateTo('');
    setSearchParams({});
  };

  const handleCancelOrder = async (orderId: string, reason?: string) => {
    try {
      await cancelOrder(orderId, reason);
      setShowCancellation(null);
    } catch (error) {
      console.error('Failed to cancel order:', error);
    }
  };

  const handlePageChange = (page: number) => {
    const newParams = new URLSearchParams(searchParams);
    newParams.set('page', page.toString());
    setSearchParams(newParams);
  };

  const renderPagination = () => {
    const currentPage = filters.page || 1;
    const totalPages = Math.ceil((stats?.total || 0) / (filters.pageSize || 10));
    
    if (totalPages <= 1) return null;

    return (
      <div className="flex items-center justify-between mt-6">
        <div className="text-sm text-gray-500">
          Showing {((currentPage - 1) * (filters.pageSize || 10)) + 1} to{' '}
          {Math.min(currentPage * (filters.pageSize || 10), stats?.total || 0)} of{' '}
          {stats?.total || 0} orders
        </div>
        
        <div className="flex space-x-2">
          <Button
            variant="outline"
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 1}
          >
            Previous
          </Button>
          
          {/* Page numbers */}
          {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
            let pageNum;
            if (totalPages <= 5) {
              pageNum = i + 1;
            } else if (currentPage <= 3) {
              pageNum = i + 1;
            } else if (currentPage >= totalPages - 2) {
              pageNum = totalPages - 4 + i;
            } else {
              pageNum = currentPage - 2 + i;
            }
            
            return (
              <Button
                key={pageNum}
                variant={pageNum === currentPage ? 'primary' : 'outline'}
                onClick={() => handlePageChange(pageNum)}
              >
                {pageNum}
              </Button>
            );
          })}
          
          <Button
            variant="outline"
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
          >
            Next
          </Button>
        </div>
      </div>
    );
  };

  if (!isAuthenticated) {
    return (
      <AppLayout title="Orders - Please Login">
        <div className="text-center py-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Please Log In</h2>
          <p className="text-gray-600 mb-6">You need to be logged in to view your orders.</p>
          <Button onClick={() => window.location.href = '/login'}>
            Log In
          </Button>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout title="My Orders">
      <div className="max-w-6xl mx-auto px-4 py-6">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">My Orders</h1>
          <p className="text-gray-600">
            Track and manage your orders from AIMS
          </p>
        </div>

        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4 mb-8">
            <div className="bg-white rounded-lg border border-gray-200 p-4">
              <div className="text-2xl font-bold text-gray-900">{stats.total}</div>
              <div className="text-sm text-gray-500">Total Orders</div>
            </div>
            <div className="bg-white rounded-lg border border-gray-200 p-4">
              <div className="text-2xl font-bold text-yellow-600">{stats.pending}</div>
              <div className="text-sm text-gray-500">Pending</div>
            </div>
            <div className="bg-white rounded-lg border border-gray-200 p-4">
              <div className="text-2xl font-bold text-blue-600">{stats.approved}</div>
              <div className="text-sm text-gray-500">Approved</div>
            </div>
            <div className="bg-white rounded-lg border border-gray-200 p-4">
              <div className="text-2xl font-bold text-indigo-600">{stats.shipped}</div>
              <div className="text-sm text-gray-500">Shipped</div>
            </div>
            <div className="bg-white rounded-lg border border-gray-200 p-4">
              <div className="text-2xl font-bold text-green-600">{stats.delivered}</div>
              <div className="text-sm text-gray-500">Delivered</div>
            </div>
            <div className="bg-white rounded-lg border border-gray-200 p-4">
              <div className="text-2xl font-bold text-red-600">{stats.cancelled}</div>
              <div className="text-sm text-gray-500">Cancelled</div>
            </div>
            <div className="bg-white rounded-lg border border-gray-200 p-4">
              <div className="text-2xl font-bold text-red-600">{stats.rejected}</div>
              <div className="text-sm text-gray-500">Rejected</div>
            </div>
          </div>
        )}

        {/* Filters */}
        <div className="bg-white rounded-lg border border-gray-200 p-6 mb-6">
          {/* Status Filter */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Filter by Status
            </label>
            <div className="flex flex-wrap gap-2">
              {STATUS_FILTER_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  onClick={() => handleStatusFilter(option.value)}
                  className={`px-3 py-1 rounded-full text-sm font-medium transition-colors ${
                    filters.status === option.value
                      ? 'bg-blue-100 text-blue-800 border border-blue-200'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200 border border-gray-200'
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          {/* Search and Date Filters */}
          <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label htmlFor="search" className="block text-sm font-medium text-gray-700 mb-1">
                Search Order ID
              </label>
              <input
                type="text"
                id="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Enter order ID..."
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            
            <div>
              <label htmlFor="dateFrom" className="block text-sm font-medium text-gray-700 mb-1">
                From Date
              </label>
              <input
                type="date"
                id="dateFrom"
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            
            <div>
              <label htmlFor="dateTo" className="block text-sm font-medium text-gray-700 mb-1">
                To Date
              </label>
              <input
                type="date"
                id="dateTo"
                value={dateTo}
                onChange={(e) => setDateTo(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            
            <div className="flex items-end space-x-2">
              <Button type="submit" className="flex-1">
                Search
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={handleClearFilters}
              >
                Clear
              </Button>
            </div>
          </form>
        </div>

        {/* Orders List */}
        {isLoading ? (
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading orders...</p>
          </div>
        ) : error ? (
          <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
            <p className="text-red-800">{error}</p>
            <Button
              onClick={() => ordersQuery.refetch()}
              className="mt-4"
            >
              Try Again
            </Button>
          </div>
        ) : orders.length === 0 ? (
          <div className="text-center py-12">
            <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <h3 className="mt-2 text-sm font-medium text-gray-900">No orders found</h3>
            <p className="mt-1 text-sm text-gray-500">
              {filters.status !== 'ALL' || searchQuery || dateFrom || dateTo
                ? 'Try adjusting your filters'
                : "You haven't placed any orders yet"
              }
            </p>
            {(!filters.status || filters.status === 'ALL') && !searchQuery && !dateFrom && !dateTo && (
              <div className="mt-6">
                <Button onClick={() => window.location.href = '/products'}>
                  Start Shopping
                </Button>
              </div>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {orders.map((order) => (
              <OrderCard
                key={order.id}
                order={order}
                onCancel={(orderId) => setShowCancellation(orderId)}
                onViewDetails={(orderId) => window.location.href = `/orders/${orderId}`}
              />
            ))}
            
            {renderPagination()}
          </div>
        )}

        {/* Order Cancellation Modal */}
        {showCancellation && (
          <OrderCancellation
            order={orders.find(o => o.id === showCancellation)!}
            onCancel={handleCancelOrder}
            onClose={() => setShowCancellation(null)}
            isLoading={false}
          />
        )}
      </div>
    </AppLayout>
  );
};