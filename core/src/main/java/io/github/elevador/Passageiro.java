package io.github.elevador;

import java.util.concurrent.Semaphore;

/**
 * Representa um passageiro (pedestre) no sistema de elevador.
 * Cada passageiro é executado em sua própria thread dedicada.
 * 
 * Regra: Métodos synchronized NÃO são utilizados. A sincronização
 * é realizada exclusivamente com a classe Semaphore.
 */
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
    private final int spriteIndex; // 1 a 6 (pessoa1.png a pessoa6.png)
    private final Predio predio;
    private final Thread thread;

    // Semáforos individuais para sincronização estrita com o elevador
    private final Semaphore semPodeEmbarcar = new Semaphore(0, true);
    private final Semaphore semEmbarqueConcluido = new Semaphore(0, true);
    private final Semaphore semPodeDesembarcar = new Semaphore(0, true);
    private final Semaphore semDesembarqueConcluido = new Semaphore(0, true);

    // Estado do passageiro
    private volatile Estado estado = Estado.CRIADO;

    // Coordenadas visuais para interpolação gráfica suave
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

        // Posição inicial no andar de origem (calculada via Config)
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
        try {
            // 1. Passageiro chega ao andar e chama o elevador
            predio.chamarElevador(this);

            // 2. Aguarda a chegada do elevador com portas abertas e permissão para embarcar
            semPodeEmbarcar.acquire();

            // 3. Embarca no elevador (interface externa)
            predio.entrarElevador(this);

            // Sinaliza para o elevador que o embarque foi concluído
            semEmbarqueConcluido.release();

            // 4. Aguarda a chegada ao andar de destino com portas abertas
            semPodeDesembarcar.acquire();

            // 5. Desembarca do elevador (interface externa)
            predio.sairElevador(this);

            // Sinaliza para o elevador que o desembarque foi concluído
            semDesembarqueConcluido.release();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --- Getters e Setters de Semáforos e Estado ---

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

    // --- Getters e Setters para Visualização Gráfica ---

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
