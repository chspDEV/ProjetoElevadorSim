# Simulador de Elevador

Uma aplicação interativa de simulação de elevador predial multithread desenvolvida em **Java** e **libGDX**, demonstrando conceitos avançados de programação concorrente, sincronização estrita com semáforos (`java.util.concurrent.Semaphore`), gerenciamento de filas por andar, algoritmo de escalonamento livre de starvation e modelo produtor-consumidor com buffer de capacidade N.

---

## Funcionalidades Principais

- **Controle Multithread com Semáforos**:
  - Sincronização estrita com a classe `Semaphore` sem uso da palavra-chave `synchronized`.
  - Mutexes binários garantindo exclusão mútua e ausência de condições de corrida.
- **Buffer de Capacidade N (Bonificação Extra)**:
  - Cabine com capacidade para múltiplos passageiros simultâneos controlada via semáforo de vagas.
- **Escalonamento Justo (Livre de Starvation)**:
  - Algoritmo LOOK/SCAN que atende os andares na direção do movimento de forma ordenada.
  - Filas FIFO por andar com semáforos justos (`fair = true`).
- **Segurança Operacional**:
  - Portas do elevador abrem apenas quando o elevador está completamente parado no andar.
  - Movimentação entre andares ocorre estritamente com as portas fechadas.
  - Embarque e desembarque sincronizados por sinalização individual.
- **Painel Lateral em Tempo Real**:
  - Monitoramento de andar atual, estado da porta e direção.
  - Contagem de passageiros a bordo, transportados e filas de espera.
- **Controle Interativo**:
  - Geração manual de passageiros via teclado ou clique do mouse nos andares.
  - Ajuste de velocidade da simulação física em tempo real.

---

## 🏗️ Arquitetura Concorrente

| Componente | Papel Concorrente |
| :--- | :--- |
| **`Passageiro`** | Executado em sua própria `Thread`. Aguarda permissão no semáforo de embarque, realiza a entrada no elevador, aguarda a chegada ao andar de destino e desembarca liberando vaga. |
| **`Elevador`** | Executado em `Thread` dedicada. Controla o ciclo do elevador, movimentação física entre andares (`VisitarAndar`), transição das portas (`AbrirPorta`/`FecharPorta`) e buffer de vagas (`semVagas`). |
| **`Predio`** | Coordena o estado do prédio, filas FIFO por andar e sincronização entre passageiros e elevador via semáforos binários (mutex) e semáforos de sinalização. |
| **`GeradorPassageiros`** | Thread produtora que gera pedestres de forma autônoma e randômica com andares de origem e destino distintos. |

---

## Como Executar

### Pré-requisitos

- **Java JDK 8+** (ou 17+) instalado.
- **Eclipse IDE** ou terminal com **Gradle** (incluso via wrapper `./gradlew`).

### Execução no Eclipse

1. Clique com o botão direito no arquivo `Lwjgl3Launcher.launch` na raiz do projeto.
2. Selecione **Run As → Lwjgl3Launcher**.
*(Ou execute a classe `io.github.elevador.lwjgl3.Lwjgl3Launcher.java` como Java Application).*

### Comandos de Execução via Terminal

**Linux / macOS:**
```bash
./gradlew lwjgl3:run
```

**Windows:**
```cmd
gradlew.bat lwjgl3:run
```

---

## 🎮 Controles

| Tecla / Ação | Ação |
| :---: | :--- |
| <kbd>ESPAÇO</kbd> | Criar passageiro com origem e destino aleatórios |
| <kbd>0</kbd> a <kbd>7</kbd> | Criar passageiro no andar correspondente |
| <kbd>Clique no Andar</kbd> | Criar passageiro no piso clicado |
| <kbd>A</kbd> | Alternar geração automática de passageiros (Auto-Spawn) |
| <kbd>P</kbd> | Pausar / Continuar a simulação |
| <kbd>+</kbd> / <kbd>-</kbd> | Acelerar / Desacelerar a velocidade da simulação |

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Java 8+ / 17+
- **Framework Gráfico**: [libGDX](https://libgdx.com/) (LWJGL3 Backend)
- **Sincronização**: `java.util.concurrent.Semaphore` (Zero uso de `synchronized`)
- **Gerenciador de Dependências**: Gradle
