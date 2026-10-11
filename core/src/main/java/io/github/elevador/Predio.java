package io.github.elevador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 * Representa o Prédio que gerencia os andares e o elevador.
 * 
 * Requisitos atendidos:
 * - Instância única da classe Prédio cria e inicializa o Elevador.
 * - Gerencia F > 5 andares (F = 8 andares: 0 a 7).
 * - Oferece a interface externa para os passageiros:
 *     - chamarElevador(Passageiro p)
 *     - entrarElevador(Passageiro p)
 *     - sairElevador(Passageiro p)
 * - Oferece os métodos internos de coordenação para o Elevador.
 * - Exclusão mútua e controle 100% baseados na classe Semaphore.
 * - ZERO métodos ou blocos synchronized.
 * - Evita starvation através de algoritmo justo LOOK/SCAN com semáforos justos.
 */
public class Predio {

    private final int totalAndares;
    private final Elevador elevador;

    // Filas de passageiros aguardando em cada andar (FIFO)
    @SuppressWarnings("unchecked")
    private final Queue<Passageiro>[] filasPorAndar;

    // Histórico de passageiros que concluíram sua viagem
    private final List<Passageiro> passageirosConcluidos = new ArrayList<>();

    // Mutex baseado em Semáforo binário para sincronização de acesso às filas e estado do prédio
    // Substitui rigorosamente synchronized!
    private final Semaphore mutexPredio = new Semaphore(1, true);

    // Semáforo para notificar o elevador quando surge uma nova chamada
    private final Semaphore semNovasChamadas = new Semaphore(0, true);

    public Predio(int totalAndares, int capacidadeElevador, int andarInicial) {
        if (totalAndares <= 5) {
            throw new IllegalArgumentException("O prédio deve possuir F > 5 andares. Informado: " + totalAndares);
        }
        this.totalAndares = totalAndares;

        // Inicializa as filas para cada andar
        this.filasPorAndar = new LinkedList[totalAndares];
        for (int i = 0; i < totalAndares; i++) {
            this.filasPorAndar[i] = new LinkedList<>();
        }

        // Conforme requisito do enunciado:
        // "O programa deve primeiramente criar uma instância única da classe Prédio,
        // a qual cria um objeto do tipo Elevador e o inicializa"
        this.elevador = new Elevador(this, capacidadeElevador, andarInicial);
    }

    public void iniciarSimulacao() {
        elevador.iniciar();
    }

    public void encerrarSimulacao() {
        elevador.parar();
    }

    // =========================================================================
    // INTERFACE EXTERNA PARA OS PASSAGEIROS DO ELEVADOR
    // =========================================================================

    /**
     * Interface externa: o passageiro chega ao andar e chama o elevador.
     */
    public void chamarElevador(Passageiro passageiro) throws InterruptedException {
        int andar = passageiro.getAndarOrigem();
        if (andar < 0 || andar >= totalAndares) {
            throw new IllegalArgumentException("Andar inválido: " + andar);
        }

        mutexPredio.acquire();
        try {
            passageiro.setEstado(Passageiro.Estado.AGUARDANDO_FILA);
            filasPorAndar[andar].add(passageiro);
            atualizarPosicoesFilaNoAndar(andar);

            // Notifica o elevador que há uma nova chamada pendente
            semNovasChamadas.release();
        } finally {
            mutexPredio.release();
        }
    }

    /**
     * Interface externa: o passageiro entra no elevador (embarca).
     */
    public void entrarElevador(Passageiro passageiro) throws InterruptedException {
        passageiro.setEstado(Passageiro.Estado.EMBARCANDO);

        // Caminha até a cabine do elevador
        float startX = passageiro.getPosX();
        float targetX = elevador.getPosicaoX() + 30f;
        int passos = 15;
        for (int i = 1; i <= passos; i++) {
            passageiro.setPosX(startX + (targetX - startX) * ((float) i / passos));
            Thread.sleep(15);
        }

        passageiro.setEstado(Passageiro.Estado.NO_ELEVADOR);
        elevador.adicionarPassageiro(passageiro);

        mutexPredio.acquire();
        try {
            atualizarPosicoesFilaNoAndar(passageiro.getAndarOrigem());
        } finally {
            mutexPredio.release();
        }
    }

    /**
     * Interface externa: o passageiro sai do elevador no andar de destino (desembarca).
     */
    public void sairElevador(Passageiro passageiro) throws InterruptedException {
        passageiro.setEstado(Passageiro.Estado.DESEMBARCANDO);

        // Remove do elevador e libera a vaga no buffer
        elevador.removerPassageiro(passageiro);
        elevador.liberarVaga();

        // Caminha para fora da cabine no andar de destino
        float startX = elevador.getPosicaoX() + 30f;
        float targetX = 600f; // caminha para a direita no andar
        int passos = 20;
        for (int i = 1; i <= passos; i++) {
            passageiro.setPosX(startX + (targetX - startX) * ((float) i / passos));
            Thread.sleep(15);
        }

        passageiro.setEstado(Passageiro.Estado.CONCLUIDO);

        mutexPredio.acquire();
        try {
            passageirosConcluidos.add(passageiro);
        } finally {
            mutexPredio.release();
        }
    }

    // =========================================================================
    // MÉTODOS DE COORDENAÇÃO INTERNA PARA O ELEVADOR
    // =========================================================================

    /**
     * Bloqueia a thread do elevador até que haja um chamado pendente no prédio.
     */
    public void aguardarNovasChamadas() throws InterruptedException {
        semNovasChamadas.acquire();
    }

    /**
     * Algoritmo SCAN/LOOK para decidir o próximo andar a visitar de forma ordenada,
     * garantindo 100% de prevenção de starvation!
     */
    public int determinarProximoAndar(int andarAtual, Elevador.Direcao direcaoAtual) throws InterruptedException {
        mutexPredio.acquire();
        try {
            List<Passageiro> aBordo = elevador.getCopiaPassageirosABordo();

            boolean[] chamados = new boolean[totalAndares];
            int totalRequisicoes = 0;

            // Destinos dos passageiros a bordo
            for (Passageiro p : aBordo) {
                chamados[p.getAndarDestino()] = true;
                totalRequisicoes++;
            }

            // Andares com pessoas aguardando na fila
            for (int i = 0; i < totalAndares; i++) {
                if (!filasPorAndar[i].isEmpty()) {
                    chamados[i] = true;
                    totalRequisicoes++;
                }
            }

            if (totalRequisicoes == 0) {
                return -1; // Sem requisições
            }

            // Se há requisição no próprio andar atual
            if (chamados[andarAtual]) {
                return andarAtual;
            }

            // Algoritmo LOOK/SCAN:
            if (direcaoAtual == Elevador.Direcao.SUBINDO || direcaoAtual == Elevador.Direcao.PARADO) {
                // Procura o menor andar acima com chamado
                for (int i = andarAtual + 1; i < totalAndares; i++) {
                    if (chamados[i]) return i;
                }
                // Se não há nenhum acima, inverte e procura o maior andar abaixo
                for (int i = andarAtual - 1; i >= 0; i--) {
                    if (chamados[i]) return i;
                }
            } else { // DESCENDO
                // Procura o maior andar abaixo com chamado
                for (int i = andarAtual - 1; i >= 0; i--) {
                    if (chamados[i]) return i;
                }
                // Se não há nenhum abaixo, inverte e procura o menor andar acima
                for (int i = andarAtual + 1; i < totalAndares; i++) {
                    if (chamados[i]) return i;
                }
            }

            return -1;
        } finally {
            mutexPredio.release();
        }
    }

    /**
     * Verifica se o elevador deve parar no andar para desembarcar ou embarcar passageiros.
     */
    public boolean temOperacaoNoAndar(int andar, List<Passageiro> passageirosABordo) throws InterruptedException {
        mutexPredio.acquire();
        try {
            // Verifica se algum passageiro a bordo deseja descer aqui
            for (Passageiro p : passageirosABordo) {
                if (p.getAndarDestino() == andar) {
                    return true;
                }
            }
            // Verifica se há passageiros aguardando para subir neste andar
            return !filasPorAndar[andar].isEmpty();
        } finally {
            mutexPredio.release();
        }
    }

    /**
     * Coordena o processo de desembarque e embarque no andar de forma sincronizada por Semáforos.
     * Primeiro desembarcam todos os que têm este andar como destino.
     * Depois embarcam os que estão na fila, respeitando a capacidade máxima do elevador.
     */
    public void atenderAndar(int andar, Elevador elevador) throws InterruptedException {
        // --- 1. PROCESSO DE DESEMBARQUE ---
        List<Passageiro> aBordo = elevador.getCopiaPassageirosABordo();
        List<Passageiro> saindo = new ArrayList<>();
        for (Passageiro p : aBordo) {
            if (p.getAndarDestino() == andar) {
                saindo.add(p);
            }
        }

        for (Passageiro p : saindo) {
            // Autoriza a thread do passageiro a desembarcar
            p.getSemPodeDesembarcar().release();

            // Aguarda a confirmação de que o passageiro concluiu a saída
            p.getSemDesembarqueConcluido().acquire();
        }

        // --- 2. PROCESSO DE EMBARQUE ---
        while (true) {
            Passageiro proximo = null;

            mutexPredio.acquire();
            try {
                if (!filasPorAndar[andar].isEmpty()) {
                    // Tenta obter uma vaga no elevador através do semáforo de vagas
                    if (elevador.tentarReservarVaga()) {
                        proximo = filasPorAndar[andar].poll();
                    }
                }
            } finally {
                mutexPredio.release();
            }

            if (proximo == null) {
                // Ou a fila acabou, ou o elevador atingiu a capacidade máxima
                break;
            }

            // Autoriza a thread do passageiro a embarcar
            proximo.getSemPodeEmbarcar().release();

            // Aguarda a confirmação de que o passageiro concluiu a entrada
            proximo.getSemEmbarqueConcluido().acquire();
        }

        // Pequena pausa natural de segurança com as portas abertas antes de fechar
        Thread.sleep(300);
    }

    /**
     * Reorganiza graficamente as posições dos passageiros na fila do andar.
     */
    private void atualizarPosicoesFilaNoAndar(int andar) {
        int idx = 0;
        for (Passageiro p : filasPorAndar[andar]) {
            float targetX = 370f + idx * 52f;
            float targetY = 25f + andar * 90f + 14f;
            p.setTargetX(targetX);
            p.setTargetY(targetY);
            if (p.getPosX() > 1100f) {
                p.setPosX(targetX + 60f); // entra suavemente vindo da direita
            }
            idx++;
        }
    }

    // =========================================================================
    // GETTERS E MÉTODOS DE CONSULTA
    // =========================================================================

    public int getTotalAndares() {
        return totalAndares;
    }

    public Elevador getElevador() {
        return elevador;
    }

    public List<Passageiro> getPassageirosNaFila(int andar) {
        try {
            mutexPredio.acquire();
            try {
                return new ArrayList<>(filasPorAndar[andar]);
            } finally {
                mutexPredio.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    public int getQuantidadeFila(int andar) {
        try {
            mutexPredio.acquire();
            try {
                return filasPorAndar[andar].size();
            } finally {
                mutexPredio.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    public int getTotalPassageirosEmEspera() {
        try {
            mutexPredio.acquire();
            try {
                int total = 0;
                for (int i = 0; i < totalAndares; i++) {
                    total += filasPorAndar[i].size();
                }
                return total;
            } finally {
                mutexPredio.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    public List<Passageiro> getCopiaPassageirosConcluidos() {
        try {
            mutexPredio.acquire();
            try {
                return new ArrayList<>(passageirosConcluidos);
            } finally {
                mutexPredio.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }
}
