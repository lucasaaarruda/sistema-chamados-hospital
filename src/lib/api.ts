export interface User {
  id: string;
  email: string;
  name: string;
  role: 'usuario' | 'tecnico';
  sector?: string;
}

export interface Ticket {
  id: string;
  title: string;
  description: string;
  category: string;
  priority: string;
  status: string;
  location: string;
  requester_name: string;
  requester_sector?: string;
  assigned_to: string | null;
  user_id: string;
  created_at: string;
  updated_at: string;
}

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

function jsonHeaders(): Record<string, string> {
  return { 'Content-Type': 'application/json' };
}

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('auth_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function handleResponse(res: Response) {
  if (!res.ok) {
    return res.text().then(t => {
      try { const j = JSON.parse(t); throw new Error(j.error || t || 'Erro na API'); }
      catch { throw new Error(t || 'Erro na API'); }
    });
  }
  const ct = res.headers.get('Content-Type') || '';
  if (ct.includes('application/json')) return res.json();
  return res.text();
}

export async function signUp(email: string, password: string, name: string, role: 'usuario' | 'tecnico', sector?: string): Promise<void> {
  const res = await fetch(`${API_URL}/auth/signup`, {
    method: 'POST',
    headers: { ...jsonHeaders() },
    body: JSON.stringify({ email, password, name, role, sector })
  });
  await handleResponse(res);
}

export async function signIn(email: string, password: string, role?: 'usuario' | 'tecnico'): Promise<void> {
  const res = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { ...jsonHeaders() },
    body: JSON.stringify(role ? { email, password, role } : { email, password })
  });
  const data = await handleResponse(res) as { token: string, user: User };
  localStorage.setItem('auth_token', data.token);
}

export async function signOut(): Promise<void> {
  localStorage.removeItem('auth_token');
}

export async function getMe(): Promise<User | null> {
  const res = await fetch(`${API_URL}/auth/me`, { headers: authHeaders() });
  if (res.status === 401) return null;
  return await handleResponse(res) as User;
}

export async function updateMe(payload: { name?: string; sector?: string }): Promise<User> {
  const res = await fetch(`${API_URL}/auth/me`, {
    method: 'PUT',
    headers: { ...jsonHeaders(), ...authHeaders() },
    body: JSON.stringify(payload)
  });
  const data = await handleResponse(res) as { token: string, user: User };
  if (data?.token) {
    localStorage.setItem('auth_token', data.token);
  }
  return data.user;
}

export async function listTickets(): Promise<Ticket[]> {
  const res = await fetch(`${API_URL}/tickets`, { headers: { ...authHeaders() } });
  return await handleResponse(res) as Ticket[];
}

export async function createTicket(payload: {
  title: string;
  description: string;
  category: string;
  priority: string;
  location: string;
  requester_name: string;
  requester_sector?: string;
  responsible_name?: string;
}): Promise<Ticket> {
  const res = await fetch(`${API_URL}/tickets`, {
    method: 'POST',
    headers: { ...jsonHeaders(), ...authHeaders() },
    body: JSON.stringify({ ...payload, status: 'Aberto' })
  });
  return await handleResponse(res) as Ticket;
}


export async function updateTicketStatus(id: string, status: string): Promise<Ticket> {
  const res = await fetch(`${API_URL}/ticket/${id}/status`, {
    method: 'PATCH',
    headers: { ...jsonHeaders(), ...authHeaders() },
    body: JSON.stringify({ status })
  });
  return await handleResponse(res) as Ticket;
}