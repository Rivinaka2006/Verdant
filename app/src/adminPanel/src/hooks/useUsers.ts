import { useState, useEffect } from 'react';
import { 
  collection, 
  query, 
  where, 
  getDocs, 
  doc, 
  updateDoc,
  orderBy,
  limit,
  startAfter,
  getCountFromServer,
  QueryDocumentSnapshot,
  DocumentData
} from 'firebase/firestore';
import { db } from '../lib/firebase';
import { User } from '../types';

export const useUsers = (role: string, pageSize: number = 10) => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastVisible, setLastVisible] = useState<QueryDocumentSnapshot<DocumentData> | null>(null);
  const [cursorStack, setCursorStack] = useState<(QueryDocumentSnapshot<DocumentData> | null)[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);

  const fetchUsers = async (direction: 'next' | 'prev' | 'first' = 'first') => {
    try {
      setLoading(true);
      setError(null);

      // Get count if not already done
      if (totalCount === 0) {
        const countQuery = query(
          collection(db, "users"),
          where("role", "==", role)
        );
        const countSnapshot = await getCountFromServer(countQuery);
        setTotalCount(countSnapshot.data().count);
      }

      let usersQuery;
      let newPage = page;
      let newStack = [...cursorStack];

      if (direction === 'first') {
        usersQuery = query(
          collection(db, "users"),
          where("role", "==", role),
          orderBy("fullName"),
          limit(pageSize)
        );
        newPage = 1;
        newStack = [];
      } else if (direction === 'next' && lastVisible) {
        usersQuery = query(
          collection(db, "users"),
          where("role", "==", role),
          orderBy("fullName"),
          startAfter(lastVisible),
          limit(pageSize)
        );
        newStack.push(lastVisible);
        newPage = page + 1;
      } else if (direction === 'prev' && cursorStack.length > 0) {
        const prevCursor = cursorStack.length > 1 ? cursorStack[cursorStack.length - 2] : null;
        
        if (prevCursor) {
            usersQuery = query(
              collection(db, "users"),
              where("role", "==", role),
              orderBy("fullName"),
              startAfter(prevCursor),
              limit(pageSize)
            );
        } else {
            usersQuery = query(
              collection(db, "users"),
              where("role", "==", role),
              orderBy("fullName"),
              limit(pageSize)
            );
        }
        
        newStack.pop();
        newPage = page - 1;
      } else {
        setLoading(false);
        return;
      }

      const snapshot = await getDocs(usersQuery);
      const newUsers = snapshot.docs.map(doc => ({
        ...doc.data(),
        userId: doc.id
      })) as User[];

      setUsers(newUsers);
      setLastVisible(snapshot.docs[snapshot.docs.length - 1] || null);
      setCursorStack(newStack);
      setPage(newPage);
      setHasMore(snapshot.docs.length === pageSize);
    } catch (err: any) {
      console.error(`Error fetching users (${role}):`, err);
      setError(err.message || "Failed to load users");
    } finally {
      setLoading(false);
    }
  };

  const toggleUserStatus = async (userId: string, currentStatus: string) => {
    try {
      const userRef = doc(db, "users", userId);
      const newStatus = currentStatus === 'Active' ? 'Deactivated' : 'Active';
      await updateDoc(userRef, {
        status: newStatus,
        isActive: newStatus === 'Active'
      });
      
      setUsers(prev => prev.map(u => 
        u.userId === userId ? { ...u, status: newStatus, isActive: newStatus === 'Active' } : u
      ));
      return true;
    } catch (err: any) {
      console.error("Error toggling user status:", err);
      setError(err.message || "Failed to update user status");
      return false;
    }
  };

  useEffect(() => {
    fetchUsers('first');
  }, [role, pageSize]);

  return { users, loading, error, hasMore, fetchUsers, toggleUserStatus, totalCount, page };
};
