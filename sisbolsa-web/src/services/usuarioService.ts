import { api } from './api';
import type { Usuario, UsuarioRequest, Paginacao, CargoOption, ModalidadeOption } from '../types';

export const usuarioService = {
  listar: (params?: {
    tipo?: string;
    buscaNome?: string;
    buscaCurso?: string;
    pagina?: number;
    tamanho?: number;
  }) => api.get<Paginacao<Usuario>>('/usuarios', params),

  buscarPorId: (id: string, tipo?: string) =>
    api.get<Usuario>(`/usuarios/${id}`, tipo ? { tipo } : undefined),

  criar: (dados: UsuarioRequest) =>
    api.post<Usuario>('/usuarios', dados),

  atualizar: (id: string, dados: UsuarioRequest) =>
    api.patch<Usuario>(`/usuarios/${id}`, dados),

  excluir: (id: string, tipo?: string) =>
    api.delete<void>(`/usuarios/${id}`, tipo ? { tipo } : undefined),

  listarCargos: () =>
    api.get<CargoOption[]>('/usuarios/cargos'),

  listarModalidades: () =>
    api.get<ModalidadeOption[]>('/usuarios/modalidades'),
};
