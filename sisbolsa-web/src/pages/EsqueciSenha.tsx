import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  Mail,
  KeyRound,
  Lock,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Eye,
  EyeOff,
  Shield,
  Check,
  X,
} from 'lucide-react';
import { authService } from '../services/authService';
import { useToast } from '../contexts/ToastContext';

export const EsqueciSenha: React.FC = () => {
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [etapa, setEtapa] = useState<'solicitar' | 'redefinir' | 'sucesso'>('solicitar');
  const [email, setEmail] = useState('');
  const [codigo, setCodigo] = useState('');
  const [codigoDev, setCodigoDev] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmaSenha, setConfirmaSenha] = useState('');

  const [showNovaSenha, setShowNovaSenha] = useState(false);
  const [showConfirmaSenha, setShowConfirmaSenha] = useState(false);
  const [erro, setErro] = useState('');
  const [loading, setLoading] = useState(false);

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

  const forca = calcularForcaSenha(novaSenha);
  const senhasConferem = novaSenha && confirmaSenha ? novaSenha === confirmaSenha : null;

  // Etapa 1: Enviar Código
  const handleSolicitarCodigo = async (e: React.FormEvent) => {
    e.preventDefault();
    setErro('');

    if (!email.trim()) {
      setErro('Informe um e-mail válido.');
      return;
    }

    setLoading(true);
    try {
      const res = await authService.esqueciSenha(email.trim());
      if (res.codigoDev) {
        setCodigoDev(res.codigoDev);
        setCodigo(res.codigoDev);
      }
      showToast(res.mensagem || 'Código de recuperação gerado!', 'sucesso');
      setEtapa('redefinir');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao solicitar código de recuperação.';
      setErro(msg);
    } finally {
      setLoading(false);
    }
  };

  // Etapa 2: Redefinir Senha
  const handleRedefinirSenha = async (e: React.FormEvent) => {
    e.preventDefault();
    setErro('');

    if (!codigo.trim() || codigo.trim().length !== 6) {
      setErro('Informe o código numérico de 6 dígitos.');
      return;
    }

    if (novaSenha.length < 6) {
      setErro('A nova senha deve ter pelo menos 6 caracteres.');
      return;
    }

    if (novaSenha !== confirmaSenha) {
      setErro('A nova senha e a confirmação não conferem.');
      return;
    }

    setLoading(true);
    try {
      const res = await authService.redefinirSenha({
        email: email.trim(),
        codigo: codigo.trim(),
        novaSenha,
        confirmaSenha,
      });

      showToast(res.mensagem || 'Senha redefinida com sucesso!', 'sucesso');
      setEtapa('sucesso');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao redefinir senha.';
      setErro(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        backgroundColor: 'var(--bg-color)',
        padding: '20px',
      }}
    >
      <div
        className="container"
        style={{
          width: '100%',
          maxWidth: '440px',
          padding: '36px 32px',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-lg)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <h1
            style={{
              fontFamily: 'Outfit, sans-serif',
              fontSize: '1.8rem',
              fontWeight: 700,
              color: 'var(--primary-color)',
              margin: '0 0 6px 0',
            }}
          >
            SisBolsa
          </h1>
          <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Recuperação de Acesso e Redefinição de Senha
          </p>
        </div>

        {erro && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '12px 14px',
              backgroundColor: 'var(--danger-bg)',
              color: 'var(--danger-color)',
              border: '1px solid var(--danger-border)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '0.85rem',
              marginBottom: '20px',
            }}
          >
            <AlertCircle size={18} style={{ flexShrink: 0 }} />
            <span>{erro}</span>
          </div>
        )}

        {etapa === 'solicitar' && (
          <form onSubmit={handleSolicitarCodigo}>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '20px', lineHeight: 1.5 }}>
              Digite o e-mail cadastrado na sua conta para enviarmos um código de verificação temporário.
            </p>

            <div className="form-group" style={{ marginBottom: '24px' }}>
              <label htmlFor="reset-email">E-mail Cadastrado</label>
              <div className="password-field-wrapper">
                <input
                  id="reset-email"
                  type="email"
                  required
                  placeholder="seu.email@sisbolsa.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  style={{ paddingLeft: '38px' }}
                />
                <Mail
                  size={16}
                  style={{
                    position: 'absolute',
                    left: '12px',
                    color: 'var(--text-tertiary)',
                  }}
                />
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{ width: '100%', justifyContent: 'center', padding: '11px', marginBottom: '16px' }}
            >
              {loading ? (
                <>
                  <Loader2 size={18} className="spin" />
                  <span>Enviando código...</span>
                </>
              ) : (
                <span>Enviar Código de Verificação</span>
              )}
            </button>

            <div style={{ textAlign: 'center' }}>
              <Link
                to="/login"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  fontSize: '0.85rem',
                  color: 'var(--text-muted)',
                  textDecoration: 'none',
                }}
              >
                <ArrowLeft size={14} />
                <span>Voltar para o Login</span>
              </Link>
            </div>
          </form>
        )}

        {etapa === 'redefinir' && (
          <form onSubmit={handleRedefinirSenha}>
            <div
              style={{
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                border: '1px solid rgba(59, 130, 246, 0.2)',
                padding: '12px',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.85rem',
                color: 'var(--primary-color)',
                marginBottom: '20px',
              }}
            >
              Código enviado para <strong>{email}</strong>. Válido por 15 minutos.
              {codigoDev && (
                <div style={{ marginTop: '4px', fontWeight: 'bold' }}>
                  Código de Verificação: <span style={{ letterSpacing: '2px', fontSize: '1rem' }}>{codigoDev}</span>
                </div>
              )}
            </div>

            <div className="form-group" style={{ marginBottom: '16px' }}>
              <label htmlFor="codigo-verificacao">Código de 6 Dígitos</label>
              <div className="password-field-wrapper">
                <input
                  id="codigo-verificacao"
                  type="text"
                  required
                  maxLength={6}
                  placeholder="000000"
                  value={codigo}
                  onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ''))}
                  style={{
                    paddingLeft: '38px',
                    letterSpacing: '4px',
                    fontSize: '1.1rem',
                    fontWeight: 600,
                  }}
                />
                <KeyRound
                  size={16}
                  style={{
                    position: 'absolute',
                    left: '12px',
                    color: 'var(--text-tertiary)',
                  }}
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '16px' }}>
              <label htmlFor="nova-senha">Nova Senha</label>
              <div className="password-field-wrapper">
                <input
                  id="nova-senha"
                  type={showNovaSenha ? 'text' : 'password'}
                  required
                  minLength={6}
                  placeholder="Mínimo de 6 caracteres"
                  value={novaSenha}
                  onChange={(e) => setNovaSenha(e.target.value)}
                  style={{ paddingLeft: '38px' }}
                />
                <Lock
                  size={16}
                  style={{
                    position: 'absolute',
                    left: '12px',
                    color: 'var(--text-tertiary)',
                  }}
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

              {novaSenha && (
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

            <div className="form-group" style={{ marginBottom: '24px' }}>
              <label htmlFor="confirma-nova-senha">Confirmar Nova Senha</label>
              <div className="password-field-wrapper">
                <input
                  id="confirma-nova-senha"
                  type={showConfirmaSenha ? 'text' : 'password'}
                  required
                  placeholder="Repita a nova senha"
                  value={confirmaSenha}
                  onChange={(e) => setConfirmaSenha(e.target.value)}
                  style={{ paddingLeft: '38px' }}
                />
                <Lock
                  size={16}
                  style={{
                    position: 'absolute',
                    left: '12px',
                    color: 'var(--text-tertiary)',
                  }}
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

            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{ width: '100%', justifyContent: 'center', padding: '11px', marginBottom: '16px' }}
            >
              {loading ? (
                <>
                  <Loader2 size={18} className="spin" />
                  <span>Redefinindo senha...</span>
                </>
              ) : (
                <span>Redefinir Senha</span>
              )}
            </button>

            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
              <button
                type="button"
                className="btn-cancel"
                style={{ background: 'none', border: 'none', padding: 0, cursor: 'pointer' }}
                onClick={() => setEtapa('solicitar')}
              >
                Trocar e-mail
              </button>
              <Link to="/login" style={{ color: 'var(--text-muted)', textDecoration: 'none' }}>
                Cancelar
              </Link>
            </div>
          </form>
        )}

        {etapa === 'sucesso' && (
          <div style={{ textAlign: 'center', padding: '12px 0' }}>
            <CheckCircle2 size={54} style={{ color: 'var(--success-color)', margin: '0 auto 16px' }} />
            <h2 style={{ fontSize: '1.3rem', marginBottom: '8px' }}>Senha Redefinida!</h2>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '24px', lineHeight: 1.5 }}>
              Sua senha foi alterada com sucesso. Agora você já pode acessar sua conta utilizando as novas credenciais.
            </p>

            <button
              type="button"
              className="btn btn-primary"
              onClick={() => navigate('/login')}
              style={{ width: '100%', justifyContent: 'center', padding: '11px' }}
            >
              Ir para a Tela de Login
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
