import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

export const NotFound: React.FC = () => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '70vh',
        textAlign: 'center',
      }}
    >
      <h1 style={{ fontSize: '4rem', margin: 0, fontFamily: 'Outfit, sans-serif', color: 'var(--primary-color)' }}>
        404
      </h1>
      <h2 style={{ fontSize: '1.5rem', margin: '8px 0 16px 0' }}>Página Não Encontrada</h2>
      <p style={{ color: 'var(--text-muted)', maxWidth: '400px', marginBottom: '24px' }}>
        O endereço que você tentou acessar não existe ou você não possui permissão para visualizá-lo.
      </p>
      <Link to="/" className="btn btn-primary">
        <ArrowLeft size={16} />
        <span>Voltar ao Início</span>
      </Link>
    </div>
  );
};
