# Hospital Call System – Execução no Windows

Este projeto contém:
- Frontend React + Vite + TypeScript
- Backend Java com SQLite (HTTP Server embutido)

O backend expõe endpoints REST para autenticação e gerenciamento de tickets; o frontend consome esses endpoints.

## Requisitos
- Node.js 18+ e npm
- Java 17+ (`java` e `javac` disponíveis no PATH)
- Windows PowerShell

## 1) Clonar o repositório
- Clone o projeto e entre na pasta:
```
git clone <URL_DO_REPOSITORIO>
cd project-bolt/project
```

## 2) Instalar dependências do frontend
- Instale os pacotes:
```
npm install
```

## 3) Configurar variáveis (opcional)
- Por padrão, o frontend usa `http://localhost:8080` como API (variável `VITE_API_URL`).
- Se quiser sobrescrever, crie um arquivo `.env` na raiz com:
```
VITE_API_URL=http://localhost:8080
```
- O backend aceita (opcional):
  - `JAVA_BACKEND_JWT_SECRET` (padrão: `LOCAL_DEV_SECRET`)
  - `CORS_ORIGIN` (padrão: `http://localhost:5173`, com suporte automático a `5174`)
- Para definir temporariamente na sessão do PowerShell:
```
$env:JAVA_BACKEND_JWT_SECRET="LOCAL_DEV_SECRET"
```

## 4) Iniciar o backend (PowerShell)
- Se o PowerShell bloquear scripts, libere para a sessão atual:
```
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```
- Inicie o backend:
```
./java-backend/run.ps1
```
- O servidor sobe em: `http://localhost:8080`
- O banco é criado automaticamente em: `java-backend\data\hospital.db` (tabelas `users` e `tickets`)

## 5) Iniciar o frontend (Vite)
- Suba o servidor de desenvolvimento:
```
npm run dev
```
- Acesse:
- `http://localhost:5173/` (ou `http://localhost:5174/` se 5173 estiver ocupada)

## Fluxo rápido de teste
- No frontend, faça cadastro (Sign Up) ou login (Sign In).
- Crie/gerencie tickets pela interface (listagem separada por status e ordenada por prioridade).

## Endpoints do backend (base `http://localhost:8080`)
- `POST /auth/signup` → `{ email, password, name, role, sector? }`
- `POST /auth/login` → `{ email, password, role? }` → retorna `{ token, user }`
- `GET /auth/me` (Authorization: `Bearer <token>`)  
- `PUT /auth/me` → `{ name?, sector? }` → retorna `{ token, user }`

- `GET /tickets` (Authorization)
- `POST /tickets` → `{ title, description, category, priority, location, requester_name, requester_sector?, responsible_name? }` (status padrão `Aberto`)
- `PUT /tickets/{id}` (Authorization)
- `PATCH /tickets/{id}/status` → `{ status }` (Authorization)
- `DELETE /tickets/{id}` (Authorization, apenas dono)

Observação: `responsible_name` no payload é mapeado internamente para `assigned_to`.

## Solução de problemas (Windows)
- Script bloqueado no PowerShell:
```
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```
- Porta ocupada:
- Feche instâncias antigas do `run.ps1` no PowerShell.
- Vite troca para `5174` automaticamente se `5173` estiver ocupada.
- API não acessível:
- Verifique se backend está ativo em `http://localhost:8080`.
- Confirme `VITE_API_URL` apontando para a mesma URL.
- Node/Java não encontrados:
- Instale as versões exigidas e verifique `node`, `npm`, `java`, `javac` no `PATH`.

## Build e Preview (Frontend)
- Gerar build de produção:
```
npm run build
```
- Preview local do build:
```
npm run preview
```

## Estrutura do projeto (resumo)
- `src/` – frontend (componentes `Auth`, `Dashboard`, contexto `AuthContext`, cliente `api.ts`)
- `java-backend/` – backend Java (`run.ps1`, código em `src/com/hospital/tickets/`, libs em `lib/`, dados em `data/`)
- `.env` – variáveis do frontend (opcional)
- `package.json` – scripts (`dev`, `build`, `preview`, `lint`, `typecheck`)

## Observação sobre dados
- Os arquivos de banco e JSON em `java-backend/data/` são ignorados pelo Git via `.gitignore`.
- Antes de subir para o GitHub, garanta que a pasta `java-backend/data/` não contenha dados sensíveis (o backend recria o banco vazio ao rodar).