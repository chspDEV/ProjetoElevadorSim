package io.github.elevador;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Programa de teste / Gerador autônomo de passageiros.
 * Gera pedestres aleatórios em andares diversos, cada um com sua thread dedicada.
 */
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
        // Gera alguns passageiros iniciais para iniciar a simulação com movimento
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

    /**
     * Cria e inicia a thread de um passageiro com andar de origem e destino aleatórios.
     */
    public Passageiro criarPassageiroAleatorio() {
        int totalAndares = predio.getTotalAndares();
        int origem = random.nextInt(totalAndares);
        int destino;
        do {
            destino = random.nextInt(totalAndares);
        } while (destino == origem);

        int spriteIndex = 1 + random.nextInt(6); // pessoa1 a pessoa6
        return criarPassageiro(origem, destino, spriteIndex);
    }

    /**
     * Cria e inicia um passageiro em um andar específico com destino aleatório.
     */
    public Passageiro criarPassageiroNoAndar(int origem) {
        int totalAndares = predio.getTotalAndares();
        int destino;
        do {
            destino = random.nextInt(totalAndares);
        } while (destino == origem);

        int spriteIndex = 1 + random.nextInt(6);
        return criarPassageiro(origem, destino, spriteIndex);
    }

    /**
     * Cria a instância do passageiro e inicia sua thread.
     */
    public Passageiro criarPassageiro(int origem, int destino, int spriteIndex) {
        int id = proximoId.getAndIncrement();
        Passageiro p = new Passageiro(id, origem, destino, spriteIndex, predio);
        p.iniciar(); // Inicia a thread dedicada do pedestre
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
