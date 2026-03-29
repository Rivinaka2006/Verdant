import React, { useState, useEffect } from 'react';
import { AlertCircle } from 'lucide-react';
import Sidebar from './components/Sidebar';
import TopBar from './components/TopBar';
import StatsCards from './components/StatsCards';
import InventoryTable from './components/InventoryTable';
import OrderBoard from './components/OrderBoard';
import AnalyticsWidgets from './components/AnalyticsWidgets';
import MediaManagement from './components/MediaManagement';
import UserManagement from './components/UserManagement';
import SellerVerification from './components/SellerVerification';
import NurseryMap from './components/NurseryMap';
import Login from './components/Login';
import { auth } from './lib/firebase';
import { onAuthStateChanged, signOut } from 'firebase/auth';
import { useDashboardData } from './hooks/useDashboardData';

const App: React.FC = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

  const { admin, metrics, loading: dashboardLoading, error: dashboardError } = useDashboardData();

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (user) {
        setIsLoggedIn(true);
      } else {
        // Auth state was lost or logged out
        setIsLoggedIn(false);
      }
      setIsLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleLogout = async () => {
    try {
      await signOut(auth);
      setIsLoggedIn(false);
    } catch (error) {
      console.error("Logout failed:", error);
      setIsLoggedIn(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-dark-950 flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-verdant-800/30 border-t-verdant-500 rounded-full animate-spin" />
      </div>
    );
  }

  if (!isLoggedIn) {
    return <Login onLogin={() => setIsLoggedIn(true)} />;
  }

  return (
    <div className="min-h-screen bg-dark-950 text-white font-sans overflow-x-hidden">
      {/* Sidebar */}
      <Sidebar
        activeTab={activeTab}
        setActiveTab={(tab) => {
          setActiveTab(tab);
          setMobileSidebarOpen(false);
        }}
        collapsed={sidebarCollapsed}
        setCollapsed={setSidebarCollapsed}
        mobileOpen={mobileSidebarOpen}
        setMobileOpen={setMobileSidebarOpen}
        onLogout={handleLogout}
        admin={admin}
      />

      {/* Top Bar */}
      <TopBar
        sidebarCollapsed={sidebarCollapsed}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        setMobileSidebarOpen={setMobileSidebarOpen}
        admin={admin}
        topNurseries={metrics.topPerformingNurseries}
      />

      {/* Main Content */}
      <main
        className={`pt-[72px] min-h-screen transition-all duration-300 ${sidebarCollapsed ? 'lg:ml-[72px]' : 'lg:ml-[260px]'
          } ml-0`}
      >
        <div className="p-4 md:p-6 space-y-5 overflow-y-auto" style={{ height: 'calc(100vh - 72px)' }}>
          {dashboardError && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-4 flex items-center gap-3 text-red-400 mb-2">
              <div className="w-8 h-8 rounded-full bg-red-500/20 flex items-center justify-center shrink-0">
                <AlertCircle className="w-4 h-4" />
              </div>
              <p className="text-sm font-medium">{dashboardError}</p>
            </div>
          )}

          {dashboardLoading ? (
            <div className="flex flex-col items-center justify-center h-64 space-y-4">
              <div className="w-10 h-10 border-4 border-verdant-800/30 border-t-verdant-500 rounded-full animate-spin" />
              <p className="text-dark-300 text-sm font-medium animate-pulse">Syncing dashboard data...</p>
            </div>
          ) : (
            <>
              {activeTab === 'dashboard' && (
                <>
                  <StatsCards metrics={metrics} />
                  <AnalyticsWidgets metrics={metrics} />
                </>
              )}

              {activeTab === 'catalog' && <InventoryTable />}

              {activeTab === 'orders' && <OrderBoard />}

              {activeTab === 'nurseries' && <NurseryMap />}

              {activeTab === 'users' && <UserManagement />}
              {activeTab === 'verification' && <SellerVerification />}



              {activeTab === 'settings' && <MediaManagement />}
            </>
          )}

          {/* Footer Spacer */}
          <div className="h-4" />
        </div>
      </main>
    </div>
  );
};

export default App;
