import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Lock, Mail, AlertCircle, Loader2 } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErro('');
    setLoading(true);

    try {
      await login(email.trim(), senha);
      showToast('Login realizado com sucesso!', 'sucesso');
      navigate('/');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Credenciais inválidas. Tente novamente.';
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
          maxWidth: '420px',
          padding: '36px 32px',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-lg)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <h1
            style={{
              fontFamily: 'Outfit, sans-serif',
              fontSize: '2rem',
              fontWeight: 700,
              color: 'var(--primary-color)',
              margin: '0 0 8px 0',
            }}
          >
            SisBolsa
          </h1>
          <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Sistema de Gestão de Bolsistas e Laboratórios
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

        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: '16px' }}>
            <label htmlFor="email">E-mail</label>
            <div className="password-field-wrapper">
              <input
                id="email"
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

          <div className="form-group" style={{ marginBottom: '12px' }}>
            <label htmlFor="senha">Senha</label>
            <div className="password-field-wrapper">
              <input
                id="senha"
                type="password"
                required
                placeholder="Sua senha de acesso"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
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
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '20px' }}>
            <Link
              to="/esqueci-senha"
              style={{
                fontSize: '0.85rem',
                color: 'var(--primary-color)',
                textDecoration: 'none',
                fontWeight: 500,
              }}
            >
              Esqueceu a senha?
            </Link>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            style={{ width: '100%', justifyContent: 'center', padding: '11px' }}
          >
            {loading ? (
              <>
                <Loader2 size={18} className="spin" style={{ animation: 'spin 1s linear infinite' }} />
                <span>Entrando...</span>
              </>
            ) : (
              <span>Entrar no Sistema</span>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
