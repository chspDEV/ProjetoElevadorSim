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
    public static final float Y_BASE_ANDAR_0 = 0f;
    public static final float ESPACAMENTO_ANDAR = 90f;
    public static final float OFFSET_PES_PASSAGEIRO = 14f;

    public static float calcularYAndar(int andar) {
        return Y_BASE_ANDAR_0 + andar * ESPACAMENTO_ANDAR;
    }

    public static float calcularYPassageiroNoAndar(int andar) {
        return calcularYAndar(andar) + OFFSET_PES_PASSAGEIRO;
    }

    // =========================================================================
    // 3. ELEVADOR E CABINE (TAMANHO BASE CONFIGURAVEL)
    // =========================================================================
    // Voce configura apenas o posicionamento e o tamanho base desejado:
    public static final float ELEVADOR_X = 245f;
    public static final float ELEVADOR_LARGURA = 120f;
    public static final float ELEVADOR_ALTURA = 120f;

    // Fundo da cabine interna (RECALCULADO AUTOMATICAMENTE na proporcao 1:1 da imagem 512x512)
    public static final float CABINE_OFFSET_X = ELEVADOR_LARGURA * 0.20f;
    public static final float CABINE_OFFSET_Y = ELEVADOR_ALTURA * 0.02f;
    public static final float CABINE_LARGURA  = ELEVADOR_LARGURA * 0.60f;
    public static final float CABINE_ALTURA   = ELEVADOR_ALTURA * 0.70f;

    // Moldura do elevador (RECALCULADA AUTOMATICAMENTE na proporcao 1:1 da imagem 512x512)
    public static final float MOLDURA_TOPO_OFFSET_Y = ELEVADOR_ALTURA * 0.72f;
    public static final float MOLDURA_TOPO_LARGURA  = ELEVADOR_LARGURA;
    public static final float MOLDURA_TOPO_ALTURA   = ELEVADOR_ALTURA * 0.28f;
    public static final float MOLDURA_ESQ_LARGURA   = ELEVADOR_LARGURA * 0.20f;
    public static final float MOLDURA_ESQ_ALTURA    = ELEVADOR_ALTURA;
    public static final float MOLDURA_DIR_OFFSET_X  = ELEVADOR_LARGURA * 0.80f;
    public static final float MOLDURA_DIR_LARGURA   = ELEVADOR_LARGURA * 0.20f;
    public static final float MOLDURA_DIR_ALTURA    = ELEVADOR_ALTURA;

    // Velocidades fisicas
    public static final float VELOCIDADE_PADRAO_PIXELS_SEG = 220f;

    // =========================================================================
    // 4. PORTAS DO ELEVADOR (CONFIGURACAO INDEPENDENTE)
    // =========================================================================
    // Voce configura apenas a largura e altura da porta (padrao: mesmo tamanho do elevador):
    public static final float PORTA_LARGURA = ELEVADOR_LARGURA;
    public static final float PORTA_ALTURA  = ELEVADOR_ALTURA;

    // Posicionamento base das portas (offset em relacao ao elevador)
    public static final float PORTA_OFFSET_X = 0f;
    public static final float PORTA_OFFSET_Y = 0f;

    // Ajustes finos opcionais para cada folha de porta
    public static final float PORTA_ESQ_OFFSET_X = 0f;
    public static final float PORTA_DIR_OFFSET_X = 35f;

    // Deslocamento maximo de abertura (RECALCULADO AUTOMATICAMENTE em ~35% da largura da porta)
    public static final float PORTA_SLIDE_MAX = PORTA_LARGURA * 0.35f;
    public static final float TEMPO_TRANSICAO_PORTA_SEG = 0.45f;

    // =========================================================================
    // 5. PASSAGEIROS E FILAS
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

    // Gerador de passageiros e limite por andar
    public static final int LIMITE_PASSAGEIROS_POR_ANDAR = 5;
    public static final long INTERVALO_GERACAO_MS = 2800;

    // Saida do corredor apos desembarque
    public static final float SAIDA_CORREDOR_X = 800f;

    // =========================================================================
    // 6. PAINEL LATERAL A DIREITA
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
    // 7. TEXTOS DO SISTEMA
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
    public static final String TEXTO_LIMITE_POR_ANDAR = "Limite p/ andar: ";
    public static final String TEXTO_DESTINO_PREFIXO = "Dest: ";

    // Categoria Atalhos
    public static final String CATEGORIA_ATALHOS = "ATALHOS";
    public static final String ATALHO_ESPACO = "[ESPACO] Criar passageiro";
    public static final String ATALHO_NUMEROS = "[0-7] Criar no andar";
    public static final String ATALHO_SPAWN = "[A] Alternar spawn";
    public static final String ATALHO_PAUSAR = "[P] Pausar";
    public static final String ATALHO_VELOCIDADE = "[+/-] Velocidade";
}
