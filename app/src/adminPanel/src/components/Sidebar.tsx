import React from 'react';
import {
  LayoutDashboard,
  ShoppingBag,
  ClipboardList,
  MapPin,
  Users,
  Shield,
  Settings,
  Leaf,
  ChevronLeft,
  ChevronRight,
  LogOut
} from 'lucide-react';
import { User } from '../types';

interface SidebarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  collapsed: boolean;
  setCollapsed: (c: boolean) => void;
  mobileOpen: boolean;
  setMobileOpen: (o: boolean) => void;
  onLogout: () => void;
  admin: User | null;
}

export const navItems = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'catalog', label: 'Product Catalog', icon: ShoppingBag },
  { id: 'orders', label: 'Order Management', icon: ClipboardList },
  { id: 'nurseries', label: 'Nursery Locations', icon: MapPin },
  { id: 'users', label: 'User Management', icon: Users },
  { id: 'verification', label: 'Seller Verification', icon: Shield },
  { id: 'settings', label: 'Settings', icon: Settings },
];

const Sidebar: React.FC<SidebarProps> = ({ activeTab, setActiveTab, collapsed, setCollapsed, mobileOpen, setMobileOpen, onLogout, admin }) => {
  return (
    <>
      {/* Mobile Overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[45] lg:hidden animate-fade-in"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <aside
        className={`fixed left-0 top-0 h-screen z-50 flex flex-col transition-all duration-300 ease-in-out ${collapsed ? 'w-[72px]' : 'w-[260px]'
          } ${mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}
        style={{
          background: 'linear-gradient(180deg, #0f1a0d 0%, #121212 100%)',
          borderRight: '1px solid rgba(45, 90, 39, 0.15)',
        }}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 px-5 h-[72px] shrink-0">
          <div className="w-10 h-10 rounded-xl bg-verdant-700 flex items-center justify-center glow-green shrink-0">
            <Leaf className="w-5 h-5 text-verdant-300" />
          </div>
          {(!collapsed || mobileOpen) && (
            <div className="overflow-hidden flex-1">
              <h1 className="text-lg font-bold text-white tracking-tight">Verdant</h1>
              <p className="text-[10px] text-verdant-300/60 font-medium uppercase tracking-widest">Admin Panel</p>
            </div>
          )}
          {mobileOpen && (
            <button
              onClick={() => setMobileOpen(false)}
              className="lg:hidden w-8 h-8 rounded-lg flex items-center justify-center text-dark-300 hover:text-white hover:bg-white/5 transition-all"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Nav Items */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const isActive = activeTab === item.id;
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`w-full flex items-center gap-3 px-3 py-3 rounded-xl transition-all duration-200 group cursor-pointer ${isActive
                  ? 'bg-verdant-700/30 text-verdant-300 shadow-lg shadow-verdant-700/10'
                  : 'text-dark-200 hover:text-white hover:bg-white/5'
                  }`}
                title={(collapsed && !mobileOpen) ? item.label : undefined}
              >
                <Icon
                  className={`w-5 h-5 shrink-0 transition-colors ${isActive ? 'text-verdant-300' : 'text-dark-200 group-hover:text-verdant-400'
                    }`}
                />
                {(!collapsed || mobileOpen) && (
                  <span className={`text-sm font-medium truncate ${isActive ? 'text-verdant-200' : ''}`}>
                    {item.label}
                  </span>
                )}
                {isActive && (!collapsed || mobileOpen) && (
                  <div className="ml-auto w-1.5 h-1.5 rounded-full bg-verdant-300" />
                )}
              </button>
            );
          })}
        </nav>

        {/* Collapse Toggle - Hidden on mobile */}
        <div className="px-3 pb-4 hidden lg:block">
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl text-dark-300 hover:text-white hover:bg-white/5 transition-all cursor-pointer"
          >
            {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
            {!collapsed && <span className="text-xs font-medium">Collapse</span>}
          </button>
        </div>

        {/* User Profile Card */}
        {(!collapsed || mobileOpen) ? (
          <div className="mx-3 mb-4 p-3 rounded-xl glass-card flex items-center justify-between group">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-full bg-gradient-to-br from-verdant-700 to-verdant-500 overflow-hidden flex items-center justify-center text-white font-semibold text-sm shrink-0">
                {admin?.profileImageUrl ? (
                  <img src={admin.profileImageUrl} alt={admin.fullName} className="w-full h-full object-cover" />
                ) : (
                  admin?.fullName?.split(' ').map(n => n[0]).join('').toUpperCase() || 'AD'
                )}
              </div>
              <div className="min-w-0">
                <p className="text-sm font-medium text-white truncate">{admin?.fullName || 'Verdant Admin'}</p>
                <p className="text-[11px] text-dark-200 truncate capitalize">{admin?.role?.replace('_', ' ') || 'Super Admin'}</p>
              </div>
            </div>
            <button
              onClick={onLogout}
              className="w-8 h-8 rounded-lg flex items-center justify-center text-dark-300 hover:text-red-400 hover:bg-red-400/10 transition-all cursor-pointer lg:opacity-0 lg:group-hover:opacity-100"
              title="Log Out"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        ) : (
          <div className="flex flex-col items-center mb-4 gap-4">
            <button
              onClick={onLogout}
              className="w-10 h-10 rounded-xl flex items-center justify-center text-dark-300 hover:text-red-400 hover:bg-white/5 transition-all cursor-pointer"
              title="Log Out"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        )}
      </aside>
    </>
  );
};

export default Sidebar;
