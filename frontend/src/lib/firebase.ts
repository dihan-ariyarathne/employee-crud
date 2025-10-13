import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';

// Frontend env values — fill these in frontend/.env
// VITE_FIREBASE_API_KEY=...
// VITE_FIREBASE_AUTH_DOMAIN=...
// VITE_FIREBASE_PROJECT_ID=...
// VITE_FIREBASE_APP_ID=...

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
} as const;

let app: ReturnType<typeof initializeApp> | null = null;
try {
  if (
    firebaseConfig.apiKey &&
    firebaseConfig.authDomain &&
    firebaseConfig.projectId &&
    firebaseConfig.appId
  ) {
    app = initializeApp(firebaseConfig);
  } else {
    // eslint-disable-next-line no-console
    console.warn('Firebase config missing; auth disabled in this build');
  }
} catch (e) {
  // eslint-disable-next-line no-console
  console.warn('Failed to initialize Firebase; continuing without auth:', (e as Error).message);
  app = null;
}

// Export auth if initialized; else, export a shim with currentUser=null so callers can proceed.
export const auth: any = app ? getAuth(app) : { currentUser: null };
export default app;
