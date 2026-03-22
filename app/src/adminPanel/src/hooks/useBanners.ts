import { useState, useEffect } from 'react';
import { db, storage } from '../lib/firebase';
import { 
  collection, 
  query, 
  orderBy, 
  onSnapshot, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  doc, 
  serverTimestamp
} from 'firebase/firestore';
import { 
  ref, 
  uploadBytesResumable, 
  getDownloadURL, 
  deleteObject 
} from 'firebase/storage';
import { Banner } from '../types';

export const useBanners = () => {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const q = query(collection(db, 'banners'), orderBy('order', 'asc'));
    
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const bannerList: Banner[] = [];
      snapshot.forEach((doc) => {
        bannerList.push({ id: doc.id, ...doc.data() } as Banner);
      });
      setBanners(bannerList);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const uploadBanner = async (file: File, metadata: Partial<Banner>) => {
    setUploading(true);
    setProgress(0);

    const storageRef = ref(storage, `banners/${Date.now()}_${file.name}`);
    const uploadTask = uploadBytesResumable(storageRef, file);

    return new Promise<void>((resolve, reject) => {
      uploadTask.on(
        'state_changed',
        (snapshot) => {
          const p = (snapshot.bytesTransferred / snapshot.totalBytes) * 100;
          setProgress(p);
        },
        (error) => {
          setUploading(false);
          reject(error);
        },
        async () => {
          const downloadURL = await getDownloadURL(uploadTask.snapshot.ref);
          await addDoc(collection(db, 'banners'), {
            imageUrl: downloadURL,
            title: metadata.title || '',
            subtitle: metadata.subtitle || '',
            link: metadata.link || '',
            active: metadata.active ?? true,
            order: metadata.order ?? (banners.length + 1),
            createdAt: serverTimestamp(),
          });
          setUploading(false);
          setProgress(0);
          resolve();
        }
      );
    });
  };

  const updateBanner = async (id: string, updates: Partial<Banner>) => {
    const bannerRef = doc(db, 'banners', id);
    await updateDoc(bannerRef, updates);
  };

  const deleteBanner = async (banner: Banner) => {
    try {
      // 1. Delete from Firestore
      await deleteDoc(doc(db, 'banners', banner.id));
      
      // 2. Delete from Storage
      const storageRef = ref(storage, banner.imageUrl);
      await deleteObject(storageRef);
    } catch (error) {
           console.error("Error deleting banner:", error);
           // Even if storage delete fails (e.g. file doesn't exist), we already deleted from firestore
    }
  };

  return {
    banners,
    loading,
    uploading,
    progress,
    uploadBanner,
    updateBanner,
    deleteBanner,
  };
};
