package io.github.elevador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

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

    private final Semaphore semVagas;
    private final Semaphore mutexElevador = new Semaphore(1, true);

    private volatile boolean executando = true;
    private volatile int andarAtual;
    private volatile Direcao direcao = Direcao.PARADO;
    private volatile EstadoPorta estadoPorta = EstadoPorta.FECHADA;
    private volatile float aberturaPorta = 0.0f;
    private volatile boolean emTransito = false;

    private volatile float posicaoY;
    private final float posicaoX = Config.ELEVADOR_X;
    private final float largura = Config.ELEVADOR_LARGURA;
    private final float altura = Config.ELEVADOR_ALTURA;

    private final List<Passageiro> passageirosABordo = new ArrayList<>();

    private volatile int totalPassageirosTransportados = 0;

    private volatile float velocidadePixelsPorSegundo = Config.VELOCIDADE_PADRAO_PIXELS_SEG;
    private volatile float tempoTransicaoPortaSegundos = Config.TEMPO_TRANSICAO_PORTA_SEG;

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

    @Override
    public void run() {
        while (executando && !Thread.currentThread().isInterrupted()) {
            try {
                int proximoAndar = predio.determinarProximoAndar(andarAtual, direcao);

                if (proximoAndar == -1) {
                    mutexElevador.acquire();
                    try {
                        direcao = Direcao.PARADO;
                    } finally {
                        mutexElevador.release();
                    }

                    predio.aguardarNovasChamadas();
                    continue;
                }

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

                if (proximoAndar != andarAtual) {
                    VisitarAndar(proximoAndar);
                }

                if (predio.temOperacaoNoAndar(andarAtual, passageirosABordo)) {
                    AbrirPorta();
                    predio.atenderAndar(andarAtual, this);
                    FecharPorta();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void VisitarAndar(int andarDestino) throws InterruptedException {
        visitarAndar(andarDestino);
    }

    public void visitarAndar(int andarDestino) throws InterruptedException {
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
        float stepTime = 0.016f;

        while (Math.abs(posicaoY - targetY) > 1.0f) {
            float delta = velocidadePixelsPorSegundo * stepTime;
            if (posicaoY < targetY) {
                posicaoY = Math.min(posicaoY + delta, targetY);
            } else {
                posicaoY = Math.max(posicaoY - delta, targetY);
            }

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

    public boolean tentarReservarVaga() {
        return semVagas.tryAcquire();
    }

    public void liberarVaga() {
        semVagas.release();
    }

    public void adicionarPassageiro(Passageiro p) throws InterruptedException {
        mutexElevador.acquire();
        try {
            passageirosABordo.add(p);
            atualizarPosicoesPassageirosABordoInterno();
        } finally {
            mutexElevador.release();
        }
    }

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

        float cabineBaseX = posicaoX + Config.CABINE_OFFSET_X;
        float espacamento = Config.CABINE_LARGURA / (capacidade + 1);
        for (int i = 0; i < total; i++) {
            Passageiro p = passageirosABordo.get(i);
            float offsetX = espacamento * (i + 1) - Config.PASSAGEIRO_CABINE_LARGURA / 2f;
            p.setPosX(cabineBaseX + offsetX);
            p.setPosY(posicaoY + Config.CABINE_OFFSET_Y + 8f);
            p.setTargetX(p.getPosX());
            p.setTargetY(p.getPosY());
        }
    }

    private float calcularYDoAndar(int andar) {
        return Config.calcularYAndar(andar);
    }

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
