import { api } from './api';

export interface ResumoAdmin {
  totalBolsistas: number;
  totalLaboratorios: number;
  totalProjetos: number;
}

export interface OcupacaoLab {
  id: string;
  nome: string;
  capacidade: number;
  totalBolsistas: number;
  percentualOcupacao: number;
}

export interface HorasBolsistaMes {
  nome: string;
  totalHoras: number;
}

export interface ProjetosPorLab {
  nome: string;
  totalProjetos: number;
}

export interface BolsistasPorCargo {
  cargo: string;
  totalBolsistas: number;
}

export const relatorioService = {
  obterResumo: () =>
    api.get<ResumoAdmin>('/relatorios/resumo'),

  obterOcupacao: () =>
    api.get<OcupacaoLab[]>('/relatorios/ocupacao'),

  obterHorasMes: () =>
    api.get<HorasBolsistaMes[]>('/relatorios/horas-mes'),

  obterProjetosPorLab: () =>
    api.get<ProjetosPorLab[]>('/relatorios/projetos-por-laboratorio'),

  obterBolsistasPorCargo: () =>
    api.get<BolsistasPorCargo[]>('/relatorios/bolsistas-por-cargo'),
};
