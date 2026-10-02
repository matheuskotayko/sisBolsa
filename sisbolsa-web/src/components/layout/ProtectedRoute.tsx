import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

interface ProtectedRouteProps {
  requireAdminOrProf?: boolean;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ requireAdminOrProf = false }) => {
  const { user, loading, canManage } = useAuth();

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: 'var(--bg-color)' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ width: '40px', height: '40px', border: '3px solid #e2e8f0', borderTopColor: 'var(--primary-color)', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 16px auto' }}></div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Carregando sessão...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (requireAdminOrProf && !canManage) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
};
