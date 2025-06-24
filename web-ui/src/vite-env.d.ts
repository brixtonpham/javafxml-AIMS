/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_WS_URL: string
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_VERSION: string
  readonly VITE_ENABLE_DEV_TOOLS: string
  readonly VITE_ENABLE_LOGGING: string
  readonly VITE_VNPAY_TMN_CODE?: string
  readonly VITE_VNPAY_HASH_SECRET?: string
  readonly VITE_VNPAY_URL?: string
  readonly VITE_VNPAY_RETURN_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

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
