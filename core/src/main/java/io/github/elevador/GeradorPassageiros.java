package io.github.elevador;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class GeradorPassageiros implements Runnable {

    private final Predio predio;
    private final Random random = new Random();
    private final AtomicInteger proximoId = new AtomicInteger(1);
    private final Thread thread;

    private volatile boolean ativo = true;
    private volatile boolean autoSpawnHabilitado = true;
    private volatile long intervaloMedioMs = Config.INTERVALO_GERACAO_MS;

    public GeradorPassageiros(Predio predio) {
        this.predio = predio;
        this.thread = new Thread(this, "Thread-GeradorPassageiros");
    }

    public void iniciar() {
        thread.start();
    }

    public void parar() {
        ativo = false;
        thread.interrupt();
    }

    @Override
    public void run() {
        try {
            Thread.sleep(800);
            for (int i = 0; i < 4; i++) {
                criarPassageiroAleatorio();
                Thread.sleep(400);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        while (ativo && !Thread.currentThread().isInterrupted()) {
            try {
                long espera = (long) (intervaloMedioMs * (0.7 + random.nextDouble() * 0.6));
                Thread.sleep(espera);

                if (autoSpawnHabilitado && ativo) {
                    criarPassageiroAleatorio();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public Passageiro criarPassageiroAleatorio() {
        int totalAndares = predio.getTotalAndares();
        java.util.List<Integer> andaresDisponiveis = new java.util.ArrayList<>();
        for (int i = 0; i < totalAndares; i++) {
            if (predio.podeAdicionarNaFila(i)) {
                andaresDisponiveis.add(i);
            }
        }

        if (andaresDisponiveis.isEmpty()) {
            return null;
        }

        int origem = andaresDisponiveis.get(random.nextInt(andaresDisponiveis.size()));
        int destino;
        do {
            destino = random.nextInt(totalAndares);
        } while (destino == origem);

        int spriteIndex = 1 + random.nextInt(6);
        return criarPassageiro(origem, destino, spriteIndex);
    }

    public Passageiro criarPassageiroNoAndar(int origem) {
        if (!predio.podeAdicionarNaFila(origem)) {
            return null;
        }

        int totalAndares = predio.getTotalAndares();
        int destino;
        do {
            destino = random.nextInt(totalAndares);
        } while (destino == origem);

        int spriteIndex = 1 + random.nextInt(6);
        return criarPassageiro(origem, destino, spriteIndex);
    }

    public Passageiro criarPassageiro(int origem, int destino, int spriteIndex) {
        int id = proximoId.getAndIncrement();
        Passageiro p = new Passageiro(id, origem, destino, spriteIndex, predio);
        p.iniciar();
        return p;
    }

    public boolean isAutoSpawnHabilitado() {
        return autoSpawnHabilitado;
    }

    public void setAutoSpawnHabilitado(boolean autoSpawnHabilitado) {
        this.autoSpawnHabilitado = autoSpawnHabilitado;
    }

    public void alternarAutoSpawn() {
        this.autoSpawnHabilitado = !this.autoSpawnHabilitado;
    }

    public void setIntervaloMedioMs(long intervaloMedioMs) {
        this.intervaloMedioMs = intervaloMedioMs;
    }
}
