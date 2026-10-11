package io.github.elevador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Predio {

    private final int totalAndares;
    private final Elevador elevador;

    @SuppressWarnings("unchecked")
    private final Queue<Passageiro>[] filasPorAndar;

    private final List<Passageiro> passageirosConcluidos = new ArrayList<>();

    private final Semaphore mutexPredio = new Semaphore(1, true);

    private final Semaphore semNovasChamadas = new Semaphore(0, true);

    public Predio(int totalAndares, int capacidadeElevador, int andarInicial) {
        if (totalAndares <= 5) {
            throw new IllegalArgumentException("O prédio deve possuir F > 5 andares. Informado: " + totalAndares);
        }
        this.totalAndares = totalAndares;

        this.filasPorAndar = new LinkedList[totalAndares];
        for (int i = 0; i < totalAndares; i++) {
            this.filasPorAndar[i] = new LinkedList<>();
        }

        this.elevador = new Elevador(this, capacidadeElevador, andarInicial);
    }

    public void iniciarSimulacao() {
        elevador.iniciar();
    }

    public void encerrarSimulacao() {
        elevador.parar();
    }

    public boolean chamarElevador(Passageiro passageiro) throws InterruptedException {
        int andar = passageiro.getAndarOrigem();
        if (andar < 0 || andar >= totalAndares) {
            throw new IllegalArgumentException("Andar inválido: " + andar);
        }

        mutexPredio.acquire();
        try {
            if (filasPorAndar[andar].size() >= Config.LIMITE_PASSAGEIROS_POR_ANDAR) {
                return false;
            }

            passageiro.setEstado(Passageiro.Estado.AGUARDANDO_FILA);
            filasPorAndar[andar].add(passageiro);
            atualizarPosicoesFilaNoAndar(andar);

            semNovasChamadas.release();
            return true;
        } finally {
            mutexPredio.release();
        }
    }

    public boolean podeAdicionarNaFila(int andar) {
        if (andar < 0 || andar >= totalAndares) return false;
        return getQuantidadeFila(andar) < Config.LIMITE_PASSAGEIROS_POR_ANDAR;
    }

    public void entrarElevador(Passageiro passageiro) throws InterruptedException {
        passageiro.setEstado(Passageiro.Estado.EMBARCANDO);

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

    public void sairElevador(Passageiro passageiro) throws InterruptedException {
        passageiro.setEstado(Passageiro.Estado.DESEMBARCANDO);

        elevador.removerPassageiro(passageiro);
        elevador.liberarVaga();

        mutexPredio.acquire();
        try {
            passageirosConcluidos.add(passageiro);
        } finally {
            mutexPredio.release();
        }

        float startX = passageiro.getPosX();
        float saidaCabineX = Config.ELEVADOR_X + Config.ELEVADOR_LARGURA + 10f;
        int passos = 8;
        for (int i = 1; i <= passos; i++) {
            passageiro.setPosX(startX + (saidaCabineX - startX) * ((float) i / passos));
            Thread.sleep(15);
        }
    }

    public void caminharParaSaida(Passageiro passageiro) throws InterruptedException {
        float startX = passageiro.getPosX();
        float targetX = Config.SAIDA_CORREDOR_X;
        int passos = 35;
        for (int i = 1; i <= passos; i++) {
            passageiro.setPosX(startX + (targetX - startX) * ((float) i / passos));
            Thread.sleep(16);
        }

        passageiro.setEstado(Passageiro.Estado.CONCLUIDO);

        mutexPredio.acquire();
        try {
            passageirosConcluidos.remove(passageiro);
        } finally {
            mutexPredio.release();
        }
    }

    public void aguardarNovasChamadas() throws InterruptedException {
        semNovasChamadas.tryAcquire(250, TimeUnit.MILLISECONDS);
    }

    public int determinarProximoAndar(int andarAtual, Elevador.Direcao direcaoAtual) throws InterruptedException {
        List<Passageiro> aBordo = elevador.getCopiaPassageirosABordo();
        boolean elevadorCheio = (aBordo.size() >= elevador.getCapacidade());

        mutexPredio.acquire();
        try {
            boolean[] chamados = new boolean[totalAndares];
            int totalRequisicoes = 0;

            for (Passageiro p : aBordo) {
                chamados[p.getAndarDestino()] = true;
                totalRequisicoes++;
            }

            if (!elevadorCheio) {
                for (int i = 0; i < totalAndares; i++) {
                    if (!filasPorAndar[i].isEmpty()) {
                        chamados[i] = true;
                        totalRequisicoes++;
                    }
                }
            }

            if (totalRequisicoes == 0) {
                return -1;
            }

            if (chamados[andarAtual]) {
                return andarAtual;
            }

            if (direcaoAtual == Elevador.Direcao.SUBINDO || direcaoAtual == Elevador.Direcao.PARADO) {
                for (int i = andarAtual + 1; i < totalAndares; i++) {
                    if (chamados[i]) return i;
                }
                for (int i = andarAtual - 1; i >= 0; i--) {
                    if (chamados[i]) return i;
                }
            } else {
                for (int i = andarAtual - 1; i >= 0; i--) {
                    if (chamados[i]) return i;
                }
                for (int i = andarAtual + 1; i < totalAndares; i++) {
                    if (chamados[i]) return i;
                }
            }

            return -1;
        } finally {
            mutexPredio.release();
        }
    }

    public boolean temOperacaoNoAndar(int andar, List<Passageiro> passageirosABordo) throws InterruptedException {
        mutexPredio.acquire();
        try {
            for (Passageiro p : passageirosABordo) {
                if (p.getAndarDestino() == andar) {
                    return true;
                }
            }
            if (passageirosABordo.size() < elevador.getCapacidade()) {
                return !filasPorAndar[andar].isEmpty();
            }
            return false;
        } finally {
            mutexPredio.release();
        }
    }

    public void atenderAndar(int andar, Elevador elevador) throws InterruptedException {
        List<Passageiro> aBordo = elevador.getCopiaPassageirosABordo();
        List<Passageiro> saindo = new ArrayList<>();
        for (Passageiro p : aBordo) {
            if (p.getAndarDestino() == andar) {
                saindo.add(p);
            }
        }

        for (Passageiro p : saindo) {
            p.getSemPodeDesembarcar().release();

            boolean ok = p.getSemDesembarqueConcluido().tryAcquire(2000, TimeUnit.MILLISECONDS);
            if (!ok) {
                elevador.removerPassageiro(p);
                elevador.liberarVaga();
            }
        }

        while (true) {
            Passageiro proximo = null;

            mutexPredio.acquire();
            try {
                if (!filasPorAndar[andar].isEmpty()) {
                    if (elevador.tentarReservarVaga()) {
                        proximo = filasPorAndar[andar].poll();
                    }
                }
            } finally {
                mutexPredio.release();
            }

            if (proximo == null) {
                break;
            }

            proximo.getSemPodeEmbarcar().release();

            boolean ok = proximo.getSemEmbarqueConcluido().tryAcquire(2000, TimeUnit.MILLISECONDS);
            if (!ok) {
                elevador.liberarVaga();
            }
        }

        Thread.sleep(300);
    }

    private void atualizarPosicoesFilaNoAndar(int andar) {
        int idx = 0;
        for (Passageiro p : filasPorAndar[andar]) {
            float targetX = Config.FILA_X_INICIAL + idx * Config.FILA_ESPACAMENTO_X;
            float targetY = Config.calcularYPassageiroNoAndar(andar);
            p.setTargetX(targetX);
            p.setTargetY(targetY);
            p.setPosX(targetX);
            idx++;
        }
    }

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
