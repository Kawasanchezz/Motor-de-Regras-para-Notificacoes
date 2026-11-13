# Política de Segurança

Projeto acadêmico (motor de regras para notificações). **Não foi projetado para ser exposto diretamente à internet.**
Nenhuma auditoria elimina todos os riscos: este documento descreve o que foi tratado e o que continua sendo uma limitação.

## Versões suportadas

| Versão | Suporte |
|---|---|
| `main` (1.0.x) | Recebe correções de segurança |

## Como reportar uma vulnerabilidade

Use o **relato privado de vulnerabilidades do GitHub** (aba *Security* → *Report a vulnerability*) do repositório.
**Não abra uma issue pública** com detalhes de exploração.

Processo esperado (melhor esforço, projeto mantido por uma pessoa): confirmação de recebimento em até 7 dias,
avaliação e plano de correção em até 30 dias, e crédito ao relator se desejado.

## Limitações conhecidas (não resolvidas)

1. **Sem autenticação nem autorização.** Qualquer cliente que alcance a API pode criar/alterar/excluir regras,
   enviar eventos e consultar o histórico (CWE-306 / OWASP A01 e A07). Por isso o `docker-compose.yml` publica as
   portas apenas em `127.0.0.1`. Para uso real, coloque a API atrás de um gateway/proxy com autenticação
   (OAuth2/JWT ou API key) ou adicione Spring Security.
2. **Sem limite de taxa na própria API** (CWE-770). Há limite de tamanho do corpo (256 KB por `Content-Length`),
   de paginação (100) e dos campos, mas nada impede um cliente de enviar milhares de requisições. Use um proxy com rate limit.
   Corpos `chunked` sem `Content-Length` não são cobertos pelo filtro de tamanho; limite também no proxy.
3. **Idempotência sem escopo por cliente:** a chave é global (`Idempotency-Key` ou `eventId`). Sem autenticação,
   um cliente pode "consumir" o `eventId` de outro.
4. **Verificação e consumo de cotas não são atômicos** (cooldown, rate limit, deduplicação). Requisições simultâneas
   do mesmo usuário podem ultrapassar um limite por uma pequena margem.
5. **Dados pessoais no banco e no Redis** (payload do evento, e-mail, telefone) ficam em texto claro, sem política de
   retenção. Os logs mascaram o destinatário e não gravam o corpo da mensagem.
6. **Canais de notificação simulados** (apenas log): não há integração real com provedores.
7. **Dados de exemplo** (migração `V2__sample_data.sql`) são criados em qualquer ambiente, inclusive produção.
   Remova-a ou mova para um perfil de desenvolvimento antes de um deploy real.
8. **Perfil `local`** (`notify.store=memory`) guarda tudo em memória: use apenas com uma instância e só para desenvolvimento.
9. **Duas CVEs críticas do Spring Framework 6.2.19 sem correção na linha 6.x:** CVE-2026-47884 (exige `XsltView` com
   mapeamento `/**`) e CVE-2026-47890 (exige Server-Sent Events com fragmentos de view). **Não são exploráveis aqui**:
   o projeto é uma API REST/JSON, sem views, templates nem SSE. A correção só existe no Spring 7 (Spring Boot 4,
   mudança de versão maior); migrar é o próximo passo planejado.
10. **A análise de vulnerabilidades do OWASP Dependency-Check não roda no build padrão** (perfil `security`, exige a chave
    `NVD_API_KEY` e não foi executado). A varredura da imagem foi feita com Trivy (ver abaixo) e deve ser repetida a
    cada atualização de dependências; use também o Dependabot e o *code scanning* do GitHub.
11. O `postgres` e o `redis` do Compose não têm sistema de arquivos somente leitura nem `cap_drop` (as imagens oficiais
    precisam de privilégios no primeiro start); só o contêiner da aplicação é totalmente restrito.

## Controles implementados

- Consultas JPA parametrizadas (sem SQL concatenado); templates por substituição simples (sem `eval`).
- Validação de entrada (`@Valid`, `@Size`), limite de profundidade/nós da árvore de condições e limites nos parâmetros das restrições.
- Respostas de erro padronizadas, sem stack trace nem nomes de classes internas.
- Redis com senha obrigatória no Docker Compose; Postgres e Redis publicados apenas em `localhost`.
- **Imagem Docker:** JRE em Alpine (sem `apt`, `curl` nem compilador), build em múltiplos estágios, usuário não-root
  (UID 10001, sem shell), imagens-base fixadas por tag e **digest**, `HEALTHCHECK` próprio e nenhum segredo copiado
  para a imagem (`.dockerignore` testado).
- **Contêiner da aplicação (Compose):** sistema de arquivos somente leitura (só `/tmp` em memória), todas as
  *capabilities* removidas, `no-new-privileges` e limites de memória, CPU e processos. Verificado em execução.
- **Build Maven:** `--strict-checksums` (falha se um artefato não bater com o checksum), Maven Enforcer (exige Java 21+,
  Maven 3.9+, sem versões duplicadas nem dependências SNAPSHOT), versões fixadas pelo BOM do Spring Boot e build
  reprodutível (`project.build.outputTimestamp` do parent). Perfil `security` com SBOM CycloneDX e Dependency-Check.
- Segredos só por variáveis de ambiente (`.env` fora do Git); `.env.example` contém apenas marcadores.
- Actuator limitado a `health,info` por padrão (métricas/Prometheus só nos perfis `dev` e `local`).
- **Dependências:** Spring Boot 3.5.x com Tomcat, Jackson, Netty, PostgreSQL JDBC, Log4j e commons-lang3 fixados em
  versões corrigidas (propriedades no `pom.xml`). Varredura da imagem com Trivy: de 40 vulnerabilidades HIGH/CRITICAL
  para 2 (as CVEs do Spring descritas acima, não exploráveis neste projeto).
- **Supply chain e CI:** Dependabot (Maven, Docker, Compose e GitHub Actions) com *cooldown* de 7 dias; workflow de CI com
  `permissions: contents: read` e actions fixadas por SHA; hook opcional de detecção de segredos (`.pre-commit-config.yaml`).
- **Ferramentas executadas na auditoria:** Gitleaks (histórico completo, sem achados), Hadolint (sem avisos),
  Trivy (configuração e imagem) e Semgrep (regras Java, secrets, Dockerfile e OWASP).

## Para quem for publicar ou implantar

- Gere senhas fortes e únicas em `.env` (nunca reutilize as de desenvolvimento).
- Use `SPRING_PROFILES_ACTIVE=prod` e `SWAGGER_ENABLED=false`.
- Habilite no GitHub: *secret scanning*, *push protection*, *private vulnerability reporting* e proteção da branch `main`.
- Ative o hook local: `pip install pre-commit && pre-commit install`.
