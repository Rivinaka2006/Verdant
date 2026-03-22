import React, { useState, useEffect } from 'react';
import { Leaf, Mail, Lock, Eye, EyeOff, ArrowRight, ShieldCheck, Sparkles, AlertCircle } from 'lucide-react';
import { db, auth } from '../lib/firebase';
import { collection, query, where, getDocs, doc, setDoc, getDoc } from 'firebase/firestore';
import { signInWithEmailAndPassword } from 'firebase/auth';

interface LoginProps {
    onLogin: () => void;
}

const Login: React.FC<LoginProps> = ({ onLogin }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Seed dummy user if it doesn't exist
    useEffect(() => {
        const seedUser = async () => {
            try {
                const userEmail = "admin@verdant.com";
                
                // Check Firestore
                const userSnap = await getDoc(doc(db, "users", "dummy-admin"));
                if (!userSnap.exists()) {
                    await setDoc(doc(db, "users", "dummy-admin"), {
                        email: userEmail,
                        fullName: "Verdant Administrator",
                        role: "super_admin",
                        status: "active",
                        createdAt: new Date().toISOString()
                    });
                    console.log("Seeded dummy user in Firestore");
                }
            } catch (err) {
                console.warn("Seeding failed (might be permissions):", err);
            }
        };
        seedUser();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);
        setError(null);

        try {
            // First, try Firebase Auth
            try {
                await signInWithEmailAndPassword(auth, email, password);
                onLogin();
            } catch (authErr: any) {
                // If Auth fails (e.g. user doesn't exist in Auth but we want them to login via the Firestore dummy doc)
                // We check the 'users' collection as requested by the user for the dummy account.
                const usersRef = collection(db, "users");
                const q = query(usersRef, where("email", "==", email));
                const querySnapshot = await getDocs(q);
                
                if (!querySnapshot.empty) {
                    // In this dummy testing scenario, we check password plain-text for the specific dummy account
                    if (password === "password123" && email === "admin@verdant.com") {
                        onLogin();
                    } else {
                        setError("Invalid credentials. Please verify your identity.");
                    }
                } else {
                    setError("Unauthorized access. This account does not exist in our systems.");
                }
            }
        } catch (err: any) {
            console.error("Login Error:", err);
            setError(err.message || "An unexpected error occurred. Please try again.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-dark-950 relative overflow-hidden font-sans text-white selection:bg-verdant-500/30">
            {/* Dynamic Background Elements */}
            <div className="absolute top-[-10%] left-[-5%] w-[50%] h-[50%] bg-verdant-800/10 rounded-full blur-[140px] animate-pulse-slow" />
            <div className="absolute bottom-[-10%] right-[-5%] w-[50%] h-[50%] bg-verdant-600/10 rounded-full blur-[140px] animate-pulse-slow" style={{ animationDelay: '1.5s' }} />
            
            {/* Geometric Accents */}
            <div className="absolute top-0 left-0 w-full h-full opacity-5 pointer-events-none">
                <div className="absolute top-20 left-1/4 w-px h-64 bg-gradient-to-b from-transparent via-verdant-400 to-transparent" />
                <div className="absolute top-1/3 right-1/4 w-px h-96 bg-gradient-to-b from-transparent via-verdant-400 to-transparent" />
                <div className="absolute left-1/2 top-0 w-px h-full bg-gradient-to-b from-transparent via-white/20 to-transparent" />
            </div>

            {/* Floating Sparkles */}
            <div className="absolute top-[20%] right-[15%] text-verdant-400/20 animate-bounce" style={{ animationDuration: '3s' }}>
                <Sparkles className="w-6 h-6" />
            </div>
            <div className="absolute bottom-[20%] left-[15%] text-verdant-400/20 animate-bounce" style={{ animationDuration: '4s' }}>
                <Sparkles className="w-4 h-4" />
            </div>

            <div className="w-full max-w-[460px] px-6 relative z-10">
                {/* Logo Section */}
                <div className="flex flex-col items-center mb-10 animate-slide-in">
                    <div className="w-16 h-16 rounded-2xl bg-verdant-700 flex items-center justify-center mb-6 shadow-[0_0_30px_rgba(46,125,50,0.4)] border border-verdant-500/30">
                        <Leaf className="w-8 h-8 text-verdant-200" />
                    </div>
                    <h1 className="text-4xl font-extrabold tracking-tight text-white mb-2 text-center">
                        Verdant <span className="text-verdant-400">Admin</span>
                    </h1>
                    <p className="text-dark-300 text-center font-medium tracking-wide text-sm opacity-80 uppercase">
                        Enterprise Ecosystem Management
                    </p>
                </div>

                {/* Login Card */}
                <div className="glass-card rounded-[2.5rem] p-10 border border-white/10 shadow-2xl backdrop-blur-3xl relative overflow-hidden animate-slide-in" style={{ animationDelay: '0.1s' }}>
                    <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-verdant-500/50 to-transparent" />
                    
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {error && (
                            <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-4 flex items-start gap-3 animate-shake">
                                <AlertCircle className="w-5 h-5 text-red-500 shrink-0 mt-0.5" />
                                <p className="text-xs text-red-200 font-medium leading-relaxed">{error}</p>
                            </div>
                        )}

                        <div className="space-y-2">
                            <label className="block text-[10px] font-bold uppercase tracking-widest text-dark-300 ml-1">
                                Identification (Email)
                            </label>
                            <div className="relative group">
                                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-4.5 h-4.5 text-dark-400 group-focus-within:text-verdant-400 transition-all duration-300" />
                                <input
                                    type="email"
                                    required
                                    placeholder="admin@verdant.com"
                                    className="w-full bg-dark-900/60 border border-white/5 rounded-2xl py-4 pl-12 pr-4 text-sm text-white placeholder-dark-500 focus:outline-none focus:border-verdant-600/50 focus:ring-1 focus:ring-verdant-600/20 transition-all duration-300 backdrop-blur-md"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                />
                            </div>
                        </div>

                        <div className="space-y-2">
                            <div className="flex items-center justify-between px-1">
                                <label className="text-[10px] font-bold uppercase tracking-widest text-dark-300">
                                    Security Code (Password)
                                </label>
                                <a href="#" className="text-[10px] font-bold uppercase tracking-widest text-verdant-400 hover:text-verdant-300 transition-colors">
                                    Reset
                                </a>
                            </div>
                            <div className="relative group">
                                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-4.5 h-4.5 text-dark-400 group-focus-within:text-verdant-400 transition-all duration-300" />
                                <input
                                    type={showPassword ? 'text' : 'password'}
                                    required
                                    placeholder="••••••••"
                                    className="w-full bg-dark-900/60 border border-white/5 rounded-2xl py-4 pl-12 pr-12 text-sm text-white placeholder-dark-500 focus:outline-none focus:border-verdant-600/50 focus:ring-1 focus:ring-verdant-600/20 transition-all duration-300 backdrop-blur-md"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-4 top-1/2 -translate-y-1/2 text-dark-400 hover:text-white transition-colors cursor-pointer"
                                >
                                    {showPassword ? <EyeOff className="w-4.5 h-4.5" /> : <Eye className="w-4.5 h-4.5" />}
                                </button>
                            </div>
                        </div>

                        <div className="flex items-center gap-3 px-1 pt-2">
                            <input
                                type="checkbox"
                                id="remember"
                                className="w-4 h-4 rounded border-white/10 bg-dark-900/80 text-verdant-600 focus:ring-verdant-600/50 cursor-pointer accent-verdant-600"
                            />
                            <label htmlFor="remember" className="text-xs font-semibold text-dark-400 cursor-pointer hover:text-dark-300 transition-colors">
                                Persistent Authentication Session
                            </label>
                        </div>

                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full bg-gradient-to-r from-verdant-700 to-verdant-600 text-white rounded-2xl py-4.5 font-bold text-sm flex items-center justify-center gap-3 hover:from-verdant-600 hover:to-verdant-500 transition-all active:scale-[0.98] cursor-pointer shadow-[0_10px_20px_-5px_rgba(46,125,50,0.5)] disabled:opacity-70 disabled:cursor-not-allowed group"
                        >
                            {isLoading ? (
                                <div className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                            ) : (
                                <>
                                    Verify & Authenticate
                                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1.5 transition-transform duration-300" />
                                </>
                            )}
                        </button>
                        
                        <div className="pt-2 text-center text-[10px] text-dark-500 font-medium">
                            DUMMY CREDENTIALS: <span className="text-dark-300">admin@verdant.com</span> / <span className="text-dark-300">password123</span>
                        </div>
                    </form>
                </div>

                {/* Footer info */}
                <div className="mt-10 flex items-center justify-center gap-3 text-[10px] text-dark-400 uppercase tracking-[0.2em] font-bold animate-slide-in opacity-70" style={{ animationDelay: '0.2s' }}>
                    <div className="h-px w-8 bg-white/10" />
                    <ShieldCheck className="w-4 h-4 text-verdant-600" />
                    <span>Secure Quantum-Verified Node</span>
                    <div className="h-px w-8 bg-white/10" />
                </div>
            </div>

            {/* Credit */}
            <div className="absolute bottom-8 left-1/2 -translate-x-1/2 text-[10px] text-dark-500 font-bold tracking-widest opacity-40 uppercase">
                Verdant Core v4.2.0 • Build ID: VER-2026-X
            </div>
            
            <style dangerouslySetInnerHTML={{ __html: `
                @keyframes shake {
                    0%, 100% { transform: translateX(0); }
                    25% { transform: translateX(-4px); }
                    75% { transform: translateX(4px); }
                }
                .animate-shake {
                    animation: shake 0.4s ease-in-out 0s 2;
                }
            `}} />
        </div>
    );
};

export default Login;
