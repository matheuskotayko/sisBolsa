import { api, BASE_URL } from './api';
import type { Auditoria, Paginacao } from '../types';

export const auditoriaService = {
  listar: (params?: {
    pagina?: number;
    entidade?: string;
    acao?: string;
    dataInicio?: string;
    dataFim?: string;
  }) => api.get<Paginacao<Auditoria>>('/auditoria', params),

  exportarUrl: (params?: {
    entidade?: string;
    acao?: string;
    dataInicio?: string;
    dataFim?: string;
  }) => {
    const qs = new URLSearchParams();
    if (params?.entidade) qs.append('entidade', params.entidade);
    if (params?.acao) qs.append('acao', params.acao);
    if (params?.dataInicio) qs.append('dataInicio', params.dataInicio);
    if (params?.dataFim) qs.append('dataFim', params.dataFim);
    const str = qs.toString();
    return `${BASE_URL}/auditoria/exportar${str ? `?${str}` : ''}`;
  },
};
