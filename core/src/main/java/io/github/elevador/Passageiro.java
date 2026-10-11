package io.github.elevador;

import java.util.concurrent.Semaphore;

public class Passageiro implements Runnable {

    public enum Estado {
        CRIADO,
        AGUARDANDO_FILA,
        EMBARCANDO,
        NO_ELEVADOR,
        DESEMBARCANDO,
        CONCLUIDO
    }

    private final int id;
    private final String nome;
    private final int andarOrigem;
    private final int andarDestino;
    private final int spriteIndex;
    private final Predio predio;
    private final Thread thread;

    private final Semaphore semPodeEmbarcar = new Semaphore(0, true);
    private final Semaphore semEmbarqueConcluido = new Semaphore(0, true);
    private final Semaphore semPodeDesembarcar = new Semaphore(0, true);
    private final Semaphore semDesembarqueConcluido = new Semaphore(0, true);

    private volatile Estado estado = Estado.CRIADO;

    private float posX;
    private float posY;
    private float targetX;
    private float targetY;
    private float opacidade = 1.0f;

    public Passageiro(int id, int andarOrigem, int andarDestino, int spriteIndex, Predio predio) {
        this.id = id;
        this.nome = "Passageiro #" + id;
        this.andarOrigem = andarOrigem;
        this.andarDestino = andarDestino;
        this.spriteIndex = spriteIndex;
        this.predio = predio;

        this.posY = Config.calcularYPassageiroNoAndar(andarOrigem);
        this.posX = 1200f; 
        this.targetX = Config.FILA_X_INICIAL;
        this.targetY = this.posY;

        this.thread = new Thread(this, "Thread-" + nome);
    }

    public void iniciar() {
        thread.start();
    }

    @Override
    public void run() {
        boolean aBordo = false;
        try {
            if (!predio.chamarElevador(this)) {
                return;
            }

            semPodeEmbarcar.acquire();
            try {
                predio.entrarElevador(this);
                aBordo = true;
            } finally {
                semEmbarqueConcluido.release();
            }

            semPodeDesembarcar.acquire();
            try {
                predio.sairElevador(this);
                aBordo = false;
            } finally {
                semDesembarqueConcluido.release();
            }

            predio.caminharParaSaida(this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (aBordo) {
                predio.getElevador().liberarVaga();
            }
        }
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public int getAndarOrigem() {
        return andarOrigem;
    }

    public int getAndarDestino() {
        return andarDestino;
    }

    public int getSpriteIndex() {
        return spriteIndex;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Semaphore getSemPodeEmbarcar() {
        return semPodeEmbarcar;
    }

    public Semaphore getSemEmbarqueConcluido() {
        return semEmbarqueConcluido;
    }

    public Semaphore getSemPodeDesembarcar() {
        return semPodeDesembarcar;
    }

    public Semaphore getSemDesembarqueConcluido() {
        return semDesembarqueConcluido;
    }

    public Thread getThread() {
        return thread;
    }

    public float getPosX() {
        return posX;
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public float getPosY() {
        return posY;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getTargetX() {
        return targetX;
    }

    public void setTargetX(float targetX) {
        this.targetX = targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public void setTargetY(float targetY) {
        this.targetY = targetY;
    }

    public float getOpacidade() {
        return opacidade;
    }

    public void setOpacidade(float opacidade) {
        this.opacidade = opacidade;
    }
}
