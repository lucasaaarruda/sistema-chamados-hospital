import { useEffect, useState } from 'react';
import { listTickets, updateTicketStatus, createTicket, Ticket, updateMe } from '../lib/api';
import { useAuth } from '../contexts/AuthContext';

export function Dashboard() {
  const { signOut, user, refreshMe } = useAuth();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeSection, setActiveSection] = useState<'cadastrar' | 'listar' | 'conta'>('cadastrar');
  const [novaDescricao, setNovaDescricao] = useState('');
  const [novaPrioridade, setNovaPrioridade] = useState<'Alta' | 'Média' | 'Baixa'>('Média');
  const [creating, setCreating] = useState(false);
  const [novoTitulo, setNovoTitulo] = useState('');
  const [novoSolicitante, setNovoSolicitante] = useState('');
  const [novoSetor, setNovoSetor] = useState('');
  const [showFinished, setShowFinished] = useState(false);
  const [accountName, setAccountName] = useState('');
  const [accountEmail, setAccountEmail] = useState('');
  const [accountSector, setAccountSector] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);

  useEffect(() => {
    if (user?.role === 'tecnico') {
      setActiveSection('listar');
    }
    setNovoSolicitante(user?.name || user?.email || '');
    setNovoSetor(user?.sector ?? '');
    setAccountName(user?.name || '');
    setAccountEmail(user?.email || '');
    setAccountSector(user?.sector ?? '');
    loadTickets();
  }, [user?.role]);

  const loadTickets = async () => {
    try {
      const data = await listTickets();
      const visible = (data || []).filter(t => showFinished ? true : (t.status !== 'Resolvido' && t.status !== 'Fechado'));
      setTickets(visible);
    } catch (error) {
      console.error('Erro ao carregar tickets:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadTicketsWithFilter = async (includeFinished: boolean) => {
    try {
      const data = await listTickets();
      const visible = (data || []).filter(t => includeFinished ? true : (t.status !== 'Resolvido' && t.status !== 'Fechado'));
      setTickets(visible);
    } catch (error) {
      console.error('Erro ao carregar tickets:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleTicketCreated = (newTicket: Ticket) => {
    setTickets(prev => [newTicket, ...prev]);
  };

  

  const handleCreateQuick = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!novaDescricao.trim()) {
      alert('Informe a descrição do chamado');
      return;
    }
    setCreating(true);
    try {
      const payload = {
        title: (novoTitulo || novaDescricao.split('\n')[0] || 'Chamado').slice(0, 60),
        description: novaDescricao,
        category: 'TI',
        priority: novaPrioridade,
        location: 'Geral',
        requester_name: novoSolicitante || user?.name || user?.email || 'Usuário',
        requester_sector: novoSetor || undefined,
        responsible_name: ''
      };
      const created = await createTicket(payload);
      handleTicketCreated(created);
      setNovaDescricao('');
      setNovoTitulo('');
      setNovoSolicitante(user?.name || user?.email || '');
      setNovoSetor(user?.sector ?? '');
      setNovaPrioridade('Média');
      setActiveSection('listar');
    } catch (error) {
      alert('Erro ao cadastrar chamado');
      console.error('Erro ao criar chamado:', error);
    } finally {
      setCreating(false);
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'Urgente': return 'bg-red-100 text-red-800';
      case 'Alta': return 'bg-orange-100 text-orange-800';
      case 'Média': return 'bg-yellow-100 text-yellow-800';
      case 'Baixa': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'Aberto': return 'bg-blue-100 text-blue-800';
      case 'Em Andamento': return 'bg-yellow-100 text-yellow-800';
      case 'Resolvido': return 'bg-green-100 text-green-800';
      case 'Fechado': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const isFinished = (status: string) => status === 'Resolvido' || status === 'Fechado';
  const priorityOrder: Record<string, number> = { 'Urgente': 0, 'Alta': 1, 'Média': 2, 'Baixa': 3 };
  const comparePriority = (a: Ticket, b: Ticket) => {
    const pa = priorityOrder[a.priority] ?? 999;
    const pb = priorityOrder[b.priority] ?? 999;
    if (pa !== pb) return pa - pb;
    // desempate por data (mais recentes primeiro)
    const da = Date.parse(a.created_at || '');
    const db = Date.parse(b.created_at || '');
    return (isNaN(db) ? 0 : db) - (isNaN(da) ? 0 : da);
  };
  const renderRow = (ticket: Ticket) => (
    <tr key={ticket.id} className="hover:bg-gray-50">
      <td className="px-6 py-4">
        <div className="text-sm font-medium text-gray-900">{ticket.title}</div>
        <div className="text-sm text-gray-500 truncate max-w-xs">{ticket.description}</div>
      </td>
      <td className="px-6 py-4">
        <span className={`px-3 py-1 rounded-full text-xs font-medium ${getPriorityColor(ticket.priority)}`}>{ticket.priority}</span>
      </td>
      <td className="px-6 py-4">
        <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(ticket.status)}`}>
          {ticket.status}
        </span>
      </td>
      <td className="px-6 py-4 text-sm text-gray-700">
        {ticket.requester_name}
        {ticket.requester_sector ? (
          <span className="text-gray-500"> ({ticket.requester_sector})</span>
        ) : null}
      </td>
      <td className="px-6 py-4">
        {user?.role === 'tecnico' ? (
          <div className="flex items-center gap-2">
            <button
              onClick={() => updateTicketStatus(ticket.id, 'Em Andamento')
                .then(() => setTickets(prev => prev.map(t => t.id === ticket.id ? { ...t, status: 'Em Andamento' } : t)))
                .catch(() => alert('Erro ao iniciar chamado'))}
              className="px-3 py-1 text-xs bg-yellow-600 text-white rounded hover:bg-yellow-700"
            >
              EM ANDAMENTO
            </button>
            <button
              onClick={() => updateTicketStatus(ticket.id, 'Resolvido')
                .then(() => setTickets(prev => showFinished
                  ? prev.map(t => t.id === ticket.id ? { ...t, status: 'Resolvido' } : t)
                  : prev.filter(t => t.id !== ticket.id)))
                .catch(() => alert('Erro ao finalizar chamado'))}
              className="px-3 py-1 text-xs bg-green-600 text-white rounded hover:bg-green-700"
            >
              FINALIZAR
            </button>
          </div>
        ) : (
          <span className="text-xs text-gray-500">Sem ações</span>
        )}
      </td>
    </tr>
  );

  return (
    <div className="min-h-screen bg-gray-100 flex">
      <aside className="w-80 bg-[#253645] text-white flex-shrink-0">
        <div className="py-8 text-center font-bold tracking-wide">MENU PRINCIPAL</div>
        <div className="px-4 pb-8">
          <div className="bg-[#2e4353] rounded-md p-4 space-y-6">
            {user?.role !== 'tecnico' && (
              <button
                onClick={() => setActiveSection('cadastrar')}
                className={`w-full py-6 font-semibold ${activeSection === 'cadastrar' ? 'text-white' : 'text-gray-200'} hover:text-white`}
              >
                CADASTRAR CHAMADO
              </button>
            )}
            <button
              onClick={() => setActiveSection('listar')}
              className={`w-full py-6 font-semibold ${activeSection === 'listar' ? 'text-white' : 'text-gray-200'} hover:text-white`}
            >
              LISTAR CHAMADOS
            </button>
            <button
              onClick={() => setActiveSection('conta')}
              className={`w-full py-6 font-semibold ${activeSection === 'conta' ? 'text-white' : 'text-gray-200'} hover:text-white`}
            >
              MINHA CONTA
            </button>
          </div>
        </div>
      </aside>

      <div className="flex-1 p-10">
        {activeSection === 'cadastrar' && user?.role !== 'tecnico' && (
          <div className="bg-white border rounded-md shadow-sm max-w-2xl mx-auto">
            <div className="px-6 py-4 border-b">
              <h2 className="font-semibold tracking-wide">CADASTRO DE NOVO CHAMADO</h2>
            </div>
            <form onSubmit={handleCreateQuick} className="px-6 py-6 space-y-6">
              <div>
                <label className="block text-sm font-medium mb-2">Nome do Chamado:</label>
                <input
                  type="text"
                  value={novoTitulo}
                  onChange={(e) => setNovoTitulo(e.target.value)}
                  className="w-full border rounded-md p-2 bg-gray-50"
                  placeholder="Digite um título curto para o chamado"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">Descrição:</label>
                <textarea
                  value={novaDescricao}
                  onChange={(e) => setNovaDescricao(e.target.value)}
                  rows={4}
                  className="w-full border rounded-md p-2 bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">Nome do Solicitante:</label>
                  <input
                    type="text"
                    value={novoSolicitante}
                    onChange={(e) => setNovoSolicitante(e.target.value)}
                    className="w-full border rounded-md p-2 bg-gray-50"
                    placeholder="Seu nome"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">Setor do Solicitante:</label>
                  <input
                    type="text"
                    value={novoSetor}
                    onChange={(e) => setNovoSetor(e.target.value)}
                    className="w-full border rounded-md p-2 bg-gray-50"
                    placeholder="Ex.: Enfermagem, TI, Administrativo"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">Prioridade:</label>
                <select
                  value={novaPrioridade}
                  onChange={(e) => setNovaPrioridade(e.target.value as 'Alta' | 'Média' | 'Baixa')}
                  className="border rounded-md p-2 bg-white"
                >
                  <option>Alta</option>
                  <option>Média</option>
                  <option>Baixa</option>
                </select>
              </div>
              <div className="pt-2">
                <button
                  type="submit"
                  disabled={creating}
                  className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-60"
                >
                  {creating ? 'Cadastrando...' : 'Cadastrar'}
                </button>
              </div>
            </form>
          </div>
        )}

        {activeSection === 'listar' && (
          <div className="bg-white border rounded-md shadow-sm">
            <div className="px-6 py-4 border-b">
              <h2 className="font-semibold tracking-wide">LISTAGEM DE CHAMADOS</h2>
            </div>
            <div className="p-6">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <input
                    id="toggleFinished"
                    type="checkbox"
                    checked={showFinished}
                    onChange={(e) => { const checked = e.target.checked; setShowFinished(checked); setLoading(true); loadTicketsWithFilter(checked); }}
                  />
                  <label htmlFor="toggleFinished" className="text-sm text-gray-700">Mostrar finalizados</label>
                </div>
              </div>
              {loading ? (
                <div className="p-8 text-center text-gray-500">Carregando...</div>
              ) : tickets.length === 0 ? (
                <div className="p-8 text-center text-gray-500">Nenhum chamado encontrado</div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-gray-50 border-b">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Título</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Prioridade</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Solicitante</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Ações</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y">
                      {showFinished ? (
                        <>
                          {/* Abertos */}
                          {tickets.some(t => t.status === 'Aberto') && (
                            <tr>
                              <td colSpan={5} className="px-6 py-3 bg-blue-50 text-blue-800 font-semibold">Abertos</td>
                            </tr>
                          )}
                          {tickets.filter(t => t.status === 'Aberto').sort(comparePriority).map(renderRow)}

                          {/* Em Andamento */}
                          {tickets.some(t => t.status === 'Em Andamento') && (
                            <tr>
                              <td colSpan={5} className="px-6 py-3 bg-yellow-50 text-yellow-800 font-semibold">Em andamento</td>
                            </tr>
                          )}
                          {tickets.filter(t => t.status === 'Em Andamento').sort(comparePriority).map(renderRow)}

                          {/* Finalizados */}
                          {tickets.some(t => isFinished(t.status)) && (
                            <tr>
                              <td colSpan={5} className="px-6 py-3 bg-green-50 text-green-800 font-semibold">Finalizados</td>
                            </tr>
                          )}
                          {tickets.filter(t => isFinished(t.status)).sort(comparePriority).map(renderRow)}
                        </>
                      ) : (
                        tickets.map(renderRow)
                      )}
                    </tbody>
                  </table>
                </div>
              )}
              <div className="flex justify-center py-6">
                <button onClick={() => { setLoading(true); loadTickets(); }} className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700">ATUALIZAR LISTA</button>
              </div>
            </div>
          </div>
        )}

        {activeSection === 'conta' && (
          <div className="bg-white border rounded-md shadow-sm max-w-xl mx-auto">
            <div className="px-6 py-4 border-b">
              <h2 className="font-semibold tracking-wide">MINHA CONTA</h2>
              <div className="text-sm mt-2"><span className="font-semibold">Tipo:</span> {user?.role === 'tecnico' ? 'Técnico' : 'Usuário'}</div>
            </div>
            <div className="px-6 py-6 space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Nome:</label>
                <input type="text" value={accountName} onChange={(e) => setAccountName(e.target.value)} className="w-full border rounded-md p-2 bg-gray-50" />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Email:</label>
                <input type="email" value={accountEmail} onChange={(e) => setAccountEmail(e.target.value)} className="w-full border rounded-md p-2 bg-gray-50" readOnly />
              </div>
              {user?.role !== 'tecnico' && (
                <div>
                  <label className="block text-sm font-medium mb-1">Setor:</label>
                  <input
                    type="text"
                    value={accountSector}
                    onChange={(e) => setAccountSector(e.target.value)}
                    className="w-full border rounded-md p-2 bg-gray-50"
                    placeholder="Ex.: Enfermagem, TI, Administrativo"
                  />
                </div>
              )}
              <div className="flex gap-4 pt-2">
                <button
                  onClick={async () => {
                    try {
                      setSavingProfile(true);
                      const payload: any = { name: accountName };
                      if (user?.role !== 'tecnico') payload.sector = accountSector;
                      await updateMe(payload);
                      await refreshMe();
                      alert('Perfil atualizado com sucesso');
                    } catch (e) {
                      alert('Erro ao salvar perfil');
                      console.error(e);
                    } finally {
                      setSavingProfile(false);
                    }
                  }}
                  disabled={savingProfile}
                  className="px-5 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-60"
                >
                  {savingProfile ? 'Salvando...' : 'SALVAR'}
                </button>
                <button onClick={signOut} className="px-5 py-2 bg-red-600 text-white rounded-md hover:bg-red-700">SAIR</button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
