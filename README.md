# Simulador de Controle de Elevador Multithread

**Processamento Multithread em CPU — Sincronização com Buffer de Tamanho N**

Projeto desenvolvido em Java com interface gráfica utilizando [libGDX](https://libgdx.com/), totalmente configurado para execução no **Eclipse IDE**.

---

## 📋 Requisitos Atendidos

| Requisito do Enunciado | Implementação |
| :--- | :--- |
| **Prédio com F > 5 andares** | Implementado com **8 andares (0 a 7)** |
| **Controle por Threads e Semáforos** | Uso estrito de `java.util.concurrent.Semaphore` |
| **Restrição Estrita: Sem `synchronized`** | **ZERO métodos ou blocos `synchronized` no código** |
| **Duas Classes Principais: `Elevador` e `Prédio`** | Classes `Elevador` e `Predio` implementadas conforme especificado |
| **Métodos Internos** | `AbrirPorta()`, `FecharPorta()`, `VisitarAndar(int)` |
| **Interface Externa para Passageiros** | `chamarElevador(Passageiro)`, `entrarElevador(Passageiro)`, `sairElevador(Passageiro)` |
| **Instanciação Inicial** | Instância única de `Predio` cria e inicializa o `Elevador` |
| **Thread do Elevador** | Loop independente executando em thread dedicada |
| **Pedestres em Threads** | Cada pedestre/passageiro é uma thread individual com destino |
| **Prevenção de Starvation** | Algoritmo LOOK/SCAN justo com semáforos justos (`fair = true`) |
| **Prevenção de Condições de Corrida** | Portas nunca abrem em movimento; passageiros só embarcam/desembarcam com portas 100% abertas |
| **Exibição Visual Completa** | Portas abrindo/fechando, cabine iluminada, passageiros a bordo, filas nos andares, crachás de destino |
| **★ Bonificação Extra (Capacidade > 1)** | **Buffer de tamanho N implementado (Capacidade = 4)** com semáforo de vagas |

---

## 🏗️ Arquitetura de Sincronização por Semáforos

1. **Capacidade do Buffer (`semVagas = new Semaphore(capacidade, true)`)**:
   - Controla o número de passageiros simultâneos na cabine (Buffer de tamanho N).
   - Quando um passageiro embarca, consome 1 permissão do semáforo.
   - Quando o passageiro desembarca no seu andar de destino, devolve 1 permissão.

2. **Exclusão Mútua (`mutexPredio` e `mutexElevador`)**:
   - Semáforos binários (`new Semaphore(1, true)`) substituindo integralmente a palavra-chave `synchronized`.
   - Garantem atomicidade no acesso às filas dos andares e variáveis de estado.

3. **Sinalização Passageiro ⇄ Elevador**:
   - Cada passageiro possui seus próprios semáforos: `semPodeEmbarcar`, `semEmbarqueConcluido`, `semPodeDesembarcar`, `semDesembarqueConcluido`.
   - O elevador libera permissões apenas quando a porta está completamente aberta e o elevador está parado.
   - O elevador aguarda a conclusão da movimentação física do passageiro antes de iniciar o fechamento da porta.

4. **Notificação de Chamadas (`semNovasChamadas`)**:
   - Quando não há passageiros no prédio nem a bordo, o elevador dorme no semáforo sem desperdiçar ciclos de CPU (sem busy waiting).

---

## 🚀 Como Executar no Eclipse

### Opção 1: Diretamente pelo Eclipse (Interface Gráfica)
1. No Eclipse, clique com o botão direito no arquivo **`Lwjgl3Launcher.launch`** (na raiz do projeto).
2. Selecione **Run As → Lwjgl3Launcher**.
3. A janela gráfica da simulação (1280x720) será aberta exibindo o prédio, elevador e passageiros!

> **Alternativa**: Abra `io.github.elevador.lwjgl3.Lwjgl3Launcher.java`, clique com o botão direito e escolha **Run As → Java Application**.

### Opção 2: Teste em Console (Sem interface gráfica)
1. Clique com o botão direito no arquivo **`ProgramaTesteConsole.launch`**.
2. Selecione **Run As → ProgramaTesteConsole**.
3. Os logs em tempo real das threads e semáforos serão impressos diretamente no console do Eclipse.

### Opção 3: Via Linha de Comando (Gradle)
```bash
./gradlew lwjgl3:run
```

---

## 🎮 Controles Interativos na Simulação

- **`[ESPAÇO]`**: Gera um novo passageiro com andar de origem e destino aleatórios.
- **`[0]` a `[7]`**: Gera um passageiro especificamente no andar pressionado.
- **`[Clique na plataforma]`**: Clique com o mouse em qualquer andar do prédio para adicionar um passageiro naquele piso.
- **`[A]`**: Liga ou pausa o gerador automático de passageiros (*Auto-Spawn*).
- **`[P]`**: Pausa ou continua o tempo da simulação.
- **`[+]` / `[-]`**: Aumenta ou diminui a velocidade da simulação física.

---

## 📦 Formato de Entrega

1. **Arquivo compactado (.zip)**:
   - Contém o projeto completo com todos os códigos fontes (`core`, `lwjgl3`) e arquivos de recursos (`assets`).
2. **Vídeo demonstrativo (até 1 minuto)**:
   - Apresente o elevador atendendo chamados, as portas abrindo e fechando, os passageiros entrando/saindo, e a capacidade simultânea > 1 funcionando sem starvation.
