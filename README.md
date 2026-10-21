# HACCP Auto — Java 21

Aplicação web local para **um estabelecimento alimentar**, com registos HACCP rastreáveis e exportação `.xlsx` e `.pdf`.

## Requisitos
- Java JDK 21 (inclui `javac` e `java`).
- Browser moderno.
- Não exige Maven, npm nem dependências Java externas.

## Primeiro arranque (Linux/macOS)
```bash
export HACCP_ADMIN_PASSWORD='SUBSTITUIR_POR_PASSWORD_FORTE_DE_16_CHARS'
./run.sh
```

## Primeiro arranque (Windows / PowerShell)
```powershell
$env:HACCP_ADMIN_PASSWORD = 'SUBSTITUIR_POR_PASSWORD_FORTE_DE_16_CHARS'
.\run.bat
```

Abrir `http://127.0.0.1:8080`. O utilizador inicial é `admin` (personalizável via `HACCP_ADMIN_USER`). O administrador inicial só é criado se a base de dados estiver vazia. Substituir imediatamente a password de exemplo antes de executar.

**Variáveis opcionais:** `HACCP_PORT` (8080), `HACCP_BIND` (127.0.0.1), `HACCP_DATA` (data), `HACCP_WEB` (web), `HACCP_SECURE_COOKIE` (`true` ao usar HTTPS em produção).

## Funcionalidades implementadas
- Contas e papéis: administrador, responsável, funcionário; passwords PBKDF2; cookies HttpOnly SameSite Strict e CSRF para formulários.
- Equipamentos e limites personalizados (não há limites HACCP universais pré-preenchidos).
- Registo de temperaturas com hora gerada pelo servidor e identificação de valores fora do intervalo.
- Planos de higienização e tarefas pendentes segundo frequência em dias.
- Registos de higienização, fornecedores e receção de mercadorias com lote/validade.
- Não conformidades, encerramento com ação corretiva e responsável.
- Registos de manutenção, pragas e formação.
- Histórico de auditoria para operações de criação e encerramento.
- Exportação de folhas Excel `.xlsx` genuínas (Office Open XML) e relatórios PDF simples, por mês de criação.
- Interface responsiva em português de Portugal.
- Persistência em disco com escrita para ficheiro temporário e substituição atómica quando suportada.

## Fluxo recomendado
1. Como admin, criar os **equipamentos** e colocar os limites especificados pelo responsável HACCP.
2. Criar **planos de higienização** e **fornecedores**.
3. Criar contas com password individual de 12+ caracteres.
4. Registar medições e execução de tarefas. Se surgir alerta, abrir uma **não conformidade** e descrever a intervenção efetiva.
5. Exportar os registos por mês para Excel ou PDF.

## Estrutura
- `src/haccp/Main.java`: servidor Java HTTP, validação, persistência, autorização, exportação.
- `web/index.html`, `web/app.js`, `web/style.css`: painel web responsivo.
- `data/store.bin`: dados persistidos (criado durante execução).
- `run.sh` / `run.bat`: compilação e execução.

## Limitações e requisitos antes de produção real
Esta é uma **base funcional V1**, não uma implementação certificada nem a realização integral da plataforma avançada proposta. Importante:
- **Não importa nem preserva modelos Excel existentes**. Exporta folhas próprias. Para preservar fórmulas/formatação dos modelos de uma empresa será necessário integrar um motor de templates como Apache POI, com mapeamento explícito das células.
- **Não integra sensores físicos**, e-mail/SMS, nem modo offline.
- O motor de dados é um ficheiro de objetos Java, e não PostgreSQL. Para utilização multiutilizador crítica é aconselhada migração para PostgreSQL, com transações e migrações.
- A autenticação não tem limitação de tentativas persistente, recuperação de conta ou MFA. Não publicar diretamente na internet. Antes da exposição pública, adicionar rate limiting, MFA se aplicável, reverse proxy HTTPS, monitorização e hardening.
- A aplicação não cria backups periódicos automaticamente. Estabelecer cópias de segurança verificadas do diretório `data` **com o servidor parado** e testar restauros.
- PDF é um resumo simples, limita-se a 150 entradas e translitera acentos. Excel inclui todos os registos do mês.
- Não há anexos, gestão de stocks por lote, controlos de validade automatizados nem aprovação digital de relatórios.
- Não alterar uma medição efetuada sem manter os dados originais. Este projeto conserva entradas e permite registar correções de contexto por não conformidades.
- O sistema não pode inventar medições, limpezas, verificações nem assinaturas. Parâmetros e procedimentos devem ser validados pelo responsável HACCP.

## Segurança e privacidade
- O primeiro arranque exige explicitamente uma palavra-passe inicial definida por variável ambiente.
- A aplicação escuta apenas em `127.0.0.1` por omissão.
- Para servidores externos, usar HTTPS e definir `HACCP_SECURE_COOKIE=true`. Nunca tornar a porta Java acessível publicamente sem proxy e controlos adequados.
- Proteger os ficheiros `data/store.bin`; podem conter dados operacionais e nomes de trabalhadores. Aplicar política de retenção e RGPD adequada.

## Nota técnica
O projeto é Java puro e não necessita de Apache POI; o ficheiro `.xlsx` é construído com o formato Office Open XML e `java.util.zip`. É intencionalmente simples de compilar.


## Acesso pela rede local e servidor só com terminal (Linux)
Esta edição aceita ligações em **0.0.0.0:8080** por omissão: o painel é aberto no navegador de qualquer equipamento autorizado da mesma rede, não no próprio servidor.

### Arranque manual
```bash
sudo apt update && sudo apt install -y openjdk-21-jdk unzip
unzip HACCP_Auto_Java21_LAN.zip
cd haccp-auto
export HACCP_ADMIN_PASSWORD='PASSWORD_FORTE_COM_12_OU_MAIS_CARACTERES'
chmod +x run.sh
./run.sh
```
No PC ou telemóvel: `http://IP_PRIVADO_DO_SERVIDOR:8080`. Obtém o IP com `hostname -I`. O terminal deve permanecer aberto no arranque manual.

### Serviço de sistema (arranque automático após reboot)
Dentro da pasta `haccp-auto`, iniciado de uma conta Linux normal com privilégios sudo:
```bash
sudo ./install-systemd.sh
sudo systemctl status haccp-auto
sudo journalctl -u haccp-auto -f
```
O instalador pergunta uma password inicial, guarda-a em `/etc/haccp-auto.env` com modo 600 e cria um serviço systemd para arrancar automaticamente. Após o primeiro arranque bem-sucedido, podes remover a linha `HACCP_ADMIN_PASSWORD=...` do ficheiro de ambiente (a conta já foi criada na base de dados). **Não apagues `data/`** se quiseres conservar registos.

### Firewall e segurança
Se usares UFW, **autoriza apenas a tua sub-rede** (exemplo de rede 192.168.1.0/24):
```bash
sudo ufw allow from 192.168.1.0/24 to any port 8080 proto tcp
```
Ajusta a sub-rede real, e assegura que o servidor e os clientes se conseguem alcançar. Não configures port-forwarding no router nem exponhas a porta 8080 à Internet. Esta versão usa HTTP, por isso passwords e dados podem ser observados numa rede não confiável: para ambientes empresariais, coloca-a atrás de reverse proxy HTTPS e ativa `HACCP_SECURE_COOKIE=true`. O modo LAN é destinado a uma rede local confiável e não equivale a estar pronto para produção.

Variáveis: `HACCP_BIND` (predefinido `0.0.0.0`), `HACCP_PORT` (8080), `HACCP_SECURE_COOKIE` (false em HTTP local). Não é necessário ambiente gráfico, Node.js ou navegador no servidor.


## Novo modelo: manutenção a quente

O módulo **Manutenção a quente** permite registar alimento, equipamento/banho-maria, temperatura medida, temperatura mínima aprovada, ação corretiva e iniciais. Registos abaixo do mínimo exigem descrição da ação corretiva. Data e hora são atribuídas pelo servidor.

No módulo, seleciona o mês e clica **Modelo original ↓**. O endpoint `/api/export-hot-holding?month=AAAA-MM&establishment=...&equipment=...` utiliza o ficheiro original `templates/hot-holding-log.xlsx` e preenche as células A8:G38 mantendo o restante ficheiro. O ficheiro tem capacidade para 31 leituras por exportação; se houver mais, responde com erro e não omite leituras silenciosamente. Este modelo destina-se a registos de conservação/manutenção a quente, não a frigoríficos.

O livro IFC 2023 está guardado em `templates/haccp-work-book-template-ifc-2023.xlsx` como material de referência, **não** está ainda automaticamente preenchido. A aplicação não inventa medições nem valida automaticamente requisitos regulamentares. O mínimo é definido pelo responsável HACCP.

Antes de atualizar: copia `data/store.bin` para backup e interrompe o serviço. A pasta `out` tem de permitir escrita pelo utilizador do serviço.
