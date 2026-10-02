import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  FolderKanban,
  Users,
  Clock,
  BarChart3,
  User,
  LogOut,
  Sun,
  Moon,
  ShieldCheck,
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../contexts/ToastContext';
import { useTheme } from '../../contexts/ThemeContext';

export const Sidebar: React.FC = () => {
  const { user, logout, isBolsista } = useAuth();
  const { showToast } = useToast();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logout();
      showToast('Sessão encerrada com sucesso.', 'sucesso');
      navigate('/login');
    } catch {
      navigate('/login');
    }
  };

  const getRoleLabel = () => {
    if (user?.tipoUsuario === 'ADMIN') return 'Administrador';
    if (user?.tipoUsuario === 'PROFESSOR') return 'Professor / Coord.';
    return 'Bolsista';
  };

  return (
    <aside className="sidebar" aria-label="Menu principal de navegação">
      <h2>SisBolsa</h2>

      {user && (
        <div className="user-profile-widget">
          {user.fotoUrl ? (
            <img src={user.fotoUrl} alt={user.nome} className="profile-img" />
          ) : (
            <div className="profile-placeholder" aria-hidden="true">
              <User size={20} />
            </div>
          )}
          <div className="profile-info">
            <span className="profile-name" title={user.nome}>
              {user.nome}
            </span>
            <span className="profile-role">{getRoleLabel()}</span>
          </div>
        </div>
      )}

      <nav className="sidebar-nav">
        <NavLink
          to="/"
          end
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <LayoutDashboard size={18} />
          <span>Início</span>
        </NavLink>

        <NavLink
          to="/laboratorios"
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <Building2 size={18} />
          <span>Laboratórios</span>
        </NavLink>

        <NavLink
          to="/projetos"
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <FolderKanban size={18} />
          <span>Projetos</span>
        </NavLink>

        {!isBolsista && (
          <NavLink
            to="/usuarios"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            <Users size={18} />
            <span>Usuários</span>
          </NavLink>
        )}

        <NavLink
          to="/frequencia"
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <Clock size={18} />
          <span>Frequência</span>
        </NavLink>

        {!isBolsista && (
          <NavLink
            to="/relatorios"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            <BarChart3 size={18} />
            <span>Relatórios</span>
          </NavLink>
        )}

        {!isBolsista && (
          <NavLink
            to="/auditoria"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            <ShieldCheck size={18} />
            <span>Auditoria</span>
          </NavLink>
        )}

        <NavLink
          to="/perfil"
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <User size={18} />
          <span>Meu Perfil</span>
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <button
          type="button"
          className="theme-toggle-btn"
          onClick={toggleTheme}
          aria-label={theme === 'dark' ? 'Alternar para tema claro' : 'Alternar para tema escuro'}
          title={theme === 'dark' ? 'Alternar para tema claro' : 'Alternar para tema escuro'}
        >
          {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          <span>{theme === 'dark' ? 'Tema Claro' : 'Tema Escuro'}</span>
        </button>

        <button
          type="button"
          className="logout-btn"
          onClick={handleLogout}
          aria-label="Sair da conta"
        >
          <LogOut size={18} />
          <span>Sair</span>
        </button>
      </div>
    </aside>
  );
};
