import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import type { ReactNode } from 'react';
import type { UserRole } from '../../types';

interface ProtectedRouteProps {
  children: ReactNode;
  requireAuth?: boolean;
  requiredRoles?: UserRole[];
  fallback?: ReactNode;
  redirectTo?: string;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireAuth = true,
  requiredRoles = [],
  fallback,
  redirectTo = '/login',
}) => {
  const { isAuthenticated, user, isLoading } = useAuth();
  const location = useLocation();

  // Show loading state while authentication is being checked
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="flex flex-col items-center space-y-4">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  // Check authentication requirement
  if (requireAuth && !isAuthenticated) {
    // Redirect to login with the current location for redirect after login
    return (
      <Navigate 
        to={redirectTo} 
        state={{ from: location }} 
        replace 
      />
    );
  }

  // Check role-based access
  if (requireAuth && isAuthenticated && requiredRoles.length > 0 && user) {
    const userRoles = user.roles.map(role => role.name);
    const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role));
    
    if (!hasRequiredRole) {
      // User doesn't have required role
      if (fallback) {
        return <>{fallback}</>;
      }
      
      // Redirect to unauthorized page or home based on user role
      const unauthorizedRedirect = getUserRoleBasedRedirect(userRoles);
      return <Navigate to={unauthorizedRedirect} replace />;
    }
  }

  // If we get here, user has access
  return <>{children}</>;
};

// Helper function to determine redirect based on user roles
const getUserRoleBasedRedirect = (userRoles: string[]): string => {
  if (userRoles.includes('ADMIN')) {
    return '/admin';
  }
  if (userRoles.includes('PRODUCT_MANAGER')) {
    return '/pm';
  }
  if (userRoles.includes('CUSTOMER')) {
    return '/';
  }
  return '/';
};

// Convenience components for specific role requirements
export const AdminRoute: React.FC<{ children: ReactNode }> = ({ children }) => (
  <ProtectedRoute requiredRoles={['ADMIN']}>
    {children}
  </ProtectedRoute>
);

export const ProductManagerRoute: React.FC<{ children: ReactNode }> = ({ children }) => (
  <ProtectedRoute requiredRoles={['PRODUCT_MANAGER', 'ADMIN']}>
    {children}
  </ProtectedRoute>
);

export const CustomerRoute: React.FC<{ children: ReactNode }> = ({ children }) => (
  <ProtectedRoute requiredRoles={['CUSTOMER', 'ADMIN']}>
    {children}
  </ProtectedRoute>
);

// Component to protect content that should only be visible to authenticated users
export const AuthenticatedOnly: React.FC<{ 
  children: ReactNode;
  fallback?: ReactNode;
}> = ({ children, fallback }) => {
  const { isAuthenticated } = useAuth();
  
  if (!isAuthenticated) {
    return fallback ? <>{fallback}</> : null;
  }
  
  return <>{children}</>;
};

// Component to show content only to unauthenticated users
export const UnauthenticatedOnly: React.FC<{ 
  children: ReactNode;
  fallback?: ReactNode;
}> = ({ children, fallback }) => {
  const { isAuthenticated } = useAuth();
  
  if (isAuthenticated) {
    return fallback ? <>{fallback}</> : null;
  }
  
  return <>{children}</>;
};

export default ProtectedRoute;