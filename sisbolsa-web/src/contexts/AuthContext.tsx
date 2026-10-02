import React, { createContext, useContext, useState, useEffect } from 'react';
import type { UsuarioAuth } from '../types';
import { authService } from '../services/authService';

interface AuthContextType {
  user: UsuarioAuth | null;
  loading: boolean;
  login: (email: string, pass: string) => Promise<void>;
  logout: () => Promise<void>;
  setUser: React.Dispatch<React.SetStateAction<UsuarioAuth | null>>;
  isAdmin: boolean;
  isProfessor: boolean;
  isBolsista: boolean;
  canManage: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UsuarioAuth | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadUser() {
      try {
        const u = await authService.obterUsuarioAtual();
        setUser(u);
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    }
    loadUser();
  }, []);

  const login = async (email: string, pass: string) => {
    const u = await authService.login(email, pass);
    setUser(u);
  };

  const logout = async () => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
    }
  };

  const isAdmin = user?.tipoUsuario === 'ADMIN';
  const isProfessor = user?.tipoUsuario === 'PROFESSOR';
  const isBolsista = user?.tipoUsuario === 'BOLSISTA';
  const canManage = isAdmin || isProfessor;

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        logout,
        setUser,
        isAdmin,
        isProfessor,
        isBolsista,
        canManage,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
