import { createContext, useContext, useEffect, useState } from 'react';
import type { User } from '../lib/api';
import { signIn as apiSignIn, signUp as apiSignUp, signOut as apiSignOut, getMe } from '../lib/api';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  signIn: (email: string, password: string, role?: 'usuario' | 'tecnico') => Promise<void>;
  signUp: (email: string, password: string, name: string, role: 'usuario' | 'tecnico', sector?: string) => Promise<void>;
  signOut: () => Promise<void>;
  refreshMe: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const me = await getMe();
        setUser(me);
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const signIn = async (email: string, password: string, role?: 'usuario' | 'tecnico') => {
    await apiSignIn(email, password, role);
    const me = await getMe();
    setUser(me);
  };

  const signUp = async (email: string, password: string, name: string, role: 'usuario' | 'tecnico', sector?: string) => {
    await apiSignUp(email, password, name, role, sector);
    await apiSignIn(email, password);
    const me = await getMe();
    setUser(me);
  };

  const signOut = async () => {
    await apiSignOut();
    setUser(null);
  };

  const refreshMe = async () => {
    try {
      const me = await getMe();
      setUser(me);
    } catch {
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, signIn, signUp, signOut, refreshMe }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
