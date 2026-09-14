import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Building2,
  FolderKanban,
  Users,
  Clock,
  ArrowRight,
  User,
  BarChart3,
  Briefcase,
  Layers,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { laboratorioService } from '../services/laboratorioService';
import { relatorioService } from '../services/relatorioService';
import { api } from '../services/api';
import type { Laboratorio, Projeto, Usuario } from '../types';
import { Badge } from '../components/ui/Badge';

export const Dashboard: React.FC = () => {
  const { isAdmin, isProfessor } = useAuth();

  if (isAdmin) {
    return <DashboardAdmin />;
  }

  if (isProfessor) {
    return <DashboardProfessor />;
  }

  return <DashboardBolsista />;
};

/* -------------------------------------------------------------------------
 * 1. DASHBOARD DO ADMINISTRADOR
 * ------------------------------------------------------------------------- */
const DashboardAdmin: React.FC = () => {
  const { user } = useAuth();
  const [resumo, setResumo] = useState({
    totalBolsistas: 0,
    totalLaboratorios: 0,
    totalProjetos: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function carregar() {
      try {
        const dados = await relatorioService.obterResumo();
        setResumo(dados);
      } catch {
        // fallback
      } finally {
        setLoading(false);
      }
    }
    carregar();
  }, []);

  return (
    <div>
      <div className="header-actions">
        <div>
          <h1>Painel de Controle</h1>
          <p className="header-subtitle">
            Bem-vindo(a), <strong>{user?.nome}</strong>! Visão geral institucional e acesso rápido.
          </p>
        </div>
      </div>

      <h2 className="section-title">
        <Layers size={20} />
        <span>Visão Geral da Plataforma</span>
      </h2>

      {/* KPI Cards Admin */}
      <div className="stats-grid">
        <div className="stat-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div className="value">{loading ? '...' : resumo.totalBolsistas}</div>
              <h3>Bolsistas Cadastrados</h3>
            </div>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: 'var(--primary-subtle)',
                color: 'var(--primary-color)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Users size={22} />
            </div>
          </div>
          <Link
            to="/usuarios?tipo=BOLSISTA"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              color: 'var(--primary-color)',
              fontWeight: 600,
              fontSize: '0.85rem',
              marginTop: '12px',
              textDecoration: 'none',
            }}
          >
            <span>Gerenciar bolsistas</span>
            <ArrowRight size={14} />
          </Link>
        </div>

        <div className="stat-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div className="value">{loading ? '...' : resumo.totalLaboratorios}</div>
              <h3>Laboratórios Ativos</h3>
            </div>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: '#f0fdf4',
                color: '#15803d',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Building2 size={22} />
            </div>
          </div>
          <Link
            to="/laboratorios"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              color: 'var(--primary-color)',
              fontWeight: 600,
              fontSize: '0.85rem',
              marginTop: '12px',
              textDecoration: 'none',
            }}
          >
            <span>Ver laboratórios</span>
            <ArrowRight size={14} />
          </Link>
        </div>

        <div className="stat-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div className="value">{loading ? '...' : resumo.totalProjetos}</div>
              <h3>Projetos de Pesquisa</h3>
            </div>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: '#fffbeb',
                color: '#b45309',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <FolderKanban size={22} />
            </div>
          </div>
          <Link
            to="/projetos"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              color: 'var(--primary-color)',
              fontWeight: 600,
              fontSize: '0.85rem',
              marginTop: '12px',
              textDecoration: 'none',
            }}
          >
            <span>Consultar projetos</span>
            <ArrowRight size={14} />
          </Link>
        </div>
      </div>

      <h2 className="section-title" style={{ marginTop: '32px' }}>
        <Briefcase size={20} />
        <span>Ações Rápidas de Administração</span>
      </h2>

      {/* Grid de Ações do Admin */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: '16px',
        }}
      >
        <Link
          to="/usuarios"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: 'var(--primary-subtle)',
              color: 'var(--primary-color)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Users size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Gestão de Usuários
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
              Cadastre, pesquise e gerencie permissões de bolsistas, professores e administradores.
            </p>
          </div>
        </Link>

        <Link
          to="/laboratorios"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#f0fdf4',
              color: '#15803d',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Building2 size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Laboratórios
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
              Cadastre novos laboratórios, atribua coordenadores e controle o teto de vagas.
            </p>
          </div>
        </Link>

        <Link
          to="/projetos"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#ede9fe',
              color: '#6d28d9',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <FolderKanban size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Projetos de Pesquisa
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
              Gerencie projetos institucionais e vincule bolsistas aos seus respectivos laboratórios.
            </p>
          </div>
        </Link>

        <Link
          to="/frequencia"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#fffbeb',
              color: '#b45309',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Clock size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Folha de Frequência
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
              Acompanhe e audite o registro de horas e as atividades desempenhadas pelos bolsistas.
            </p>
          </div>
        </Link>

        <Link
          to="/relatorios"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#eff6ff',
              color: 'var(--primary-color)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <BarChart3 size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Relatórios & Métricas
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
              Acesse estatísticas consolidadas de ocupação e horas da instituição.
            </p>
          </div>
        </Link>

        <Link
          to="/perfil"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#f1f5f9',
              color: 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <User size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Meu Perfil
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
              Altere seus dados cadastrais, e-mail de acesso e credenciais de segurança.
            </p>
          </div>
        </Link>
      </div>
    </div>
  );
};

/* -------------------------------------------------------------------------
 * 2. DASHBOARD DO PROFESSOR
 * ------------------------------------------------------------------------- */
const DashboardProfessor: React.FC = () => {
  const { user } = useAuth();
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function carregar() {
      try {
        const labs = await laboratorioService.listarTodos();
        setLaboratorios(labs);
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    }
    carregar();
  }, []);

  const totalBolsistas = laboratorios.reduce((acc, l) => acc + (l.totalBolsistas || 0), 0);

  return (
    <div>
      <div className="header-actions">
        <div>
          <h1>Área do Professor</h1>
          <p className="header-subtitle">
            Bem-vindo(a), <strong>{user?.nome}</strong>! Gestão dos laboratórios sob sua coordenação.
          </p>
        </div>
      </div>

      <h2 className="section-title">
        <Layers size={20} />
        <span>Atalhos Rápidos</span>
      </h2>

      {/* Grid de Atalhos do Professor */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
          gap: '16px',
          marginBottom: '28px',
        }}
      >
        <Link
          to="/usuarios"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: 'var(--primary-subtle)',
              color: 'var(--primary-color)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Users size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Gerenciar Bolsistas
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Equipe do seu laboratório
            </p>
          </div>
        </Link>

        <Link
          to="/laboratorios"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#f0fdf4',
              color: '#15803d',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Building2 size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Meus Laboratórios
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Estrutura e projetos ativos
            </p>
          </div>
        </Link>

        <Link
          to="/frequencia"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#fffbeb',
              color: '#b45309',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Clock size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Frequências da Equipe
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Auditoria de horas apontadas
            </p>
          </div>
        </Link>

        <Link
          to="/perfil"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#f1f5f9',
              color: 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <User size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Editar Perfil
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Suas credenciais e foto
            </p>
          </div>
        </Link>
      </div>

      {/* Tabela de Laboratórios Coordenados */}
      <div className="container">
        <h2 className="section-title">
          <Building2 size={20} />
          <span>Meus Laboratórios Coordenados (Total de Bolsistas: {totalBolsistas})</span>
        </h2>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Laboratório</th>
                <th>Área de Pesquisa</th>
                <th>Status</th>
                <th>Capacidade / Ocupação</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                    Carregando laboratórios...
                  </td>
                </tr>
              ) : laboratorios.length === 0 ? (
                <tr>
                  <td colSpan={5} className="empty-state-cell">
                    Você não coordena nenhum laboratório cadastrado.
                  </td>
                </tr>
              ) : (
                laboratorios.map((lab) => {
                  const percentual = Math.min(
                    100,
                    Math.round(((lab.totalBolsistas || 0) / (lab.capacidade || 1)) * 100)
                  );
                  return (
                    <tr key={lab.id}>
                      <td>
                        <Link
                          to={`/laboratorios/${lab.id}`}
                          style={{ color: 'var(--primary-color)', fontWeight: 600, textDecoration: 'none' }}
                        >
                          {lab.nome}
                        </Link>
                      </td>
                      <td>{lab.areaPesquisa}</td>
                      <td>
                        <Badge type="status" value={lab.status} />
                      </td>
                      <td style={{ minWidth: '150px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '4px' }}>
                          <span>{lab.totalBolsistas || 0} de {lab.capacidade} bolsas</span>
                          <span>{percentual}%</span>
                        </div>
                        <div className="progress-bar-container" style={{ margin: 0 }}>
                          <div
                            className={`progress-bar-fill ${percentual > 90 ? 'danger' : percentual > 70 ? 'warning' : 'success'}`}
                            style={{ width: `${percentual}%` }}
                          />
                        </div>
                      </td>
                      <td>
                        <Link
                          to={`/laboratorios/${lab.id}`}
                          className="btn btn-secondary"
                          style={{ padding: '5px 10px', fontSize: '0.8rem' }}
                        >
                          <span>Gerenciar</span>
                          <ArrowRight size={14} />
                        </Link>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

/* -------------------------------------------------------------------------
 * 3. DASHBOARD DO BOLSISTA
 * ------------------------------------------------------------------------- */
const DashboardBolsista: React.FC = () => {
  const { user } = useAuth();
  const [perfil, setPerfil] = useState<Usuario | null>(null);
  const [projetos, setProjetos] = useState<Projeto[]>([]);
  const [lab, setLab] = useState<Laboratorio | null>(null);
  const [equipe, setEquipe] = useState<Usuario[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function carregar() {
      if (!user) return;
      try {
        const [dadosPerfil, projs] = await Promise.all([
          api.get<Usuario>(`/usuarios/${user.id}`).catch(() => null),
          api.get<Projeto[]>(`/usuarios/${user.id}/projetos`).catch(() => []),
        ]);
        setPerfil(dadosPerfil);
        setProjetos(projs);

        if (user.laboratorioId) {
          const [labData, membros] = await Promise.all([
            laboratorioService.buscarPorId(user.laboratorioId).catch(() => null),
            laboratorioService.listarBolsistas(user.laboratorioId).catch(() => []),
          ]);
          setLab(labData);
          setEquipe(membros);
        }
      } finally {
        setLoading(false);
      }
    }
    carregar();
  }, [user]);

  return (
    <div>
      <div className="header-actions">
        <div>
          <h1>Área do Bolsista</h1>
          <p className="header-subtitle">
            Bem-vindo(a), <strong>{user?.nome}</strong>! Acompanhe seus projetos e aponte suas horas.
          </p>
        </div>
      </div>

      {/* Card Informativo da Bolsa */}
      {perfil?.modalidadeBolsa && (
        <div
          className="container"
          style={{
            marginBottom: '24px',
            backgroundColor: 'var(--primary-subtle)',
            border: '1px solid var(--border-color)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '16px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div
              style={{
                width: '46px',
                height: '46px',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: 'var(--primary-color)',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <Briefcase size={22} />
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--text-main)' }}>
                {perfil.modalidadeBolsaDescricao || perfil.modalidadeBolsa}
              </h2>
              <p style={{ margin: '2px 0 0 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                {perfil.valorBolsa ? `Valor: R$ ${Number(perfil.valorBolsa).toFixed(2).replace('.', ',')} / mês` : 'Pesquisador vinculado'}
                {perfil.dataFimBolsa ? ` • Vigência até ${new Date(perfil.dataFimBolsa + 'T00:00:00').toLocaleDateString('pt-BR')}` : ''}
              </p>
            </div>
          </div>
          {perfil.bolsaVencida ? (
            <span style={{ backgroundColor: '#fee2e2', color: '#dc2626', padding: '6px 12px', borderRadius: 'var(--radius-sm)', fontWeight: 600, fontSize: '0.8rem' }}>
              Vigência Encerrada
            </span>
          ) : perfil.bolsaPrestesAVencer ? (
            <span style={{ backgroundColor: '#fef3c7', color: '#d97706', padding: '6px 12px', borderRadius: 'var(--radius-sm)', fontWeight: 600, fontSize: '0.8rem' }}>
              Bolsa a Vencer
            </span>
          ) : (
            <span style={{ backgroundColor: '#dcfce7', color: '#15803d', padding: '6px 12px', borderRadius: 'var(--radius-sm)', fontWeight: 600, fontSize: '0.8rem' }}>
              Bolsa Ativa
            </span>
          )}
        </div>
      )}

      <h2 className="section-title">
        <Layers size={20} />
        <span>Atalhos Rápidos</span>
      </h2>

      {/* Grid de Atalhos do Bolsista */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '16px',
          marginBottom: '28px',
        }}
      >
        <Link
          to="/frequencia"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: 'var(--primary-subtle)',
              color: 'var(--primary-color)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Clock size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Lançar Frequência
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Registrar horas e atividades
            </p>
          </div>
        </Link>

        {user?.laboratorioId && (
          <Link
            to={`/laboratorios/${user.laboratorioId}`}
            className="container"
            style={{
              margin: 0,
              textDecoration: 'none',
              display: 'flex',
              alignItems: 'center',
              gap: '16px',
              transition: 'var(--transition-smooth)',
            }}
          >
            <div
              style={{
                width: '46px',
                height: '46px',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: '#f0fdf4',
                color: '#15803d',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <Building2 size={22} />
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
                Meu Laboratório
              </h2>
              <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                {lab?.nome || 'Ver equipe e projetos'}
              </p>
            </div>
          </Link>
        )}

        <Link
          to="/projetos"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#ede9fe',
              color: '#6d28d9',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <FolderKanban size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Visualizar Projetos
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Pesquisas em andamento
            </p>
          </div>
        </Link>

        <Link
          to="/perfil"
          className="container"
          style={{
            margin: 0,
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            transition: 'var(--transition-smooth)',
          }}
        >
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: 'var(--radius-sm)',
              backgroundColor: '#f1f5f9',
              color: 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <User size={22} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-main)', fontWeight: 600 }}>
              Meu Perfil
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Dados cadastrais e senha
            </p>
          </div>
        </Link>
      </div>

      {/* Minha Equipe */}
      <div className="container" style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 className="section-title" style={{ margin: 0 }}>
            <Users size={20} />
            <span>Minha Equipe {lab ? `(${lab.nome})` : ''}</span>
          </h2>
          {lab?.coordenador && (
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Coordenador: <strong>{lab.coordenador}</strong>
            </span>
          )}
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th>E-mail</th>
                <th>Cargo / Função</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={3} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                    Carregando equipe...
                  </td>
                </tr>
              ) : equipe.length === 0 ? (
                <tr>
                  <td colSpan={3} className="empty-state-cell">
                    Nenhum outro participante no laboratório.
                  </td>
                </tr>
              ) : (
                equipe.map((m) => (
                  <tr key={m.id} style={m.id === user?.id ? { backgroundColor: 'var(--primary-subtle)' } : undefined}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        {m.fotoUrl ? (
                          <img src={m.fotoUrl} alt={m.nome} className="profile-img" style={{ width: '32px', height: '32px' }} />
                        ) : (
                          <div className="profile-placeholder" style={{ width: '32px', height: '32px' }}>
                            <User size={16} />
                          </div>
                        )}
                        <strong>
                          {m.nome} {m.id === user?.id ? '(Você)' : ''}
                        </strong>
                      </div>
                    </td>
                    <td>{m.email}</td>
                    <td>
                      <span className="count-badge count-badge-purple">
                        {m.cargo || 'Bolsista'}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Meus Projetos */}
      <div className="container">
        <h2 className="section-title">
          <FolderKanban size={20} />
          <span>Meus Projetos Vinculados ({projetos.length})</span>
        </h2>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Projeto</th>
                <th>Descrição</th>
                <th>Laboratório</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={4} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                    Carregando projetos...
                  </td>
                </tr>
              ) : projetos.length === 0 ? (
                <tr>
                  <td colSpan={4} className="empty-state-cell">
                    Você não está vinculado a nenhum projeto no momento.
                  </td>
                </tr>
              ) : (
                projetos.map((p) => (
                  <tr key={p.id}>
                    <td><strong>{p.nome}</strong></td>
                    <td>{p.descricao || 'Sem descrição cadastrada.'}</td>
                    <td>{p.nomeLaboratorio}</td>
                    <td>
                      <Link
                        to={`/projetos/${p.id}`}
                        style={{ color: 'var(--primary-color)', fontWeight: 600, textDecoration: 'none' }}
                      >
                        Ver Detalhes &rarr;
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
