import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ToastProvider } from './contexts/ToastContext';
import { ThemeProvider } from './contexts/ThemeContext';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute } from './components/layout/ProtectedRoute';

import { Login } from './pages/Login';
import { EsqueciSenha } from './pages/EsqueciSenha';
import { Dashboard } from './pages/Dashboard';
import { Laboratorios } from './pages/Laboratorios';
import { LaboratorioDetalhes } from './pages/LaboratorioDetalhes';
import { Projetos } from './pages/Projetos';
import { ProjetoDetalhes } from './pages/ProjetoDetalhes';
import { Usuarios } from './pages/Usuarios';
import { FrequenciaPage } from './pages/Frequencia';
import { Relatorios } from './pages/Relatorios';
import { AuditoriaPage } from './pages/Auditoria';
import { Perfil } from './pages/Perfil';
import { NotFound } from './pages/NotFound';

export const App: React.FC = () => {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <ToastProvider>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/esqueci-senha" element={<EsqueciSenha />} />

              <Route element={<ProtectedRoute />}>
                <Route element={<AppLayout />}>
                  <Route path="/" element={<Dashboard />} />
                  <Route path="/laboratorios" element={<Laboratorios />} />
                  <Route path="/laboratorios/:id" element={<LaboratorioDetalhes />} />
                  <Route path="/projetos" element={<Projetos />} />
                  <Route path="/projetos/:id" element={<ProjetoDetalhes />} />
                  <Route path="/frequencia" element={<FrequenciaPage />} />
                  <Route path="/perfil" element={<Perfil />} />

                  {/* Manager / Admin Routes */}
                  <Route element={<ProtectedRoute requireAdminOrProf />}>
                    <Route path="/usuarios" element={<Usuarios />} />
                    <Route path="/relatorios" element={<Relatorios />} />
                    <Route path="/auditoria" element={<AuditoriaPage />} />
                  </Route>

                  <Route path="*" element={<NotFound />} />
                </Route>
              </Route>
            </Routes>
          </AuthProvider>
        </ToastProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
};

export default App;
