import React, { useState, useEffect, useMemo } from 'react';
import {
  Users,
  Search,
  UserCheck,
  UserX,
  Mail,
  Phone,
  ChevronDown,
  Shield,
  Briefcase,
  RefreshCw,
  Star,
  MapPin,
  Info,
  CreditCard,
  Truck,
  Fingerprint
} from 'lucide-react';
import { collection, query, where, getDocs, doc, updateDoc, Timestamp } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { User, Address, Nursery } from '../types';

interface Seller extends User {
  nursery?: Nursery;
}

const AddressDetails: React.FC<{ title: string; address: Address | undefined; icon: React.ReactNode }> = ({ title, address, icon }) => {
  if (!address) return null;
  return (
    <div className="bg-white/[0.03] rounded-xl p-3 border border-white/5 space-y-2">
      <div className="flex items-center gap-2 mb-1">
        <div className="p-1.5 rounded-lg bg-blue-500/10 text-blue-400">
          {icon}
        </div>
        <h5 className="text-[11px] font-bold uppercase tracking-wider text-dark-200">{title}</h5>
      </div>
      <div className="space-y-1">
        <p className="text-sm font-semibold text-white">{address.firstName} {address.lastName}</p>
        <p className="text-xs text-dark-300 flex items-center gap-1.5">
          <MapPin className="w-3 h-3 text-dark-400" />
          {address.address}, {address.city}, {address.country}
        </p>
        <div className="flex flex-wrap gap-x-4 gap-y-1 mt-2 pt-2 border-t border-white/5">
          <div className="flex items-center gap-1.5 text-[10px] text-dark-400">
            <Mail className="w-3 h-3" />
            {address.email}
          </div>
          <div className="flex items-center gap-1.5 text-[10px] text-dark-400">
            <Phone className="w-3 h-3" />
            {address.phone}
          </div>
          <div className="flex items-center gap-1.5 text-[10px] text-dark-400">
            <Info className="w-3 h-3" />
            {address.postalCode}
          </div>
        </div>
      </div>
    </div>
  );
};

const UserRow: React.FC<{
  user: User;
  onToggleStatus: (userId: string, currentStatus: string) => void;
}> = ({ user, onToggleStatus }) => {
  const isActive = user.status === 'Active' || (!user.status && user.isActive !== false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);

  const handleToggle = async (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsUpdating(true);
    await onToggleStatus(user.userId, isActive ? 'Active' : 'Deactivated');
    setIsUpdating(false);
  };

  return (
    <div
      className={`group rounded-2xl transition-all border ${isExpanded ? 'bg-white/[0.04] border-white/10 ring-1 ring-blue-500/20' : 'hover:bg-white/[0.02] border-transparent hover:border-white/5'} animate-fade-in overflow-hidden cursor-pointer`}
      onClick={() => setIsExpanded(!isExpanded)}
    >
      <div className="flex items-center justify-between p-4">
        <div className="flex items-center gap-4 min-w-0">
          <div className="relative shrink-0">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-blue-600/20 to-dark-800 overflow-hidden flex items-center justify-center border border-white/10 shadow-lg group-hover:scale-105 transition-transform">
              {user.profileImageUrl ? (
                <img src={user.profileImageUrl} alt={user.fullName} className="w-full h-full object-cover" />
              ) : (
                <span className="text-blue-400 font-bold text-lg">
                  {user.fullName?.charAt(0).toUpperCase() || '?'}
                </span>
              )}
            </div>
            <div className={`absolute -bottom-1 -right-1 w-4 h-4 rounded-full border-2 border-dark-950 flex items-center justify-center ${isActive ? 'bg-emerald-500 shadow-emerald-500/50' : 'bg-red-500 shadow-red-500/50'} shadow-sm`}>
              {isActive ? <UserCheck className="w-2 h-2 text-white" /> : <UserX className="w-2 h-2 text-white" />}
            </div>
          </div>

          <div className="min-w-0">
            <h4 className="text-sm font-semibold text-white truncate group-hover:text-blue-400 transition-colors">
              {user.fullName}
            </h4>
            <div className="flex items-center gap-3 mt-1 text-[11px] text-dark-300">
              <div className="flex items-center gap-1">
                <Mail className="w-3 h-3" />
                <span className="truncate max-w-[120px]">{user.email}</span>
              </div>
              <div className="flex items-center gap-1">
                <Phone className="w-3 h-3" />
                <span>{user.phone || 'N/A'}</span>
              </div>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="hidden sm:flex flex-col items-end px-3 border-r border-white/5 mr-1">
            <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full ${isActive ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'}`}>
              {isActive ? 'Active' : 'Deactivated'}
            </span>
            <span className="text-[10px] text-dark-400 mt-1">
              {user.createdAt instanceof Timestamp ? user.createdAt.toDate().toLocaleDateString() : 'N/A'}
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleToggle}
              disabled={isUpdating}
              className={`w-9 h-9 rounded-xl flex items-center justify-center transition-all ${isActive
                ? 'bg-red-500/10 text-red-400 hover:bg-red-500/20'
                : 'bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20'
                } ${isUpdating ? 'opacity-50 cursor-wait' : 'cursor-pointer'}`}
              title={isActive ? 'Deactivate Account' : 'Activate Account'}
            >
              {isUpdating ? (
                <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
              ) : isActive ? (
                <UserX className="w-4.5 h-4.5" />
              ) : (
                <UserCheck className="w-4.5 h-4.5" />
              )}
            </button>
            <div className={`p-1 rounded-lg transition-transform duration-300 ${isExpanded ? 'rotate-180 bg-white/5' : 'text-dark-400 group-hover:text-white'}`}>
              <ChevronDown className="w-4 h-4" />
            </div>
          </div>
        </div>
      </div>

      {isExpanded && (
        <div className="px-4 pb-4 pt-2 border-t border-white/5 grid grid-cols-1 md:grid-cols-2 gap-4 animate-slide-down">
          <AddressDetails
            title="Billing Address"
            address={user.billing}
            icon={<CreditCard className="w-3.5 h-3.5" />}
          />
          <AddressDetails
            title="Shipping Address"
            address={user.sameAsBilling ? user.billing : user.shipping}
            icon={<Truck className="w-3.5 h-3.5" />}
          />

          <div className="md:col-span-2 bg-white/[0.02] rounded-xl p-3 border border-white/5 flex flex-wrap gap-4 items-center justify-between">
            <div className="flex gap-4">
              <div className="flex items-center gap-2">
                <div className={`p-1.5 rounded-lg ${user.biometricEnabled ? 'bg-emerald-500/10 text-emerald-400' : 'bg-dark-800 text-dark-400'}`}>
                  <Fingerprint className="w-3.5 h-3.5" />
                </div>
                <div className="text-[10px] font-bold uppercase tracking-wider text-dark-300">Biometrics</div>
              </div>
              <div className="flex items-center gap-2">
                <div className={`p-1.5 rounded-lg ${user.fcmToken ? 'bg-blue-500/10 text-blue-400' : 'bg-dark-800 text-dark-400'}`}>
                  <Shield className="w-3.5 h-3.5" />
                </div>
                <div className="text-[10px] font-bold uppercase tracking-wider text-dark-300">Notifications</div>
              </div>
            </div>

            <div className="text-[10px] text-dark-400 font-medium italic">
              User ID: <span className="font-mono text-white/50">{user.userId}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const SellerRow: React.FC<{
  seller: Seller;
  onToggleStatus: (userId: string, currentStatus: string) => void;
}> = ({ seller, onToggleStatus }) => {
  const isActive = seller.status === 'Active' || (!seller.status && seller.isActive !== false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);

  const handleToggle = async (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsUpdating(true);
    await onToggleStatus(seller.userId, isActive ? 'Active' : 'Deactivated');
    setIsUpdating(false);
  };

  return (
    <div
      className={`group rounded-2xl transition-all border ${isExpanded ? 'bg-verdant-900/10 border-verdant-500/20 ring-1 ring-verdant-500/20' : 'hover:bg-white/[0.02] border-transparent hover:border-white/5'} animate-fade-in relative overflow-hidden cursor-pointer`}
      onClick={() => setIsExpanded(!isExpanded)}
    >
      {/* Background Glow Effect */}
      <div className={`absolute top-0 right-0 w-32 h-32 bg-verdant-500/5 blur-[40px] rounded-full pointer-events-none -translate-y-16 translate-x-16 transition-opacity ${isExpanded ? 'opacity-100' : 'opacity-0'}`} />

      <div className="flex items-center justify-between p-4 relative z-10">
        <div className="flex items-center gap-4 min-w-0">
          <div className="relative shrink-0">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-verdant-700/30 to-dark-800 overflow-hidden flex items-center justify-center border border-white/10 shadow-xl group-hover:border-verdant-500/30 transition-all group-hover:scale-105">
              {seller.profileImageUrl ? (
                <img src={seller.profileImageUrl} alt={seller.fullName} className="w-full h-full object-cover" />
              ) : (
                <span className="text-verdant-300 font-bold text-xl">
                  {seller.fullName?.charAt(0).toUpperCase() || '?'}
                </span>
              )}
            </div>
            <div className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-full border-2 border-dark-950 flex items-center justify-center ${isActive ? 'bg-emerald-500 shadow-emerald-500/50' : 'bg-red-500 shadow-red-500/50'} shadow-md`}>
              {isActive ? <UserCheck className="w-2.5 h-2.5 text-white" /> : <UserX className="w-2.5 h-2.5 text-white" />}
            </div>
          </div>

          <div className="min-w-0">
            <div className="flex items-center gap-2 mb-0.5">
              <h4 className="text-sm font-bold text-white truncate group-hover:text-verdant-300 transition-colors">
                {seller.fullName}
              </h4>
              {seller.nursery && (
                <span className="flex items-center gap-1 bg-verdant-500/10 text-verdant-400 text-[10px] font-bold px-2 py-0.5 rounded-full border border-verdant-500/20">
                  <Star className="w-2.5 h-2.5 fill-current" />
                  {seller.nursery.ratingAverage?.toFixed(1) || 'N/A'}
                </span>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <div className="flex items-center gap-3 text-[11px] text-dark-300">
                <div className="flex items-center gap-1">
                  <Mail className="w-3 h-3 text-dark-400" />
                  <span className="truncate max-w-[120px]">{seller.email}</span>
                </div>
                {seller.nursery && (
                  <div className="flex items-center gap-1 text-verdant-300/80">
                    <Briefcase className="w-3 h-3" />
                    <span className="truncate font-medium">{seller.nursery.nurseryName}</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="hidden md:flex flex-col items-end px-4 border-r border-white/5 mr-1">
            <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full ${isActive ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'}`}>
              {isActive ? 'Active Seller' : 'Deactivated'}
            </span>
            {seller.nursery && (
              <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full mt-1.5 border ${seller.nursery.bankVerified ? 'bg-blue-500/10 text-blue-400 border-blue-500/20' : 'bg-amber-500/10 text-amber-400 border-amber-500/20'}`}>
                {seller.nursery.bankVerified ? 'Verified' : 'Pending Verification'}
              </span>
            )}
            <span className="text-[10px] text-dark-400 mt-1.5 font-medium">
              Since {seller.createdAt instanceof Timestamp ? seller.createdAt.toDate().toLocaleDateString('en-US', { month: 'short', year: 'numeric' }) : 'N/A'}
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleToggle}
              disabled={isUpdating}
              className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all ${isActive
                ? 'bg-red-500/10 text-red-400 hover:bg-red-500/20 hover:scale-105 active:scale-95'
                : 'bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 hover:scale-105 active:scale-95'
                } ${isUpdating ? 'opacity-50 cursor-wait' : 'cursor-pointer shadow-lg active:shadow-inner'}`}
              title={isActive ? 'Deactivate Seller' : 'Activate Seller'}
            >
              {isUpdating ? (
                <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
              ) : isActive ? (
                <UserX className="w-5 h-5" />
              ) : (
                <UserCheck className="w-5 h-5" />
              )}
            </button>
            <div className={`p-1 rounded-lg transition-transform duration-300 ${isExpanded ? 'rotate-180 bg-verdant-500/10 text-verdant-400' : 'text-dark-400 group-hover:text-white'}`}>
              <ChevronDown className="w-4 h-4" />
            </div>
          </div>
        </div>
      </div>

      {isExpanded && (
        <div className="px-4 pb-4 pt-2 border-t border-white/5 grid grid-cols-1 md:grid-cols-2 gap-4 animate-slide-down relative z-10">
          <div className="bg-white/[0.03] rounded-xl p-3 border border-white/5 space-y-3">
            <div className="flex items-center gap-2 mb-1">
              <div className="p-1.5 rounded-lg bg-verdant-500/10 text-verdant-400">
                <Briefcase className="w-3.5 h-3.5" />
              </div>
              <h5 className="text-[11px] font-bold uppercase tracking-wider text-dark-200">Nursery Details</h5>
            </div>
            {seller.nursery ? (
              <div className="space-y-2">
                <p className="text-sm font-bold text-white">{seller.nursery.nurseryName}</p>
                <p className="text-xs text-dark-300 leading-relaxed">{seller.nursery.description}</p>
                <div className="flex items-center gap-2 mt-2">
                  <div className="flex items-center gap-1 text-[10px] text-dark-400 italic">
                    <MapPin className="w-3 h-3 text-dark-500" />
                    <span>{seller.nursery.latitude?.toFixed(4)}, {seller.nursery.longitude?.toFixed(4)}</span>
                  </div>
                  <div className="flex items-center gap-1 text-[10px] text-verdant-400 bg-verdant-500/5 px-2 py-0.5 rounded-full">
                    <Star className="w-2.5 h-2.5 fill-current" />
                    {seller.nursery.totalReviews} Reviews
                  </div>
                </div>
              </div>
            ) : (
              <p className="text-xs text-dark-500 italic">No nursery linked to this account.</p>
            )}
          </div>

          <div className="bg-white/[0.03] rounded-xl p-3 border border-white/5 space-y-3">
            <div className="flex items-center gap-2 mb-1">
              <div className="p-1.5 rounded-lg bg-blue-500/10 text-blue-400">
                <Shield className="w-3.5 h-3.5" />
              </div>
              <h5 className="text-[11px] font-bold uppercase tracking-wider text-dark-200">Account security</h5>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className={`p-2 rounded-lg border ${seller.biometricEnabled ? 'bg-emerald-500/5 border-emerald-500/10' : 'bg-dark-800 border-white/5'}`}>
                <div className={`flex items-center gap-2 mb-1 ${seller.biometricEnabled ? 'text-emerald-400' : 'text-dark-400'}`}>
                  <Fingerprint className="w-3 h-3" />
                  <span className="text-[10px] font-bold uppercase">Biometrics</span>
                </div>
                <div className="text-[9px] text-dark-300 font-medium">{seller.biometricEnabled ? 'Enabled' : 'Disabled'}</div>
              </div>
              <div className={`p-2 rounded-lg border ${seller.fcmToken ? 'bg-blue-500/5 border-blue-500/10' : 'bg-dark-800 border-white/5'}`}>
                <div className={`flex items-center gap-2 mb-1 ${seller.fcmToken ? 'text-blue-400' : 'text-dark-400'}`}>
                  <Shield className="w-3 h-3" />
                  <span className="text-[10px] font-bold uppercase">FCM Sync</span>
                </div>
                <div className="text-[9px] text-dark-300 font-medium truncate">{seller.fcmToken ? 'Connected' : 'Disconnected'}</div>
              </div>
            </div>
            <div className="pt-2 border-t border-white/5">
              <p className="text-[10px] text-dark-400 flex items-center justify-between">
                <span>User ID:</span>
                <span className="font-mono text-white/40">{seller.userId}</span>
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const UserManagement: React.FC = () => {
  const [sellers, setSellers] = useState<Seller[]>([]);
  const [customers, setCustomers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [sellerSearch, setSellerSearch] = useState('');
  const [customerSearch, setCustomerSearch] = useState('');

  // Load users from Firestore
  const loadUsers = async () => {
    try {
      setLoading(true);

      // Fetch Sellers
      const sellersQuery = query(
        collection(db, "users"),
        where("role", "==", "seller")
      );
      const sellersSnapshot = await getDocs(sellersQuery);
      const sellersData = sellersSnapshot.docs.map(doc => ({
        userId: doc.id,
        ...doc.data()
      })) as User[];

      // Fetch Nurseries for sellers
      const sellerIds = sellersData.map(s => s.userId);
      let nurseries: Record<string, Nursery> = {};

      if (sellerIds.length > 0) {
        const nurseriesQuery = query(
          collection(db, "nurseries"),
          where("ownerId", "in", sellerIds)
        );
        const nurseriesSnapshot = await getDocs(nurseriesQuery);
        nurseries = nurseriesSnapshot.docs.reduce((acc, doc) => {
          const nursery = doc.data() as Nursery;
          acc[nursery.ownerId] = nursery;
          return acc;
        }, {} as Record<string, Nursery>);
      }

      const sellersWithNurseries: Seller[] = sellersData.map(seller => ({
        ...seller,
        nursery: nurseries[seller.userId]
      }));
      setSellers(sellersWithNurseries);

      // Fetch Customers
      const customersQuery = query(
        collection(db, "users"),
        where("role", "==", "customer")
      );
      const customersSnapshot = await getDocs(customersQuery);
      const customersData = customersSnapshot.docs.map(doc => ({
        userId: doc.id,
        ...doc.data()
      })) as User[];
      setCustomers(customersData);
    } catch (err) {
      console.error("Error loading users:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const toggleUserStatus = async (userId: string, currentStatus: string) => {
    try {
      const userRef = doc(db, "users", userId);
      const newStatus = currentStatus === 'Active' ? 'Deactivated' : 'Active';
      await updateDoc(userRef, {
        status: newStatus,
        isActive: newStatus === 'Active'
      });

      // Update local state
      setSellers(prev => prev.map(s =>
        s.userId === userId ? { ...s, status: newStatus, isActive: newStatus === 'Active' } : s
      ));
      setCustomers(prev => prev.map(c =>
        c.userId === userId ? { ...c, status: newStatus, isActive: newStatus === 'Active' } : c
      ));
    } catch (err) {
      console.error("Error toggling user status:", err);
    }
  };

  const filteredSellers = useMemo(() => {
    return sellers.filter(s =>
      s.fullName?.toLowerCase().includes(sellerSearch.toLowerCase()) ||
      s.email?.toLowerCase().includes(sellerSearch.toLowerCase()) ||
      s.nursery?.nurseryName?.toLowerCase().includes(sellerSearch.toLowerCase()) ||
      s.phone?.includes(sellerSearch)
    );
  }, [sellers, sellerSearch]);

  const filteredCustomers = useMemo(() => {
    return customers.filter(u =>
      u.fullName?.toLowerCase().includes(customerSearch.toLowerCase()) ||
      u.email?.toLowerCase().includes(customerSearch.toLowerCase()) ||
      u.phone?.includes(customerSearch) ||
      u.billing?.city?.toLowerCase().includes(customerSearch.toLowerCase())
    );
  }, [customers, customerSearch]);

  return (
    <div className="space-y-6">
      {/* Sellers Section */}
      <section className="glass-card rounded-2xl overflow-hidden animate-slide-up bg-gradient-to-br from-dark-900 via-dark-950 to-dark-900 border border-white/5 shadow-xl" style={{ animationDelay: '100ms' }}>
        <div className="p-6 border-b border-white/5 flex flex-col md:flex-row md:items-center justify-between gap-4 relative overflow-hidden">
          {/* Subtle Decorative Header Background */}
          <div className="absolute top-0 left-0 w-full h-full bg-verdant-600/5 -rotate-6 scale-150 pointer-events-none" />

          <div className="flex items-center gap-4 relative z-10">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-verdant-600/20 to-verdant-800/20 flex items-center justify-center border border-verdant-500/20 shadow-inner">
              <Briefcase className="w-6 h-6 text-verdant-400" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white font-display tracking-tight">Nursery Sellers</h3>
              <p className="text-xs text-dark-300 font-medium">Verified platform nursery owners & partners</p>
            </div>
          </div>

          <div className="relative w-full md:w-80 group z-10">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4.5 h-4.5 text-dark-400 group-focus-within:text-verdant-400 transition-colors" />
            <input
              type="text"
              placeholder="Search by name, email or nursery..."
              value={sellerSearch}
              onChange={(e) => setSellerSearch(e.target.value)}
              className="w-full bg-dark-800/50 border border-white/10 rounded-xl py-2.5 pl-11 pr-4 text-sm text-white focus:outline-none focus:border-verdant-500/50 focus:ring-4 focus:ring-verdant-500/5 transition-all placeholder:text-dark-500"
            />
          </div>
        </div>

        <div className="p-3 min-h-[460px] space-y-2">
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <div className="w-8 h-8 border-4 border-verdant-500/20 border-t-verdant-500 rounded-full animate-spin" />
            </div>
          ) : filteredSellers.length > 0 ? (
            filteredSellers.map(seller => (
              <SellerRow
                key={seller.userId}
                seller={seller}
                onToggleStatus={toggleUserStatus}
              />
            ))
          ) : (
            <div className="py-20 flex flex-col items-center justify-center text-center">
              <div className="w-16 h-16 rounded-full bg-dark-800 flex items-center justify-center mb-4">
                <Users className="w-8 h-8 text-dark-500" />
              </div>
              <p className="text-base font-semibold text-white">No sellers found</p>
              <p className="text-xs text-dark-400 mt-1">No sellers match your search</p>
            </div>
          )}
        </div>

        {/* Sellers Footer */}
        <div className="p-4 px-6 border-t border-white/5 bg-white/[0.01]">
          <div className="flex items-center justify-between">
            <div className="text-xs text-dark-400 font-medium">
              Displaying <span className="text-white font-bold">{filteredSellers.length}</span> of <span className="text-white font-bold">{sellers.length}</span> sellers
            </div>
            <button
              onClick={loadUsers}
              disabled={loading}
              className="p-2.5 rounded-xl bg-dark-800 border border-white/5 text-dark-300 hover:text-white hover:bg-dark-700 disabled:opacity-30 transition-all"
              title="Reload"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </section>

      {/* Customers Section */}
      <section className="glass-card rounded-2xl overflow-hidden animate-slide-up bg-dark-950 border border-white/5 shadow-2xl" style={{ animationDelay: '200ms' }}>
        <div className="p-6 border-b border-white/5 flex flex-col md:flex-row md:items-center justify-between gap-4 relative">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-blue-500/10 flex items-center justify-center border border-blue-500/20 shadow-inner">
              <Users className="w-6 h-6 text-blue-400" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white font-display tracking-tight">Store Customers</h3>
              <p className="text-xs text-dark-300 font-medium">Registered mobile app users & shoppers</p>
            </div>
          </div>

          <div className="relative w-full md:w-80 group">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4.5 h-4.5 text-dark-400 group-focus-within:text-blue-400 transition-colors" />
            <input
              type="text"
              placeholder="Search by name, email or city..."
              value={customerSearch}
              onChange={(e) => setCustomerSearch(e.target.value)}
              className="w-full bg-dark-800/50 border border-white/10 rounded-xl py-2.5 pl-11 pr-4 text-sm text-white focus:outline-none focus:border-blue-500/50 focus:ring-4 focus:ring-blue-500/5 transition-all placeholder:text-dark-500"
            />
          </div>
        </div>

        <div className="p-3 min-h-[460px] space-y-2">
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <div className="w-8 h-8 border-4 border-blue-500/20 border-t-blue-500 rounded-full animate-spin" />
            </div>
          ) : filteredCustomers.length > 0 ? (
            filteredCustomers.map(user => (
              <UserRow
                key={user.userId}
                user={user}
                onToggleStatus={toggleUserStatus}
              />
            ))
          ) : (
            <div className="h-[460px] flex flex-col items-center justify-center text-center">
              <div className="w-16 h-16 rounded-full bg-dark-800 flex items-center justify-center mb-4">
                <Users className="w-8 h-8 text-dark-400" />
              </div>
              <p className="text-base font-semibold text-white">No customers found</p>
              <p className="text-xs text-dark-400 mt-1">No customers match your search</p>
            </div>
          )}
        </div>

        {/* Customers Footer */}
        <div className="p-4 px-6 border-t border-white/5 bg-white/[0.01]">
          <div className="flex items-center justify-between">
            <div className="text-xs text-dark-400 font-medium">
              Displaying <span className="text-white font-bold">{filteredCustomers.length}</span> of <span className="text-white font-bold">{customers.length}</span> customers
            </div>
            <button
              onClick={loadUsers}
              disabled={loading}
              className="p-2.5 rounded-xl bg-dark-800 border border-white/5 text-dark-300 hover:text-white hover:bg-dark-700 disabled:opacity-30 transition-all"
              title="Reload"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </section>

      {/* Security Tip Overlay */}
      <div className="p-5 rounded-2xl bg-gradient-to-r from-amber-500/5 to-amber-600/5 border border-amber-500/10 flex items-start gap-4 shadow-lg group">
        <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center shrink-0 border border-amber-500/20 group-hover:scale-110 transition-transform">
          <Shield className="w-5 h-5 text-amber-500" />
        </div>
        <div>
          <h4 className="text-sm font-bold text-amber-500 font-display">Administrative Security Note</h4>
          <p className="text-xs text-dark-300 mt-1 leading-relaxed max-w-2xl font-medium">
            Deactivating a user account will immediately revoke their session and access to the Verdant platform.
            <span className="text-amber-500/80"> Nursery owners with deactivated accounts cannot manage inventory, fulfill orders, or access financial data.</span>
            Always verify identity before account reactivation.
          </p>
        </div>
      </div>
    </div>
  );
};

export default UserManagement;
