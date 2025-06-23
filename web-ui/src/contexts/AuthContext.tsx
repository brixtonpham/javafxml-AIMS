import React, { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { authService } from '../services/authService';
import type { AuthState } from '../types';

interface AuthContextType extends AuthState {
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: true,
  });

  // Initialize authentication state on app load
  useEffect(() => {
    const initAuth = async () => {
      try {
        setAuthState(prev => ({ ...prev, isLoading: true }));
        
        const storedToken = authService.getStoredToken();
        const storedUser = authService.getStoredUser();

        if (storedToken && storedUser) {
          // Validate the stored session
          const validatedUser = await authService.validateSession();
          
          if (validatedUser) {
            setAuthState({
              user: validatedUser,
              token: storedToken,
              isAuthenticated: true,
              isLoading: false,
            });
          } else {
            // Session invalid, clear stored data
            authService.clearAuthData();
            setAuthState({
              user: null,
              token: null,
              isAuthenticated: false,
              isLoading: false,
            });
          }
        } else {
          setAuthState({
            user: null,
            token: null,
            isAuthenticated: false,
            isLoading: false,
          });
        }
      } catch (error) {
        console.error('Auth initialization error:', error);
        authService.clearAuthData();
        setAuthState({
          user: null,
          token: null,
          isAuthenticated: false,
          isLoading: false,
        });
      }
    };

    initAuth();
  }, []);

  // Set up automatic token refresh
  useEffect(() => {
    if (!authState.isAuthenticated || !authState.token) {
      return;
    }

    // Refresh token every 23 hours (1 hour before expiry)
    const refreshInterval = setInterval(() => {
      refreshToken().catch(error => {
        console.error('Auto token refresh failed:', error);
        logout();
      });
    }, 23 * 60 * 60 * 1000); // 23 hours

    return () => clearInterval(refreshInterval);
  }, [authState.isAuthenticated, authState.token]);

  const login = async (username: string, password: string): Promise<void> => {
    try {
      setAuthState(prev => ({ ...prev, isLoading: true }));
      
      const authResponse = await authService.login({ username, password });
      
      setAuthState({
        user: authResponse.user,
        token: authResponse.token,
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      setAuthState(prev => ({ ...prev, isLoading: false }));
      throw error;
    }
  };

  const logout = async (): Promise<void> => {
    try {
      setAuthState(prev => ({ ...prev, isLoading: true }));
      
      await authService.logout();
      
      setAuthState({
        user: null,
        token: null,
        isAuthenticated: false,
        isLoading: false,
      });
    } catch (error) {
      // Even if logout API call fails, clear local state
      console.error('Logout error:', error);
      authService.clearAuthData();
      setAuthState({
        user: null,
        token: null,
        isAuthenticated: false,
        isLoading: false,
      });
    }
  };

  const refreshToken = async (): Promise<void> => {
    try {
      const currentUser = await authService.getCurrentUser();
      setAuthState(prev => ({
        ...prev,
        user: currentUser,
      }));
    } catch (error) {
      console.error('Token refresh failed:', error);
      await logout();
      throw error;
    }
  };

  const contextValue: AuthContextType = {
    ...authState,
    login,
    logout,
    refreshToken,
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};