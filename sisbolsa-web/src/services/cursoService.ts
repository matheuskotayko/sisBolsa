import { api } from './api';
import type { Curso } from '../types';

export const cursoService = {
  listar: () => api.get<Curso[]>('/cursos'),

  criar: (nome: string) => api.post<Curso>('/cursos', { nome }),
};
