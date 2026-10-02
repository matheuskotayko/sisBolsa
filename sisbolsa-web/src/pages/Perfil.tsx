import React, { useEffect, useState } from 'react';
import { User, Lock, Save, Loader2, Eye, EyeOff, Check, X, Shield } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { authService } from '../services/authService';
import { Badge } from '../components/ui/Badge';

export const Perfil: React.FC = () => {
  const { user, setUser } = useAuth();
  const { showToast } = useToast();

  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [fotoUrl, setFotoUrl] = useState('');
  const [senhaAtual, setSenhaAtual] = useState('');
  const [senha, setSenha] = useState('');
  const [confirmaSenha, setConfirmaSenha] = useState('');

  const [showSenhaAtual, setShowSenhaAtual] = useState(false);
  const [showNovaSenha, setShowNovaSenha] = useState(false);
  const [showConfirmaSenha, setShowConfirmaSenha] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (user) {
      setNome(user.nome || '');
      setEmail(user.email || '');
      setFotoUrl(user.fotoUrl || '');
    }
  }, [user]);

  // Cálculo de Força de Senha
  const calcularForcaSenha = (s: string): { nivel: number; texto: string; cor: string } => {
    if (!s) return { nivel: 0, texto: '', cor: '' };
    let score = 0;
    if (s.length >= 6) score++;
    if (s.length >= 8) score++;
    if (/[A-Z]/.test(s) && /[a-z]/.test(s)) score++;
    if (/[0-9]/.test(s)) score++;
    if (/[^A-Za-z0-9]/.test(s)) score++;

    if (score <= 2) return { nivel: 1, texto: 'Fraca', cor: '#ef4444' };
    if (score <= 3) return { nivel: 2, texto: 'Média', cor: '#f59e0b' };
    return { nivel: 3, texto: 'Forte', cor: '#10b981' };
  };

  const forca = calcularForcaSenha(senha);
  const senhasConferem = senha && confirmaSenha ? senha === confirmaSenha : null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!nome.trim() || nome.length < 3) {
      showToast('O nome completo deve ter pelo menos 3 caracteres.', 'erro');
      return;
    }

    if (!email.trim()) {
      showToast('Informe um e-mail válido.', 'erro');
      return;
    }

    if (senha || confirmaSenha || senhaAtual) {
      if (!senhaAtual) {
        showToast('Informe sua senha atual para autorizar a alteração de credenciais.', 'erro');
        return;
      }
      if (senha.length < 6) {
        showToast('A nova senha deve ter pelo menos 6 caracteres.', 'erro');
        return;
      }
      if (senha !== confirmaSenha) {
        showToast('A confirmação da nova senha não confere.', 'erro');
        return;
      }
    }

    setSaving(true);
    try {
      const atualizado = await authService.atualizarPerfil({
        nome: nome.trim(),
        email: email.trim(),
        fotoUrl: fotoUrl.trim() || null,
        senhaAtual: senhaAtual || undefined,
        senha: senha || undefined,
        confirmaSenha: confirmaSenha || undefined,
      });

      setUser(atualizado);
      setSenhaAtual('');
      setSenha('');
      setConfirmaSenha('');
      showToast('Perfil e credenciais atualizados com sucesso!', 'sucesso');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao atualizar perfil';
      showToast(msg, 'erro');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div className="header-actions">
        <div>
          <h1>Meu Perfil</h1>
          <p className="header-subtitle">
            Gerenciamento de credenciais de acesso, dados cadastrais e foto de identificação
          </p>
        </div>
      </div>

      <div style={{ maxWidth: '800px' }}>
        <form onSubmit={handleSubmit}>
          {/* Card Informações Pessoais */}
          <div className="container" style={{ marginBottom: '24px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '20px', marginBottom: '24px' }}>
              {fotoUrl ? (
                <img
                  src={fotoUrl}
                  alt={nome}
                  className="profile-img"
                  style={{ width: '72px', height: '72px', borderWidth: '3px' }}
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = '';
                  }}
                />
              ) : (
                <div
                  className="profile-placeholder"
                  style={{ width: '72px', height: '72px', fontSize: '1.8rem' }}
                >
                  <User size={36} />
                </div>
              )}
              <div>
                <h2 style={{ margin: '0 0 6px 0', fontSize: '1.3rem', fontFamily: 'Outfit, sans-serif' }}>
                  {nome || 'Seu Nome'}
                </h2>
                {user && <Badge type="role" value={user.tipoUsuario} />}
              </div>
            </div>

            <h2 className="section-title">
              <User size={20} />
              <span>Informações Pessoais</span>
            </h2>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '16px', marginBottom: '16px' }}>
              <div className="form-group">
                <label htmlFor="perfil-nome">
                  Nome Completo <span className="asterisco">*</span>
                </label>
                <input
                  id="perfil-nome"
                  type="text"
                  required
                  minLength={3}
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label htmlFor="perfil-email">
                  E-mail de Acesso <span className="asterisco">*</span>
                </label>
                <input
                  id="perfil-email"
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label htmlFor="perfil-foto">URL da Foto de Perfil</label>
                <input
                  id="perfil-foto"
                  type="url"
                  placeholder="https://exemplo.com/foto.jpg"
                  value={fotoUrl}
                  onChange={(e) => setFotoUrl(e.target.value)}
                />
              </div>
            </div>
          </div>

          {/* Card Segurança e Senha */}
          <div className="container" style={{ marginBottom: '24px' }}>
            <h2 className="section-title">
              <Lock size={20} />
              <span>Segurança e Alteração de Senha</span>
            </h2>
            <p style={{ margin: '0 0 16px 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Deixe os campos abaixo em branco se não desejar alterar sua senha.
            </p>

            <div className="form-group" style={{ marginBottom: '16px' }}>
              <label htmlFor="perfil-senha-atual">Senha Atual</label>
              <div className="password-field-wrapper">
                <input
                  id="perfil-senha-atual"
                  type={showSenhaAtual ? 'text' : 'password'}
                  placeholder="Digite sua senha atual para autorizar alterações"
                  value={senhaAtual}
                  onChange={(e) => setSenhaAtual(e.target.value)}
                />
                <button
                  type="button"
                  className="password-toggle-btn"
                  onClick={() => setShowSenhaAtual(!showSenhaAtual)}
                  aria-label={showSenhaAtual ? 'Ocultar senha' : 'Exibir senha'}
                >
                  {showSenhaAtual ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label htmlFor="perfil-nova-senha">Nova Senha</label>
                <div className="password-field-wrapper">
                  <input
                    id="perfil-nova-senha"
                    type={showNovaSenha ? 'text' : 'password'}
                    minLength={6}
                    placeholder="Mínimo de 6 caracteres"
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                  />
                  <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowNovaSenha(!showNovaSenha)}
                    aria-label={showNovaSenha ? 'Ocultar senha' : 'Exibir senha'}
                  >
                    {showNovaSenha ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>

                {senha && (
                  <div style={{ marginTop: '8px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '4px' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--text-muted)' }}>
                        <Shield size={12} /> Força:
                      </span>
                      <strong style={{ color: forca.cor }}>{forca.texto}</strong>
                    </div>
                    <div style={{ display: 'flex', gap: '4px', height: '4px' }}>
                      <div style={{ flex: 1, borderRadius: '2px', backgroundColor: forca.nivel >= 1 ? forca.cor : 'var(--border-grid)' }} />
                      <div style={{ flex: 1, borderRadius: '2px', backgroundColor: forca.nivel >= 2 ? forca.cor : 'var(--border-grid)' }} />
                      <div style={{ flex: 1, borderRadius: '2px', backgroundColor: forca.nivel >= 3 ? forca.cor : 'var(--border-grid)' }} />
                    </div>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="perfil-confirma-senha">Confirmar Nova Senha</label>
                <div className="password-field-wrapper">
                  <input
                    id="perfil-confirma-senha"
                    type={showConfirmaSenha ? 'text' : 'password'}
                    placeholder="Repita a nova senha"
                    value={confirmaSenha}
                    onChange={(e) => setConfirmaSenha(e.target.value)}
                  />
                  <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowConfirmaSenha(!showConfirmaSenha)}
                    aria-label={showConfirmaSenha ? 'Ocultar senha' : 'Exibir senha'}
                  >
                    {showConfirmaSenha ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>

                {confirmaSenha && (
                  <div style={{ marginTop: '6px', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    {senhasConferem ? (
                      <span style={{ color: 'var(--success-color)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Check size={12} /> Senhas coincidem
                      </span>
                    ) : (
                      <span style={{ color: 'var(--danger-color)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <X size={12} /> Senhas não conferem
                      </span>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? <Loader2 size={16} className="spin" /> : <Save size={16} />}
              <span>Salvar Alterações do Perfil</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
