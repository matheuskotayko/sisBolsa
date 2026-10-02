import React from 'react';
import type { TipoUsuario, LaboratorioStatus } from '../../types';

interface BadgeProps {
  type: 'role' | 'status';
  value: TipoUsuario | LaboratorioStatus | string;
}

export const Badge: React.FC<BadgeProps> = ({ type, value }) => {
  if (type === 'role') {
    const roleClass =
      value === 'ADMIN'
        ? 'badge-admin'
        : value === 'PROFESSOR'
        ? 'badge-professor'
        : 'badge-bolsista';

    return <span className={`badge ${roleClass}`}>{value}</span>;
  }

  if (type === 'status') {
    const statusClass =
      value === 'Ativo'
        ? 'status-ativo'
        : value === 'Em Pausa'
        ? 'status-em-pausa'
        : 'status-concluido';

    return <span className={`badge ${statusClass}`}>{value}</span>;
  }

  return <span className="badge">{value}</span>;
};
