package io.github.elevador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Representa o elevador físico e seu controlador multithread.
 * 
 * Requisitos atendidos:
 * - Roda em uma thread separada com loop próprio.
 * - Invoca os métodos AbrirPorta(), FecharPorta(), VisitarAndar().
 * - Sincronização estrita via Semaphore (capacidade do buffer, mutexes, sinalização).
 * - ZERO métodos synchronized.
 * - Suporta capacidade > 1 (Bonificação Extra).
 * - Previne starvation usando algoritmo LOOK/SCAN.
 * - Previne condições de corrida (portas só abrem parado, trânsito apenas com portas fechadas).
 */
public class Elevador implements Runnable {

    public enum EstadoPorta {
        FECHADA,
        ABRINDO,
        ABERTA,
        FECHANDO
    }

    public enum Direcao {
        SUBINDO,
        DESCENDO,
        PARADO
    }

    private final Predio predio;
    private final int capacidade;
    private final Thread thread;

    // Semáforo para controlar a capacidade do elevador (Buffer de tamanho N)
    // Permite que até 'capacidade' passageiros estejam a bordo simultaneamente.
    private final Semaphore semVagas;

    // Mutex baseado em Semáforo binário para proteger os dados internos do elevador
    // Substitui rigorosamente a necessidade de synchronized!
    private final Semaphore mutexElevador = new Semaphore(1, true);

    // Estado do elevador
    private volatile boolean executando = true;
    private volatile int andarAtual;
    private volatile Direcao direcao = Direcao.PARADO;
    private volatile EstadoPorta estadoPorta = EstadoPorta.FECHADA;
    private volatile float aberturaPorta = 0.0f; // 0.0f (fechada) a 1.0f (totalmente aberta)
    private volatile boolean emTransito = false;

    // Coordenadas visuais contínuas para renderização gráfica
    private volatile float posicaoY;
    private final float posicaoX = 145f;
    private final float largura = 120f;
    private final float altura = 86f;

    // Passageiros atualmente dentro do elevador
    private final List<Passageiro> passageirosABordo = new ArrayList<>();

    // Estatísticas
    private volatile int totalPassageirosTransportados = 0;

    // Configuração de velocidade (pixels por segundo) e tempo de porta (segundos)
    private volatile float velocidadePixelsPorSegundo = 220f;
    private volatile float tempoTransicaoPortaSegundos = 0.45f;

    public Elevador(Predio predio, int capacidade, int andarInicial) {
        this.predio = predio;
        this.capacidade = capacidade;
        this.andarAtual = andarInicial;
        this.posicaoY = calcularYDoAndar(andarInicial);
        this.semVagas = new Semaphore(capacidade, true);
        this.thread = new Thread(this, "Thread-Elevador");
    }

    public void iniciar() {
        thread.start();
    }

    public void parar() {
        executando = false;
        thread.interrupt();
    }

    /**
     * Loop principal da thread do elevador.
     * Serve aos passageiros de forma ordenada, evitando starvation.
     */
    @Override
    public void run() {
        while (executando && !Thread.currentThread().isInterrupted()) {
            try {
                // 1. Determina se há chamados no prédio ou destinos dos passageiros a bordo
                int proximoAndar = predio.determinarProximoAndar(andarAtual, direcao);

                if (proximoAndar == -1) {
                    // Sem requisições pendentes: elevador fica ocioso aguardando chamados
                    mutexElevador.acquire();
                    try {
                        direcao = Direcao.PARADO;
                    } finally {
                        mutexElevador.release();
                    }

                    // Bloqueia no semáforo até que um novo chamado seja feito no prédio
                    predio.aguardarNovasChamadas();
                    continue;
                }

                // 2. Atualiza a direção de movimento
                mutexElevador.acquire();
                try {
                    if (proximoAndar > andarAtual) {
                        direcao = Direcao.SUBINDO;
                    } else if (proximoAndar < andarAtual) {
                        direcao = Direcao.DESCENDO;
                    }
                } finally {
                    mutexElevador.release();
                }

                // 3. Se o próximo andar for diferente do atual, viaja até ele
                if (proximoAndar != andarAtual) {
                    VisitarAndar(proximoAndar);
                }

                // 4. Ao chegar ao andar, verifica se há operações a realizar (desembarque ou embarque)
                if (predio.temOperacaoNoAndar(andarAtual, passageirosABordo)) {
                    // Abre as portas
                    AbrirPorta();

                    // Realiza os desembarques e embarques sincronizados por semáforos
                    predio.atenderAndar(andarAtual, this);

                    // Fecha as portas
                    FecharPorta();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // =========================================================================
    // MÉTODOS REQUISITADOS PELO ENUNCIADO (Internos do Elevador)
    // =========================================================================

    /**
     * Visita um andar especificado, simulando o deslocamento físico.
     * O elevador só se move com as portas rigorosamente fechadas.
     */
    public void VisitarAndar(int andarDestino) throws InterruptedException {
        visitarAndar(andarDestino);
    }

    public void visitarAndar(int andarDestino) throws InterruptedException {
        // Garantia: portas devem estar fechadas antes de iniciar o movimento
        if (estadoPorta != EstadoPorta.FECHADA) {
            fecharPorta();
        }

        mutexElevador.acquire();
        try {
            emTransito = true;
            direcao = andarDestino > andarAtual ? Direcao.SUBINDO : Direcao.DESCENDO;
        } finally {
            mutexElevador.release();
        }

        float targetY = calcularYDoAndar(andarDestino);
        float stepTime = 0.016f; // ~60fps step (16ms)

        while (Math.abs(posicaoY - targetY) > 1.0f) {
            float delta = velocidadePixelsPorSegundo * stepTime;
            if (posicaoY < targetY) {
                posicaoY = Math.min(posicaoY + delta, targetY);
            } else {
                posicaoY = Math.max(posicaoY - delta, targetY);
            }

            // Atualiza a posição dos passageiros a bordo para acompanharem o elevador
            atualizarPosicoesPassageirosABordo();

            Thread.sleep((long) (stepTime * 1000));
        }

        posicaoY = targetY;
        mutexElevador.acquire();
        try {
            andarAtual = andarDestino;
            emTransito = false;
        } finally {
            mutexElevador.release();
        }

        atualizarPosicoesPassageirosABordo();
    }

    /**
     * Abre a porta do elevador.
     * Só pode ser executado quando o elevador estiver parado no andar.
     */
    public void AbrirPorta() throws InterruptedException {
        abrirPorta();
    }

    public void abrirPorta() throws InterruptedException {
        mutexElevador.acquire();
        try {
            if (emTransito) {
                throw new IllegalStateException("Tentativa de abrir a porta com o elevador em trânsito!");
            }
            estadoPorta = EstadoPorta.ABRINDO;
        } finally {
            mutexElevador.release();
        }

        // Animação gradual de abertura da porta
        int passos = 25;
        long tempoPorPasso = (long) ((tempoTransicaoPortaSegundos * 1000) / passos);
        for (int i = 1; i <= passos; i++) {
            aberturaPorta = (float) i / passos;
            Thread.sleep(tempoPorPasso);
        }

        mutexElevador.acquire();
        try {
            aberturaPorta = 1.0f;
            estadoPorta = EstadoPorta.ABERTA;
        } finally {
            mutexElevador.release();
        }
    }

    /**
     * Fecha a porta do elevador.
     */
    public void FecharPorta() throws InterruptedException {
        fecharPorta();
    }

    public void fecharPorta() throws InterruptedException {
        mutexElevador.acquire();
        try {
            estadoPorta = EstadoPorta.FECHANDO;
        } finally {
            mutexElevador.release();
        }

        // Animação gradual de fechamento da porta
        int passos = 25;
        long tempoPorPasso = (long) ((tempoTransicaoPortaSegundos * 1000) / passos);
        for (int i = passos - 1; i >= 0; i--) {
            aberturaPorta = (float) i / passos;
            Thread.sleep(tempoPorPasso);
        }

        mutexElevador.acquire();
        try {
            aberturaPorta = 0.0f;
            estadoPorta = EstadoPorta.FECHADA;
        } finally {
            mutexElevador.release();
        }
    }

    // =========================================================================
    // CONTROLE DE PASSAGEIROS E VAGAS (Buffer de Capacidade N)
    // =========================================================================

    /**
     * Tenta reservar uma vaga no elevador de forma não-bloqueante.
     */
    public boolean tentarReservarVaga() {
        return semVagas.tryAcquire();
    }

    /**
     * Libera uma vaga no elevador (quando um passageiro desembarca).
     */
    public void liberarVaga() {
        semVagas.release();
    }

    /**
     * Adiciona um passageiro à lista de passageiros a bordo.
     * Protegido por mutexElevador (Semaphore).
     */
    public void adicionarPassageiro(Passageiro p) throws InterruptedException {
        mutexElevador.acquire();
        try {
            passageirosABordo.add(p);
            atualizarPosicoesPassageirosABordoInterno();
        } finally {
            mutexElevador.release();
        }
    }

    /**
     * Remove um passageiro da lista de passageiros a bordo.
     * Protegido por mutexElevador (Semaphore).
     */
    public void removerPassageiro(Passageiro p) throws InterruptedException {
        mutexElevador.acquire();
        try {
            passageirosABordo.remove(p);
            totalPassageirosTransportados++;
            atualizarPosicoesPassageirosABordoInterno();
        } finally {
            mutexElevador.release();
        }
    }

    /**
     * Atualiza as posições gráficas dos passageiros dentro da cabine.
     */
    private void atualizarPosicoesPassageirosABordo() {
        try {
            mutexElevador.acquire();
            try {
                atualizarPosicoesPassageirosABordoInterno();
            } finally {
                mutexElevador.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void atualizarPosicoesPassageirosABordoInterno() {
        int total = passageirosABordo.size();
        if (total == 0) return;

        float espacamento = largura / (capacidade + 1);
        for (int i = 0; i < total; i++) {
            Passageiro p = passageirosABordo.get(i);
            float offsetX = espacamento * (i + 1) - 22f; // centraliza o avatar (largura ~44px)
            p.setPosX(posicaoX + offsetX);
            p.setPosY(posicaoY + 12f);
            p.setTargetX(p.getPosX());
            p.setTargetY(p.getPosY());
        }
    }

    private float calcularYDoAndar(int andar) {
        return 25f + andar * 90f;
    }

    // =========================================================================
    // GETTERS E MÉTODOS DE CONSULTA
    // =========================================================================

    public int getCapacidade() {
        return capacidade;
    }

    public int getAndarAtual() {
        return andarAtual;
    }

    public Direcao getDirecao() {
        return direcao;
    }

    public EstadoPorta getEstadoPorta() {
        return estadoPorta;
    }

    public float getAberturaPorta() {
        return aberturaPorta;
    }

    public boolean isEmTransito() {
        return emTransito;
    }

    public float getPosicaoX() {
        return posicaoX;
    }

    public float getPosicaoY() {
        return posicaoY;
    }

    public float getLargura() {
        return largura;
    }

    public float getAltura() {
        return altura;
    }

    public int getQuantidadePassageiros() {
        try {
            mutexElevador.acquire();
            try {
                return passageirosABordo.size();
            } finally {
                mutexElevador.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    public List<Passageiro> getCopiaPassageirosABordo() {
        try {
            mutexElevador.acquire();
            try {
                return new ArrayList<>(passageirosABordo);
            } finally {
                mutexElevador.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    public int getTotalPassageirosTransportados() {
        return totalPassageirosTransportados;
    }

    public Semaphore getSemVagas() {
        return semVagas;
    }

    public void setVelocidadeMultiplicador(float mult) {
        this.velocidadePixelsPorSegundo = 220f * mult;
        this.tempoTransicaoPortaSegundos = 0.45f / mult;
    }
}
