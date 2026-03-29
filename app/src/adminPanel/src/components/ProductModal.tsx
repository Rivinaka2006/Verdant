import React, { useState, useEffect } from 'react';
import { X, Save, Loader2, Plus, Trash2, Camera, Edit3, ChevronDown } from 'lucide-react';
import { Product } from '../types';
import { db } from '../lib/firebase';
import { collection, addDoc, updateDoc, doc, serverTimestamp } from 'firebase/firestore';

interface ProductModalProps {
  isOpen: boolean;
  onClose: () => void;
  product: Product | null;
}

const CATEGORIES = ['Indoor Plants', 'Outdoor Plants', 'Succulents', 'Flowering Plants', 'Herbs'];

const ProductModal: React.FC<ProductModalProps> = ({ isOpen, onClose, product }) => {
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<Partial<Product>>({
    name: '',
    description: '',
    category: 'Indoor Plants',
    price: 0,
    oldPrice: 0,
    stock: 0,
    available: true,
    nurseryId: 'default-nursery',
    imageUrls: [],
    videoUrl: '',
    lightRequirement: 'Moderate',
    waterFrequency: 'Once a week',
    careInstructions: '',
    rating: 5,
    soldCount: 0,
  });

  const [imageUrlInput, setImageUrlInput] = useState('');

  useEffect(() => {
    if (product) {
      setFormData(product);
    } else {
      setFormData({
        name: '',
        description: '',
        category: 'Indoor Plants',
        price: 0,
        oldPrice: 0,
        stock: 0,
        available: true,
        nurseryId: 'default-nursery',
        imageUrls: [],
        videoUrl: '',
        lightRequirement: 'Moderate',
        waterFrequency: 'Once a week',
        careInstructions: '',
        rating: 5,
        soldCount: 0,
      });
    }
  }, [product, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (product) {
        const { productId, createdAt, ...updateData } = formData as Product;
        await updateDoc(doc(db, "products", product.productId), {
          ...updateData,
          updatedAt: serverTimestamp()
        });
      } else {
        await addDoc(collection(db, "products"), {
          ...formData,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        });
      }
      onClose();
    } catch (err) {
      console.error("Error saving product:", err);
      alert("Failed to save product. Please check console for details.");
    } finally {
      setLoading(false);
    }
  };

  const addImageUrl = () => {
    if (imageUrlInput.trim()) {
      setFormData(prev => ({
        ...prev,
        imageUrls: [...(prev.imageUrls || []), imageUrlInput.trim()]
      }));
      setImageUrlInput('');
    }
  };

  const removeImageUrl = (index: number) => {
    setFormData(prev => ({
      ...prev,
      imageUrls: (prev.imageUrls || []).filter((_, i) => i !== index)
    }));
  };

  return (
    <div className="fixed inset-0 z-[100] flex justify-end overflow-hidden">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-dark-950/60 backdrop-blur-md transition-opacity animate-in fade-in duration-500"
        onClick={onClose}
      />

      {/* Drawer Panel */}
      <div className="relative w-full max-w-xl bg-dark-900 border-l border-white/10 shadow-[0_0_50px_rgba(0,0,0,0.5)] flex flex-col animate-slide-in-right">
        {/* Animated Accent Line */}
        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-verdant-600 via-verdant-400 to-verdant-600" />
        
        {/* Header */}
        <div className="flex items-center justify-between p-6 bg-dark-800/50 backdrop-blur-xl border-b border-white/5 sticky top-0 z-10">
          <div className="space-y-1">
            <h2 className="text-xl font-bold text-white font-display flex items-center gap-3">
              {product ? <Edit3 className="w-5 h-5 text-verdant-400" /> : <Plus className="w-5 h-5 text-verdant-400" />}
              {product ? 'Refine Plant' : 'List New Plant'}
            </h2>
            <p className="text-[10px] text-dark-300 uppercase tracking-widest font-bold opacity-70">
              {product ? `SKU: ${product.productId.substring(0, 12)}` : 'Catalog Integration'}
            </p>
          </div>
          <button 
            onClick={onClose}
            className="w-10 h-10 rounded-full flex items-center justify-center text-dark-400 hover:text-white hover:bg-white/5 transition-all group"
          >
            <X className="w-5 h-5 group-hover:rotate-90 transition-transform duration-300" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto custom-scrollbar">
          <form id="product-form" onSubmit={handleSubmit} className="p-6 md:p-8 space-y-10">
            
            {/* Section: Basic Identity */}
            <div className="space-y-6">
              <div className="flex items-center gap-2 mb-4">
                <span className="w-1.5 h-1.5 rounded-full bg-verdant-500" />
                <h3 className="text-xs font-bold text-verdant-400 uppercase tracking-widest">General Information</h3>
              </div>

              <div className="space-y-4">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Plant Name</label>
                  <input
                    required
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="e.g., Midnight Monstera"
                    className="w-full px-5 py-4 rounded-2xl bg-dark-800/50 border border-white/5 text-white text-sm focus:outline-none focus:border-verdant-500/50 focus:bg-dark-800 transition-all placeholder:text-dark-600"
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Category</label>
                    <div className="relative">
                      <select
                        value={formData.category}
                        onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                        className="w-full px-5 py-4 rounded-2xl bg-dark-800/50 border border-white/5 text-white text-sm focus:outline-none focus:border-verdant-500/50 transition-all appearance-none cursor-pointer"
                      >
                        {CATEGORIES.map(cat => <option key={cat} value={cat} className="bg-dark-800">{cat}</option>)}
                      </select>
                      <ChevronDown className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-dark-400 pointer-events-none" />
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Availability</label>
                    <button
                      type="button"
                      onClick={() => setFormData({ ...formData, available: !formData.available })}
                      className={`flex items-center gap-3 w-full px-5 py-3.5 rounded-2xl border transition-all ${formData.available ? 'bg-verdant-500/10 border-verdant-500/30 text-verdant-400' : 'bg-dark-800/50 border-white/5 text-dark-400'}`}
                    >
                      <div className={`w-10 h-5 rounded-full relative transition-colors ${formData.available ? 'bg-verdant-500' : 'bg-dark-700'}`}>
                        <div className={`absolute top-1 w-3 h-3 rounded-full bg-white transition-all ${formData.available ? 'left-6' : 'left-1'}`} />
                      </div>
                      <span className="text-xs font-bold uppercase tracking-wider">{formData.available ? 'Live' : 'Hidden'}</span>
                    </button>
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Description</label>
                  <textarea
                    required
                    rows={4}
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Tell the story of this plant..."
                    className="w-full px-5 py-4 rounded-2xl bg-dark-800/50 border border-white/5 text-white text-sm focus:outline-none focus:border-verdant-500/50 transition-all resize-none placeholder:text-dark-600"
                  />
                </div>
              </div>
            </div>

            {/* Section: Commercial Data */}
            <div className="space-y-6">
              <div className="flex items-center gap-2 mb-4">
                <span className="w-1.5 h-1.5 rounded-full bg-blue-500" />
                <h3 className="text-xs font-bold text-blue-400 uppercase tracking-widest">Pricing & Inventory</h3>
              </div>
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Price (LKR)</label>
                  <input
                    required
                    type="number"
                    min="0"
                    value={formData.price}
                    onChange={(e) => setFormData({ ...formData, price: Math.max(0, Number(e.target.value)) })}
                    className="w-full px-5 py-4 rounded-2xl bg-dark-800/50 border border-white/5 text-verdant-300 text-lg font-bold focus:outline-none focus:border-verdant-500/50 transition-all"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Old Price</label>
                  <input
                    type="number"
                    min="0"
                    value={formData.oldPrice}
                    onChange={(e) => setFormData({ ...formData, oldPrice: Math.max(0, Number(e.target.value)) })}
                    className="w-full px-5 py-4 rounded-2xl bg-dark-800/50 border border-white/5 text-dark-400 text-sm focus:outline-none focus:border-white/20 transition-all font-medium"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Stock Level</label>
                  <input
                    required
                    type="number"
                    min="0"
                    value={formData.stock}
                    onChange={(e) => setFormData({ ...formData, stock: Math.max(0, Number(e.target.value)) })}
                    className={`w-full px-5 py-4 rounded-2xl bg-dark-800/50 border border-white/5 text-sm font-bold focus:outline-none transition-all ${formData.stock === 0 ? 'text-rose-500' : 'text-white'}`}
                  />
                </div>
              </div>
            </div>

            {/* Section: Visuals */}
            <div className="space-y-6">
              <div className="flex items-center gap-2 mb-4">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                <h3 className="text-xs font-bold text-amber-500 uppercase tracking-widest">Visual Gallery</h3>
              </div>

              <div className="space-y-4">
                <div className="flex gap-3">
                  <div className="relative flex-1 group">
                    <Camera className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-dark-500 group-focus-within:text-verdant-400 transition-colors" />
                    <input
                      type="url"
                      value={imageUrlInput}
                      onChange={(e) => setImageUrlInput(e.target.value)}
                      placeholder="Paste image URL here..."
                      className="w-full pl-12 pr-5 py-4 rounded-2xl bg-dark-800/50 border border-white/5 text-white text-xs focus:outline-none focus:border-verdant-500/50 transition-all font-mono"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={addImageUrl}
                    className="px-6 py-4 rounded-2xl bg-verdant-600 text-white hover:bg-verdant-500 transition-all shadow-lg shadow-verdant-900/20 active:scale-95"
                  >
                    <Plus className="w-5 h-5" />
                  </button>
                </div>

                <div className="grid grid-cols-4 gap-4">
                  {formData.imageUrls?.map((url, idx) => (
                    <div key={idx} className="relative aspect-square rounded-2xl overflow-hidden border border-white/10 group shadow-lg">
                      <img src={url} alt="Plant" className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" />
                      <div className="absolute inset-0 bg-dark-950/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                        <button
                          type="button"
                          onClick={() => removeImageUrl(idx)}
                          className="w-10 h-10 rounded-full bg-rose-500 text-white flex items-center justify-center shadow-xl transform translate-y-4 group-hover:translate-y-0 transition-all"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                  {(formData.imageUrls?.length || 0) < 4 && (
                    <div className="aspect-square rounded-2xl border-2 border-dashed border-dark-700 flex flex-col items-center justify-center text-dark-500 group hover:border-dark-500 transition-colors">
                      <Camera className="w-6 h-6 mb-2 opacity-30 group-hover:opacity-60 transition-opacity" />
                      <span className="text-[10px] font-bold uppercase tracking-wider opacity-30 group-hover:opacity-60">Add Image</span>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Section: Botanical Care */}
            <div className="p-6 rounded-3xl bg-white/[0.02] border border-white/5 space-y-6">
              <div className="flex items-center gap-2 mb-2">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                <h3 className="text-xs font-bold text-emerald-400 uppercase tracking-widest">Expert Care Guide</h3>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Light Exposure</label>
                  <input
                    type="text"
                    value={formData.lightRequirement}
                    onChange={(e) => setFormData({ ...formData, lightRequirement: e.target.value })}
                    placeholder="e.g., Bright Indirect"
                    className="w-full px-5 py-3 rounded-xl bg-dark-900 border border-white/5 text-white text-sm focus:outline-none focus:border-emerald-500/50"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Water Hydration</label>
                  <input
                    type="text"
                    value={formData.waterFrequency}
                    onChange={(e) => setFormData({ ...formData, waterFrequency: e.target.value })}
                    placeholder="e.g., Bi-weekly"
                    className="w-full px-5 py-3 rounded-xl bg-dark-900 border border-white/5 text-white text-sm focus:outline-none focus:border-emerald-500/50"
                  />
                </div>
              </div>
              
              <div className="space-y-2">
                <label className="text-[10px] font-bold text-dark-300 uppercase tracking-widest px-1">Professional Care Tips</label>
                <textarea
                  rows={3}
                  value={formData.careInstructions}
                  onChange={(e) => setFormData({ ...formData, careInstructions: e.target.value })}
                  placeholder="Share unique tips for this specific cultivar..."
                  className="w-full px-5 py-3 rounded-xl bg-dark-900 border border-white/5 text-white text-sm focus:outline-none focus:border-emerald-500/50 resize-none"
                />
              </div>
            </div>
          </form>
        </div>

        {/* Footer */}
        <div className="p-6 bg-dark-800/80 backdrop-blur-xl border-t border-white/5 flex items-center justify-between gap-4 sticky bottom-0 z-10 shadow-[0_-10px_30px_rgba(0,0,0,0.3)]">
          <button
            type="button"
            onClick={onClose}
            className="px-8 py-3.5 rounded-2xl text-dark-300 text-sm font-bold hover:text-white hover:bg-white/5 transition-all"
          >
            Discard
          </button>
          <button
            type="submit"
            form="product-form"
            disabled={loading}
            className="flex-1 max-w-[240px] px-8 py-3.5 rounded-2xl bg-gradient-to-r from-verdant-700 to-verdant-600 text-white text-sm font-bold hover:from-verdant-600 hover:to-verdant-500 transition-all glow-green flex items-center justify-center gap-2 overflow-hidden relative shadow-xl disabled:opacity-50 group"
          >
            {loading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <>
                <Save className="w-5 h-5 group-hover:scale-110 transition-transform" />
                <span className="relative z-10">{product ? 'Update Inventory' : 'Create Listing'}</span>
              </>
            )}
            
            {/* Glossy overlay effect */}
            <div className="absolute top-0 -left-full w-full h-full bg-gradient-to-r from-transparent via-white/10 to-transparent group-hover:left-full transition-all duration-1000 ease-in-out pointer-events-none" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductModal;
