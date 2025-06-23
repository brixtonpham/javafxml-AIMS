/// <reference types="vite/client" />

// Global window extensions for error reporting and notifications
declare global {
  interface Window {
    errorReporting?: {
      captureException: (error: any) => void;
    };
    showUpdateNotification?: () => void;
  }
}

export {};
