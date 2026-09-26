import { api, BASE_URL } from './api';
import type { Frequencia, FrequenciaRequest, FrequenciaResumo, Paginacao } from '../types';

export const frequenciaService = {
  listar: (params?: {
    bolsistaId?: string;
    dataInicio?: string;
    dataFim?: string;
    pagina?: number;
    tamanho?: number;
  }) => api.get<Paginacao<Frequencia>>('/frequencias', params),

  buscarPorId: (id: string) =>
    api.get<Frequencia>(`/frequencias/${id}`),

  criar: (dados: FrequenciaRequest) =>
    api.post<Frequencia>('/frequencias', dados),

  atualizar: (id: string, dados: FrequenciaRequest) =>
    api.put<Frequencia>(`/frequencias/${id}`, dados),

  excluir: (id: string) =>
    api.delete<void>(`/frequencias/${id}`),

  obterResumo: (bolsistaId?: string) =>
    api.get<FrequenciaResumo>('/frequencias/resumo', bolsistaId ? { bolsistaId } : undefined),

  obterUrlPdf: (params?: { bolsistaId?: string; dataInicio?: string; dataFim?: string }) => {
    const qs = new URLSearchParams();
    if (params?.bolsistaId) qs.append('bolsistaId', params.bolsistaId);
    if (params?.dataInicio) qs.append('dataInicio', params.dataInicio);
    if (params?.dataFim) qs.append('dataFim', params.dataFim);
    const str = qs.toString();
    return `${BASE_URL}/frequencias/comprovante-pdf${str ? `?${str}` : ''}`;
  },
};
