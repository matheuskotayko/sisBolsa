import { api } from './api';
import type { UsuarioAuth } from '../types';

export const authService = {
  login: (email: string, senha: string) =>
    api.post<UsuarioAuth>('/auth/login', { email, senha }),

  logout: () =>
    api.post<void>('/auth/logout'),

  obterUsuarioAtual: () =>
    api.get<UsuarioAuth>('/auth/me'),

  atualizarPerfil: (dados: {
    nome: string;
    email: string;
    fotoUrl?: string | null;
    senhaAtual?: string;
    senha?: string;
    confirmaSenha?: string;
  }) => api.patch<UsuarioAuth>('/auth/perfil', dados),

  esqueciSenha: (email: string) =>
    api.post<{ mensagem: string; codigoDev?: string }>('/auth/password-reset-requests', { email }),

  redefinirSenha: (dados: { email: string; codigo: string; novaSenha: string; confirmaSenha: string }) =>
    api.post<{ mensagem: string }>('/auth/password-resets', dados),
};
