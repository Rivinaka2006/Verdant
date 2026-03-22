import React, { useState, useEffect, useRef } from 'react';
import { Search, Bell, ChevronDown, Sun, Maximize2, Menu, MapPin, Terminal } from 'lucide-react';
import { User } from '../types';
import { navItems } from './Sidebar';

interface TopBarProps {
  sidebarCollapsed: boolean;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  setMobileSidebarOpen: (o: boolean) => void;
  admin: User | null;
  topNurseries?: { name: string; revenue: number; color: string }[];
}

const TopBar: React.FC<TopBarProps> = ({ 
  sidebarCollapsed, 
  activeTab, 
  setActiveTab, 
  setMobileSidebarOpen, 
  admin,
  topNurseries = []
}) => {
  const [searchFocused, setSearchFocused] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [showResults, setShowResults] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  const getTitle = (tab: string) => {
    switch (tab) {
      case 'dashboard': return 'Dashboard';
      case 'catalog': return 'Catalog';
      case 'orders': return 'Orders';
      case 'nurseries': return 'Nurseries';
      case 'users': return 'User Management';
      case 'financials': return 'Financials';
      case 'settings': return 'Settings';
      default: return 'Dashboard';
    }
  };

  // Close search results when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setShowResults(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const filteredSections = searchQuery.length > 0
    ? navItems.filter(item => item.label.toLowerCase().includes(searchQuery.toLowerCase()))
    : [];

  const filteredNurseries = searchQuery.length > 0
    ? topNurseries.filter(nursery => nursery.name.toLowerCase().includes(searchQuery.toLowerCase()))
    : [];

  const handleResultClick = (tabId: string) => {
    setActiveTab(tabId);
    setSearchQuery('');
    setShowResults(false);
  };

  return (
    <header
      className={`fixed top-0 right-0 h-[72px] z-40 flex items-center justify-between px-4 md:px-6 transition-all duration-300 ${sidebarCollapsed ? 'lg:left-[72px]' : 'lg:left-[260px]'
        } left-0`}
      style={{
        background: 'linear-gradient(180deg, rgba(12,12,12,0.95) 0%, rgba(12,12,12,0.8) 100%)',
        backdropFilter: 'blur(20px)',
        borderBottom: '1px solid rgba(255,255,255,0.04)',
      }}
    >
      {/* Left: Mobile Toggle + Breadcrumb + Search */}
      <div className="flex items-center gap-3 md:gap-6 min-w-0 flex-1">
        <button
          onClick={() => setMobileSidebarOpen(true)}
          className="lg:hidden w-10 h-10 rounded-xl flex items-center justify-center text-dark-200 hover:text-white hover:bg-dark-700 transition-all shrink-0"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="hidden sm:block shrink-0">
          <p className="text-[10px] text-dark-300 font-bold uppercase tracking-widest leading-none mb-1">Verdant</p>
          <h2 className="text-base font-bold text-white leading-none truncate">{getTitle(activeTab)}</h2>
        </div>

        <div
          ref={searchRef}
          className={`relative hidden md:flex items-center transition-all duration-300 ${searchFocused ? 'w-96' : 'w-64'
            }`}
        >
          <Search className={`absolute left-3 w-4 h-4 transition-colors ${searchFocused ? 'text-verdant-400' : 'text-dark-300'}`} />
          <input
            type="text"
            placeholder="Search dashboard..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setShowResults(true);
            }}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-dark-800/50 border border-dark-600/50 text-sm text-white placeholder-dark-400 focus:outline-none focus:border-verdant-700/50 focus:bg-dark-800 transition-all"
            onFocus={() => {
              setSearchFocused(true);
              if (searchQuery) setShowResults(true);
            }}
          />
          {searchQuery && (
            <button 
              onClick={() => setSearchQuery('')}
              className="absolute right-10 p-1 text-dark-400 hover:text-white"
            >
              <div className="w-4 h-4 flex items-center justify-center text-[10px]">✕</div>
            </button>
          )}
          <kbd className="absolute right-3 px-1.5 py-0.5 text-[10px] text-dark-400 bg-dark-700/50 rounded border border-dark-600 font-mono">
            ⌘K
          </kbd>

          {/* Search Results Dropdown */}
          {showResults && (searchQuery.length > 0) && (
            <div className="absolute top-[calc(100%+8px)] left-0 w-full glass-card border border-white/5 rounded-2xl p-2 shadow-2xl animate-fade-in z-50 overflow-hidden">
              <div className="max-h-[400px] overflow-y-auto p-1">
                {filteredSections.length > 0 && (
                  <div className="mb-4">
                    <p className="text-[10px] uppercase tracking-widest font-bold text-dark-400 px-3 mb-2">Sections</p>
                    {filteredSections.map((item) => {
                      const Icon = item.icon;
                      return (
                        <button
                          key={item.id}
                          onClick={() => handleResultClick(item.id)}
                          className="w-full flex items-center gap-3 p-2.5 rounded-xl hover:bg-white/5 text-dark-100 hover:text-white transition-all group text-left"
                        >
                          <div className="w-8 h-8 rounded-lg bg-dark-700 flex items-center justify-center group-hover:bg-verdant-700/20 text-dark-300 group-hover:text-verdant-400 transition-colors">
                            <Icon className="w-4 h-4" />
                          </div>
                          <div>
                            <p className="text-sm font-medium">{item.label}</p>
                            <p className="text-[10px] text-dark-400">Navigate to {item.label}</p>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                )}

                {filteredNurseries.length > 0 && (
                  <div>
                    <p className="text-[10px] uppercase tracking-widest font-bold text-dark-400 px-3 mb-2">Top Nurseries</p>
                    {filteredNurseries.map((nursery, idx) => (
                      <button
                        key={`${nursery.name}-${idx}`}
                        onClick={() => handleResultClick('nurseries')}
                        className="w-full flex items-center gap-3 p-2.5 rounded-xl hover:bg-white/5 text-dark-100 hover:text-white transition-all group text-left"
                      >
                        <div className="w-8 h-8 rounded-lg bg-dark-700 flex items-center justify-center group-hover:bg-verdant-700/20 text-dark-300 group-hover:text-verdant-400 transition-colors">
                          <MapPin className="w-4 h-4" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium truncate">{nursery.name}</p>
                          <p className="text-[10px] text-dark-400">View performance in Logistics</p>
                        </div>
                        <div 
                          className="w-2 h-2 rounded-full shrink-0" 
                          style={{ backgroundColor: nursery.color }}
                        />
                      </button>
                    ))}
                  </div>
                )}

                {filteredSections.length === 0 && filteredNurseries.length === 0 && (
                  <div className="p-8 text-center">
                    <div className="w-12 h-12 rounded-2xl bg-dark-800 flex items-center justify-center mx-auto mb-3">
                      <Terminal className="w-6 h-6 text-dark-400" />
                    </div>
                    <p className="text-sm font-medium text-dark-100">No results found</p>
                    <p className="text-xs text-dark-400 mt-1">Try a different search term</p>
                  </div>
                )}
              </div>
              
              <div className="mt-2 p-2 border-t border-white/5 flex items-center justify-between">
                <p className="text-[10px] text-dark-400">
                  <span className="text-verdant-400">Enter</span> to select
                </p>
                <div className="flex gap-2">
                  <span className="px-1.5 py-0.5 rounded bg-dark-800 border border-dark-600 text-[10px] text-dark-400 font-mono">↑↓</span>
                  <span className="px-1.5 py-0.5 rounded bg-dark-800 border border-dark-600 text-[10px] text-dark-400 font-mono">ESC</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Mobile Search Icon Only */}
        <button className="md:hidden w-10 h-10 rounded-xl flex items-center justify-center text-dark-200 hover:text-white">
          <Search className="w-5 h-5" />
        </button>
      </div>

      {/* Right: Actions */}
      <div className="flex items-center gap-1 md:gap-2 shrink-0">
        <button className="hidden sm:flex w-10 h-10 rounded-xl items-center justify-center text-dark-200 hover:text-white hover:bg-dark-700 transition-all cursor-pointer">
          <Maximize2 className="w-4 h-4" />
        </button>

        <button className="w-10 h-10 rounded-xl flex items-center justify-center text-dark-200 hover:text-white hover:bg-dark-700 transition-all cursor-pointer">
          <Sun className="w-4 h-4" />
        </button>

        {/* Notifications */}
        <button className="relative w-10 h-10 rounded-xl flex items-center justify-center text-dark-200 hover:text-white hover:bg-dark-700 transition-all cursor-pointer">
          <Bell className="w-4 h-4" />
          <span className="absolute top-2.5 right-2.5 w-2 h-2 bg-red-500 rounded-full border-2 border-dark-900" />
        </button>

        {/* Divider */}
        <div className="hidden sm:block w-px h-6 bg-dark-600 mx-1" />

        {/* Profile */}
        <button className="flex items-center gap-2 md:gap-3 pl-1 md:pl-2 pr-1 md:pr-3 py-1.5 rounded-xl hover:bg-dark-700 transition-all cursor-pointer">
          <div className="w-8 h-8 md:w-9 md:h-9 rounded-full bg-gradient-to-br from-verdant-700 to-verdant-500 overflow-hidden flex items-center justify-center text-white font-bold text-xs md:text-sm shrink-0">
            {admin?.profileImageUrl ? (
              <img src={admin.profileImageUrl} alt={admin.fullName} className="w-full h-full object-cover" />
            ) : (
              admin?.fullName?.split(' ').map(n => n[0]).join('').toUpperCase() || 'AD'
            )}
          </div>
          <div className="text-left hidden lg:block">
            <p className="text-sm font-medium text-white leading-tight">{admin?.fullName || 'Verdant Admin'}</p>
            <p className="text-[11px] text-dark-200 leading-tight capitalize">{admin?.role?.replace('_', ' ') || 'Super Admin'}</p>
          </div>
          <ChevronDown className="w-3.5 h-3.5 text-dark-300 hidden md:block" />
        </button>
      </div>
    </header>
  );
};

export default TopBar;
