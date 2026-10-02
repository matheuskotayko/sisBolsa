export type TipoUsuario = 'ADMIN' | 'PROFESSOR' | 'BOLSISTA';
export type UserRole = TipoUsuario;

export type CargoBolsista =
  | 'DESENVOLVEDOR'
  | 'PESQUISADOR'
  | 'LIDER_TECNICO'
  | 'DESIGNER'
  | 'AUXILIAR';

export type LaboratorioStatus = 'Ativo' | 'Em Pausa' | 'Concluído';

export interface CargoOption {
  valor: CargoBolsista;
  descricao: string;
}

export interface ModalidadeOption {
  valor: string;
  descricao: string;
}

export interface Curso {
  id: string;
  nome: string;
}

export interface UsuarioAuth {
  id: string;
  nome: string;
  email: string;
  tipoUsuario: TipoUsuario;
  fotoUrl?: string | null;
  laboratorioId?: string | null;
}

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  tipoUsuario: TipoUsuario;
  fotoUrl: string | null;
  bio: string | null;
  ativo: boolean;
  curso?: string | null;
  matricula?: string | null;
  cpf?: string | null;
  telefone?: string | null;
  dataNascimento?: string | null;
  laboratorioId?: string | null;
  nomeLaboratorio?: string | null;
  cargo?: CargoBolsista | null;
  modalidadeBolsa?: string | null;
  modalidadeBolsaDescricao?: string | null;
  valorBolsa?: number | null;
  dataInicioBolsa?: string | null;
  dataFimBolsa?: string | null;
  bolsaVencida?: boolean;
  bolsaPrestesAVencer?: boolean;
}

export interface UsuarioRequest {
  nome: string;
  email: string;
  senha?: string | null;
  tipoUsuario: TipoUsuario;
  curso?: string | null;
  matricula?: string | null;
  cpf?: string | null;
  telefone?: string | null;
  dataNascimento?: string | null;
  laboratorioId?: string | null;
  cargo?: CargoBolsista | string | null;
  modalidadeBolsa?: string | null;
  valorBolsa?: number | null;
  dataInicioBolsa?: string | null;
  dataFimBolsa?: string | null;
  fotoUrl?: string | null;
  bio?: string | null;
}

export interface Laboratorio {
  id: string;
  nome: string;
  areaPesquisa: string;
  status: LaboratorioStatus;
  capacidade: number;
  coordenadorId: string | null;
  coordenador: string | null;
  totalBolsistas: number;
  ativo: boolean;
  projetos?: Projeto[];
}

export interface LaboratorioRequest {
  nome: string;
  areaPesquisa: string;
  status: LaboratorioStatus;
  capacidade: number;
  coordenadorId: string | null;
}

export interface Projeto {
  id: string;
  nome: string;
  descricao: string | null;
  laboratorioId: string;
  nomeLaboratorio: string;
  ativo: boolean;
  totalMembros?: number;
  linkRepositorio?: string | null;
  linkDocumentacao?: string | null;
  membros?: MembroProjeto[];
}

export interface ProjetoRequest {
  nome: string;
  descricao?: string | null;
  laboratorioId: string;
  linkRepositorio?: string | null;
  linkDocumentacao?: string | null;
}

export interface MembroProjeto {
  id: string;
  nome: string;
  email: string;
  curso?: string | null;
  cargo?: string | null;
  fotoUrl?: string | null;
}

export interface Frequencia {
  id: string;
  bolsistaId: string;
  nomeBolsista: string;
  data: string;
  horasTrabalhadas: number;
  descricao: string;
  linkComprovante?: string | null;
  ativo: boolean;
}

export interface FrequenciaRequest {
  bolsistaId?: string | null;
  data: string;
  horasTrabalhadas: number;
  descricao: string;
  linkComprovante?: string | null;
}

export interface FrequenciaResumo {
  horasMes: number;
  horasTotal: number;
}

export interface DashboardMetricas {
  totalBolsistas: number;
  totalLaboratorios: number;
  totalProjetos: number;
  totalHorasMes: number;
}

export interface RelatorioItem {
  id: string;
  nome: string;
  capacidade: number;
  ocupacao: number;
  percentual: number;
}

export interface Auditoria {
  id: string;
  usuarioId?: string | null;
  usuarioNome: string;
  acao: string;
  entidade: string;
  detalhes?: string | null;
  ipOrigem?: string | null;
  dataHora: string;
}

export interface Paginacao<T> {
  itens: T[];
  pagina: number;
  totalPaginas: number;
  totalItens: number;
}
