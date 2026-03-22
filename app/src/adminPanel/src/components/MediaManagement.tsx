import React, { useState, useRef } from 'react';
import { Image as ImageIcon, Plus, CloudUpload, Trash2, ExternalLink, Power, Layout, Loader2, ArrowUp, ArrowDown } from 'lucide-react';
import { useBanners } from '../hooks/useBanners';
import { Banner } from '../types';

const MediaManagement: React.FC = () => {
  const { banners, loading, uploading, progress, uploadBanner, updateBanner, deleteBanner } = useBanners();
  const [isDragging, setIsDragging] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Upload Form State
  const [newBanner, setNewBanner] = useState({
    title: '',
    subtitle: '',
    link: '',
    active: true,
  });

  const handleFileUpload = async (file: File) => {
    try {
      await uploadBanner(file, {
        ...newBanner,
        order: banners.length > 0 ? Math.max(...banners.map(b => b.order)) + 1 : 1
      });
      setShowUploadModal(false);
      setNewBanner({ title: '', subtitle: '', link: '', active: true });
    } catch (error) {
      console.error("Upload failed:", error);
      alert("Failed to upload banner. Please try again.");
    }
  };

  const onFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      handleFileUpload(e.target.files[0]);
    }
  };

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  };

  const toggleStatus = async (banner: Banner) => {
    try {
      await updateBanner(banner.id, { active: !banner.active });
    } catch (error) {
      console.error("Update failed:", error);
    }
  };

  const moveBanner = async (banner: Banner, direction: 'up' | 'down') => {
    const currentIndex = banners.findIndex(b => b.id === banner.id);
    if ((direction === 'up' && currentIndex === 0) || (direction === 'down' && currentIndex === banners.length - 1)) return;

    const targetIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1;
    const targetBanner = banners[targetIndex];

    try {
      // Swap orders
      await updateBanner(banner.id, { order: targetBanner.order });
      await updateBanner(targetBanner.id, { order: banner.order });
    } catch (error) {
      console.error("Reorder failed:", error);
    }
  };

  return (
    <div className="glass-card rounded-2xl overflow-hidden h-full flex flex-col bg-dark-900/40 backdrop-blur-xl border border-white/5 shadow-2xl">
      {/* Header */}
      <div className="flex items-center justify-between p-6 border-b border-white/5 bg-white/[0.02]">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-verdant-700/20 flex items-center justify-center border border-verdant-700/30">
            <Layout className="w-5 h-5 text-verdant-400" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white tracking-tight">Banner Management</h3>
            <p className="text-xs text-dark-300 font-medium">{banners.length} Active Banners</p>
          </div>
        </div>
        <button 
          onClick={() => setShowUploadModal(true)}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-verdant-600 text-white text-sm font-semibold hover:bg-verdant-500 hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer shadow-lg shadow-verdant-900/20"
        >
          <Plus className="w-4 h-4" />
          Add New Banner
        </button>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {/* Upload Modal Overlay */}
        {showUploadModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
            <div className="glass-card w-full max-w-md rounded-2xl p-6 border border-white/10 shadow-2xl relative animate-scale-in">
              <h4 className="text-lg font-bold text-white mb-4">Promotional Banner Details</h4>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-dark-200 mb-1.5 uppercase tracking-wider">Banner Title</label>
                  <input 
                    type="text" 
                    value={newBanner.title}
                    onChange={(e) => setNewBanner({...newBanner, title: e.target.value})}
                    placeholder="E.g. Summer Plant Sale"
                    className="w-full bg-dark-800/80 border border-white/5 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:ring-2 focus:ring-verdant-600/50 transition-all placeholder:text-dark-500"
                  />
                </div>
                
                <div>
                  <label className="block text-xs font-bold text-dark-200 mb-1.5 uppercase tracking-wider">Subtitle / Promo Text</label>
                  <input 
                    type="text" 
                    value={newBanner.subtitle}
                    onChange={(e) => setNewBanner({...newBanner, subtitle: e.target.value})}
                    placeholder="E.g. Up to 40% off on all Monstera"
                    className="w-full bg-dark-800/80 border border-white/5 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:ring-2 focus:ring-verdant-600/50 transition-all placeholder:text-dark-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-dark-200 mb-1.5 uppercase tracking-wider">Redirect Link (URL)</label>
                  <input 
                    type="text" 
                    value={newBanner.link}
                    onChange={(e) => setNewBanner({...newBanner, link: e.target.value})}
                    placeholder="E.g. /category/indoor-plants"
                    className="w-full bg-dark-800/80 border border-white/5 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:ring-2 focus:ring-verdant-600/50 transition-all placeholder:text-dark-500"
                  />
                </div>

                <div className="pt-2">
                   <div
                    className={`drag-zone rounded-2xl p-8 flex flex-col items-center justify-center text-center border-2 border-dashed transition-all cursor-pointer ${
                      isDragging ? 'border-verdant-500 bg-verdant-500/10' : 'border-white/10 bg-white/[0.02] hover:bg-white/[0.05]'
                    }`}
                    onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
                    onDragLeave={() => setIsDragging(false)}
                    onDrop={onDrop}
                    onClick={() => fileInputRef.current?.click()}
                  >
                    <input 
                      type="file" 
                      ref={fileInputRef} 
                      className="hidden" 
                      accept="image/*"
                      onChange={onFileSelect}
                    />
                    
                    {uploading ? (
                      <div className="flex flex-col items-center gap-3">
                         <div className="w-12 h-12 relative flex items-center justify-center">
                            <Loader2 className="w-10 h-10 text-verdant-500 animate-spin absolute" />
                            <span className="text-[10px] font-bold text-white">{Math.round(progress)}%</span>
                         </div>
                         <p className="text-sm font-semibold text-white animate-pulse">Uploading Media...</p>
                      </div>
                    ) : (
                      <>
                        <div className="w-14 h-14 rounded-2xl bg-verdant-700/20 flex items-center justify-center mb-4 border border-verdant-700/30">
                          <CloudUpload className="w-7 h-7 text-verdant-400" />
                        </div>
                        <p className="text-sm font-bold text-white mb-1">Click or drag banner image</p>
                        <p className="text-xs text-dark-300 font-medium">Recommended: 1920x600 px (WEBP/JPG)</p>
                      </>
                    )}
                  </div>
                </div>

                <div className="flex gap-3 pt-2">
                  <button 
                    onClick={() => setShowUploadModal(false)}
                    className="flex-1 px-4 py-3 rounded-xl bg-dark-700 text-white text-sm font-bold hover:bg-dark-600 transition-all border border-white/5"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Existing Banners List */}
        <div className="grid grid-cols-1 gap-4">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20 space-y-4">
               <div className="w-12 h-12 border-4 border-verdant-800/20 border-t-verdant-500 rounded-full animate-spin" />
               <p className="text-dark-300 text-sm font-medium">Fetching banners from cloud...</p>
            </div>
          ) : banners.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 border-2 border-dashed border-white/5 rounded-3xl bg-white/[0.01]">
              <div className="w-16 h-16 rounded-full bg-dark-800 flex items-center justify-center mb-4">
                <ImageIcon className="w-8 h-8 text-dark-400" />
              </div>
              <h4 className="text-white font-bold">No active banners</h4>
              <p className="text-dark-300 text-sm mt-1">Upload your first promotional banner to get started</p>
            </div>
          ) : (
            banners.map((banner, index) => (
              <div 
                key={banner.id}
                className={`group relative overflow-hidden rounded-2xl bg-dark-800/40 border border-white/[0.05] hover:border-verdant-500/30 transition-all duration-300 shadow-xl ${!banner.active ? 'opacity-60 saturate-50' : ''}`}
              >
                <div className="flex flex-col md:flex-row h-full">
                  {/* Image Preview */}
                  <div className="w-full md:w-64 h-40 md:h-auto relative overflow-hidden bg-dark-900 border-r border-white/5">
                    <img 
                      src={banner.imageUrl} 
                      alt={banner.title} 
                      className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-dark-950/80 via-transparent to-transparent md:bg-gradient-to-r" />
                  </div>

                  {/* Details */}
                  <div className="flex-1 p-5 flex flex-col justify-between">
                    <div>
                      <div className="flex items-center gap-2 mb-2">
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-dark-700 text-dark-300 uppercase tracking-widest">
                          Banner #{banner.order}
                        </span>
                        {banner.active ? (
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-green-500/20 text-green-400 border border-green-500/30 flex items-center gap-1">
                            <div className="w-1 h-1 rounded-full bg-green-400 animate-pulse" />
                            Live
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-red-500/20 text-red-400 border border-red-500/30">
                            Inactive
                          </span>
                        )}
                      </div>
                      <h4 className="text-lg font-bold text-white mb-1">{banner.title || 'Untitled Banner'}</h4>
                      <p className="text-sm text-dark-200 line-clamp-1">{banner.subtitle || 'No description provided'}</p>
                      {banner.link && (
                        <div className="flex items-center gap-1.5 mt-3 text-xs text-verdant-400 font-medium">
                          <ExternalLink className="w-3 h-3" />
                          <span className="truncate max-w-[200px]">{banner.link}</span>
                        </div>
                      )}
                    </div>

                    <div className="flex items-center justify-between mt-5 pt-4 border-t border-white/5">
                      <div className="flex items-center gap-1">
                        <button 
                          onClick={() => moveBanner(banner, 'up')}
                          disabled={index === 0}
                          className="p-1.5 rounded-lg bg-dark-700 text-white disabled:opacity-20 hover:bg-dark-600 transition-all"
                          title="Move Up"
                        >
                          <ArrowUp className="w-3.5 h-3.5" />
                        </button>
                        <button 
                          onClick={() => moveBanner(banner, 'down')}
                          disabled={index === banners.length - 1}
                          className="p-1.5 rounded-lg bg-dark-700 text-white disabled:opacity-20 hover:bg-dark-600 transition-all"
                          title="Move Down"
                        >
                          <ArrowDown className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <div className="flex items-center gap-2">
                        <button 
                           onClick={() => toggleStatus(banner)}
                           className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all border ${
                            banner.active ? 'bg-amber-500/10 text-amber-500 border-amber-500/20 hover:bg-amber-500/20' : 'bg-green-500/10 text-green-500 border-green-500/20 hover:bg-green-500/20'
                           }`}
                        >
                          <Power className="w-3.5 h-3.5" />
                          {banner.active ? 'Deactivate' : 'Activate'}
                        </button>
                        <button 
                          onClick={() => {
                            if(window.confirm('Are you sure you want to delete this banner?')) {
                              deleteBanner(banner);
                            }
                          }}
                          className="p-2 rounded-xl bg-red-500/10 text-red-500 hover:bg-red-500/20 border border-red-500/20 transition-all"
                          title="Delete Banner"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default MediaManagement;
