import { api } from './api';
import type { Laboratorio, LaboratorioRequest, Projeto, Usuario, Paginacao } from '../types';

/* usado quando o chamador so quer "todos os laboratorios" pra popular um
 * dropdown/lookup, sem controle de pagina - suficiente enquanto o numero de
 * laboratorios de um departamento nao passar disso. */
const TAMANHO_TODOS = 200;

export const laboratorioService = {
  listar: (params?: { pagina?: number; tamanho?: number; buscaNome?: string }) =>
    api.get<Paginacao<Laboratorio>>('/laboratorios', params),

  listarTodos: () =>
    api.get<Paginacao<Laboratorio>>('/laboratorios', { tamanho: TAMANHO_TODOS }).then((r) => r.itens),

  buscarPorId: (id: string) =>
    api.get<Laboratorio>(`/laboratorios/${id}`),

  criar: (dados: LaboratorioRequest) =>
    api.post<Laboratorio>('/laboratorios', dados),

  atualizar: (id: string, dados: LaboratorioRequest) =>
    api.put<Laboratorio>(`/laboratorios/${id}`, dados),

  excluir: (id: string) =>
    api.delete<void>(`/laboratorios/${id}`),

  listarProjetos: (id: string) =>
    api.get<Projeto[]>(`/laboratorios/${id}/projetos`),

  listarBolsistas: (id: string) =>
    api.get<Usuario[]>(`/laboratorios/${id}/bolsistas`),
};
