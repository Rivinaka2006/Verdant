import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { getFirestore } from "firebase/firestore";
import { getAuth } from "firebase/auth";
import { getStorage } from "firebase/storage";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyB30cMDBPhyHFRzhb4q57lI2UcUMQlice0",
  authDomain: "verdant-bbf83.firebaseapp.com",
  projectId: "verdant-bbf83",
  storageBucket: "verdant-bbf83.firebasestorage.app",
  messagingSenderId: "742645884353",
  appId: "1:742645884353:web:c2d4be4ebe6e5305c47b40",
  measurementId: "G-HQC9NHKK1L"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = typeof window !== "undefined" ? getAnalytics(app) : null;
const db = getFirestore(app);
const auth = getAuth(app);
const storage = getStorage(app);

export { app, analytics, db, auth, storage };

