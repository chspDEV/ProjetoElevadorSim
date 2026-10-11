package io.github.elevador;

/**
 * Classe estatica com todas as informacoes e parametros para ajustes finos:
 * - Dimensoes e posicoes dos sprites (elevador, portas, passageiros)
 * - Posicionamento e espacamento de textos e categorias
 * - Escalas de fontes
 * - Textos e atalhos exibidos na interface
 */
public final class Config {

    private Config() {}

    // =========================================================================
    // 1. DIMENSOES DA TELA
    // =========================================================================
    public static final int LARGURA_TELA = 1280;
    public static final int ALTURA_TELA = 720;

    // =========================================================================
    // 2. ANDARES E PREDIO
    // =========================================================================
    public static final int TOTAL_ANDARES = 8;
    public static final int CAPACIDADE_PADRAO = 4;
    public static final int ANDAR_INICIAL = 0;

    // Coordenadas verticais dos pisos
    public static final float Y_BASE_ANDAR_0 = 15f;
    public static final float ESPACAMENTO_ANDAR = 90f;
    public static final float OFFSET_PES_PASSAGEIRO = 14f;

    public static float calcularYAndar(int andar) {
        return Y_BASE_ANDAR_0 + andar * ESPACAMENTO_ANDAR;
    }

    public static float calcularYPassageiroNoAndar(int andar) {
        return calcularYAndar(andar) + OFFSET_PES_PASSAGEIRO;
    }

    // =========================================================================
    // 3. ELEVADOR, CABINE E PORTAS
    // =========================================================================
    public static final float ELEVADOR_X = 145f;
    public static final float ELEVADOR_LARGURA = 104f;
    public static final float ELEVADOR_ALTURA = 104;

    // Fundo da cabine interna (atras das portas)
    public static final float CABINE_OFFSET_X = 22f;
    public static final float CABINE_OFFSET_Y = 4f;
    public static final float CABINE_LARGURA = 76f;
    public static final float CABINE_ALTURA = 70f;

    // Deslocamento maximo de abertura da porta
    public static final float PORTA_SLIDE_MAX = 38f;

    // Moldura do elevador (sobreposicao nas bordas)
    public static final float MOLDURA_TOPO_OFFSET_Y = 68f;
    public static final float MOLDURA_TOPO_LARGURA = 120f;
    public static final float MOLDURA_TOPO_ALTURA = 18f;
    public static final float MOLDURA_ESQ_LARGURA = 22f;
    public static final float MOLDURA_ESQ_ALTURA = 86f;
    public static final float MOLDURA_DIR_OFFSET_X = 98f;
    public static final float MOLDURA_DIR_LARGURA = 22f;
    public static final float MOLDURA_DIR_ALTURA = 86f;

    // Velocidades fisicas
    public static final float VELOCIDADE_PADRAO_PIXELS_SEG = 220f;
    public static final float TEMPO_TRANSICAO_PORTA_SEG = 0.45f;

    // =========================================================================
    // 4. PASSAGEIROS E FILAS
    // =========================================================================
    // Dimensoes dos sprites de pedestres na fila
    public static final float PASSAGEIRO_LARGURA = 52f;
    public static final float PASSAGEIRO_ALTURA = 52f;
    public static final float FILA_X_INICIAL = 370f;
    public static final float FILA_ESPACAMENTO_X = 52f;

    // Posicionamento do texto de destino sobre a cabeca do pedestre na fila
    public static final float TEXTO_DESTINO_OFFSET_X = -4f;
    public static final float TEXTO_DESTINO_OFFSET_Y = 64f;

    // Dimensoes dos sprites de passageiros dentro do elevador
    public static final float PASSAGEIRO_CABINE_LARGURA = 52f;
    public static final float PASSAGEIRO_CABINE_ALTURA = 52f;
    public static final float TEXTO_DESTINO_CABINE_OFFSET_X = -2f;
    public static final float TEXTO_DESTINO_CABINE_OFFSET_Y = 56f;

    // Gerador de passageiros
    public static final long INTERVALO_GERACAO_MS = 2800;

    // =========================================================================
    // 5. PAINEL LATERAL A DIREITA
    // =========================================================================
    // Retangulo de fundo
    public static final float PAINEL_BG_X = 1010f;
    public static final float PAINEL_BG_Y = 20f;
    public static final float PAINEL_BG_LARGURA = 255f;
    public static final float PAINEL_BG_ALTURA = 680f;

    // Posicionamento e espacamentos do texto
    public static final float PAINEL_TEXTO_X = 1030f;
    public static final float PAINEL_TEXTO_Y_INICIAL = 675f;
    public static final float ESPACAMENTO_ENTRE_CATEGORIAS = 38f;
    public static final float ESPACAMENTO_TITULO_CATEGORIA = 26f;
    public static final float ESPACAMENTO_LINHA_TEXTO = 22f;

    // Escalas de fonte
    public static final float ESCALA_FONTE_PADRAO = 1.0f;
    public static final float ESCALA_FONTE_CATEGORIA = 1.2f;
    public static final float ESCALA_FONTE_PEQUENA = 0.85f;

    // =========================================================================
    // 6. TEXTOS DO SISTEMA
    // =========================================================================
    public static final String TITULO_APLICACAO = "Simulador de Elevador";

    // Categoria Elevador
    public static final String CATEGORIA_ELEVADOR = "ELEVADOR";
    public static final String TEXTO_ANDAR_ATUAL = "Andar atual: ";
    public static final String TEXTO_ESTADO_PORTA = "Estado da porta: ";
    public static final String TEXTO_DIRECAO = "Direcao: ";

    // Categoria Passageiros
    public static final String CATEGORIA_PASSAGEIROS = "PASSAGEIROS";
    public static final String TEXTO_PASSAGEIROS = "Passageiros: ";
    public static final String TEXTO_TRANSPORTADOS = "Transportados: ";
    public static final String TEXTO_FILA_PREDIO = "Fila no predio: ";
    public static final String TEXTO_DESTINO_PREFIXO = "Dest: ";

    // Categoria Atalhos
    public static final String CATEGORIA_ATALHOS = "ATALHOS";
    public static final String ATALHO_ESPACO = "[ESPACO] Criar passageiro";
    public static final String ATALHO_NUMEROS = "[0-7] Criar no andar";
    public static final String ATALHO_SPAWN = "[A] Alternar spawn";
    public static final String ATALHO_PAUSAR = "[P] Pausar";
    public static final String ATALHO_VELOCIDADE = "[+/-] Velocidade";
}
