import React from 'react';
import Card from '../ui/Card';
import Button from '../ui/Button';

interface QuickAction {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  href: string;
  variant?: 'primary' | 'secondary' | 'outline';
}

interface QuickActionsProps {
  showHeader?: boolean;
  className?: string;
}

const QuickActions: React.FC<QuickActionsProps> = ({
  showHeader = true,
  className = '',
}) => {
  const quickActions: QuickAction[] = [
    {
      id: 'create-product',
      title: 'Add Product',
      description: 'Create new product listing',
      href: '/admin/products/create',
      variant: 'primary',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
        </svg>
      ),
    },
    {
      id: 'create-user',
      title: 'Add User',
      description: 'Create new user account',
      href: '/admin/users/create',
      variant: 'secondary',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
      ),
    },
    {
      id: 'pending-orders',
      title: 'Review Orders',
      description: 'Check pending orders',
      href: '/admin/orders/pending',
      variant: 'outline',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
        </svg>
      ),
    },
    {
      id: 'manage-users',
      title: 'Manage Users',
      description: 'View and edit users',
      href: '/admin/users',
      variant: 'outline',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
        </svg>
      ),
    },
    {
      id: 'manage-products',
      title: 'Manage Products',
      description: 'View and edit products',
      href: '/admin/products',
      variant: 'outline',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
        </svg>
      ),
    },
    {
      id: 'export-data',
      title: 'Export Data',
      description: 'Download reports',
      href: '/admin/export',
      variant: 'outline',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      ),
    },
  ];

  const handleActionClick = (href: string) => {
    window.location.href = href;
  };

  return (
    <Card className={`p-6 ${className}`}>
      {showHeader && (
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
      )}
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {quickActions.map((action) => (
          <Button
            key={action.id}
            variant={action.variant}
            onClick={() => handleActionClick(action.href)}
            className="h-auto p-4 text-left justify-start flex-col items-start space-y-2"
          >
            <div className="flex items-center space-x-2 w-full">
              {action.icon}
              <span className="font-medium">{action.title}</span>
            </div>
            <span className="text-sm opacity-75 font-normal">
              {action.description}
            </span>
          </Button>
        ))}
      </div>
      
      <div className="mt-6 pt-4 border-t border-gray-200">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-2 sm:space-y-0">
          <p className="text-sm text-gray-600">
            Need help? Check the admin guide.
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={() => window.location.href = '/admin/help'}
            className="self-start sm:self-auto"
          >
            View Documentation
          </Button>
        </div>
      </div>
    </Card>
  );
};

export default QuickActions;