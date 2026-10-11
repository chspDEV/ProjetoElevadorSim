package io.github.elevador;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.File;
import java.util.List;

/**
 * Aplicação gráfica principal em LibGDX para simulação do elevador multithread.
 * 
 * Requisitos e destaques:
 * - Resolução 1280x720 ajustada ao cenário do prédio.
 * - Instanciação única da classe Prédio (que cria e inicializa o Elevador).
 * - Renderização do elevador se deslocando entre os andares (0 a 7).
 * - Portas do elevador abrindo e fechando com animação visual clara.
 * - Interior do elevador iluminado com passageiros visíveis a bordo (Capacidade > 1 com pontuação extra!).
 * - Fila de passageiros nos andares com crachá visual do andar de destino sobre a cabeça.
 * - Animação de pedestres embarcando e desembarcando.
 * - Painel de telemetria lateral com status de concorrência, semáforos, ocupação e controles interativos.
 * - Controles por teclado e clique do mouse para gerar passageiros e testar cenários.
 */
public class Main extends ApplicationAdapter {

    // Constantes do Prédio e Elevador
    private static final int TOTAL_ANDARES = 8;        // F = 8 andares (0 a 7), satisfaz F > 5
    private static final int CAPACIDADE_PADRAO = 4;    // Capacidade > 1 (Bonificação Extra)
    private static final int ANDAR_INICIAL = 0;

    // Modelos concorrentes
    private Predio predio;
    private Elevador elevador;
    private GeradorPassageiros gerador;

    // Recursos gráficos do LibGDX
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont fontPadrao;
    private BitmapFont fontTitulo;
    private BitmapFont fontPequena;
    private GlyphLayout glyphLayout;

    // Texturas e Regiões
    private Texture texFundo;
    private Texture texElevador;
    private TextureRegion regElevadorTopo;
    private TextureRegion regElevadorEsq;
    private TextureRegion regElevadorDir;
    private Texture[] texPessoas;

    // Controle de tempo para animações
    private float tempoAnimacao = 0f;
    private boolean pausado = false;
    private float multiplicadorVelocidade = 1.0f;

    /**
     * Resolve arquivos de assets de forma resiliente em qualquer ambiente:
     * - Execução no Eclipse (working directory na raiz do projeto ou no submódulo)
     * - Execução via Gradle (working directory em assets/)
     * - Execução via JAR empacotado ou Classpath
     */
    public static FileHandle resolverArquivo(String caminho) {
        // 1. Tenta direto (working directory = assets/)
        FileHandle fh = Gdx.files.internal(caminho);
        if (fh.exists()) return fh;

        // 2. Tenta dentro da subpasta assets/ (working directory = raiz do projeto)
        fh = Gdx.files.internal("assets/" + caminho);
        if (fh.exists()) return fh;

        // 3. Tenta ../assets/ (working directory = lwjgl3/ ou core/)
        fh = Gdx.files.internal("../assets/" + caminho);
        if (fh.exists()) return fh;

        // 4. Tenta classpath
        fh = Gdx.files.classpath(caminho);
        if (fh.exists()) return fh;

        fh = Gdx.files.classpath("assets/" + caminho);
        if (fh.exists()) return fh;

        // 5. Busca dinâmica navegando os diretórios pais a partir de user.dir
        String userDir = System.getProperty("user.dir", ".");
        File current = new File(userDir);
        for (int depth = 0; depth < 5 && current != null && current.exists(); depth++) {
            File assetsDir = new File(current, "assets");
            if (assetsDir.exists() && assetsDir.isDirectory()) {
                File target = new File(assetsDir, caminho);
                if (target.exists()) {
                    return Gdx.files.absolute(target.getAbsolutePath());
                }
            }
            File directFile = new File(current, caminho);
            if (directFile.exists() && directFile.isFile()) {
                return Gdx.files.absolute(directFile.getAbsolutePath());
            }
            current = current.getParentFile();
        }

        // Fallback padrão
        return Gdx.files.internal(caminho);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        glyphLayout = new GlyphLayout();

        // Inicializa fontes
        fontPadrao = new BitmapFont();
        fontPadrao.setColor(Color.WHITE);

        fontTitulo = new BitmapFont();
        fontTitulo.getData().setScale(1.25f);
        fontTitulo.setColor(new Color(0.3f, 0.85f, 1.0f, 1.0f));

        fontPequena = new BitmapFont();
        fontPequena.getData().setScale(0.85f);
        fontPequena.setColor(Color.LIGHT_GRAY);

        // Carrega as texturas dos assets usando resolução resiliente
        texFundo = new Texture(resolverArquivo("background1280x720.png"));
        texElevador = new Texture(resolverArquivo("elevador.png"));
        int ew = texElevador.getWidth();
        int eh = texElevador.getHeight();
        regElevadorTopo = new TextureRegion(texElevador, 0, 0, ew, (int) (eh * 0.22f));
        regElevadorEsq = new TextureRegion(texElevador, 0, 0, (int) (ew * 0.18f), eh);
        regElevadorDir = new TextureRegion(texElevador, (int) (ew * 0.82f), 0, (int) (ew * 0.18f), eh);

        texPessoas = new Texture[6];
        for (int i = 0; i < 6; i++) {
            texPessoas[i] = new Texture(resolverArquivo("pessoa" + (i + 1) + ".png"));
        }

        // 1. Instância única da classe Prédio cria e inicializa o Elevador
        // (requisito explícito do enunciado)
        predio = new Predio(TOTAL_ANDARES, CAPACIDADE_PADRAO, ANDAR_INICIAL);
        elevador = predio.getElevador();

        // 2. Inicia a simulação com a thread do elevador
        predio.iniciarSimulacao();

        // 3. Inicia o programa de teste gerador de passageiros em threads
        gerador = new GeradorPassageiros(predio);
        gerador.iniciar();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        if (!pausado) {
            tempoAnimacao += delta;
        }

        processarEntradas();

        // Limpa a tela
        ScreenUtils.clear(0.08f, 0.10f, 0.15f, 1f);

        // 1. Desenha o fundo com os andares
        batch.begin();
        batch.draw(texFundo, 0, 0, 1280, 720);
        batch.end();

        // 2. Desenha elementos geométricos (painel lateral, interior do elevador, portas, badges)
        desenharGeometria();

        // 3. Desenha os sprites (passageiros e elevador) e textos (HUD e andares)
        desenharSpritesETextos();
    }

    private void processarEntradas() {
        // [ESPAÇO] - Gerar passageiro aleatório
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            gerador.criarPassageiroAleatorio();
        }

        // [0] a [7] - Gerar passageiro no andar específico
        for (int i = 0; i < TOTAL_ANDARES; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + i)) {
                gerador.criarPassageiroNoAndar(i);
            }
        }

        // [A] - Alternar Auto-Spawn
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            gerador.alternarAutoSpawn();
        }

        // [P] - Pausar / Continuar
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            pausado = !pausado;
        }

        // [+] / [-] - Ajustar velocidade
        if (Gdx.input.isKeyJustPressed(Input.Keys.PLUS) || Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) {
            multiplicadorVelocidade = Math.min(3.0f, multiplicadorVelocidade + 0.25f);
            elevador.setVelocidadeMultiplicador(multiplicadorVelocidade);
            gerador.setIntervaloMedioMs((long) (2800 / multiplicadorVelocidade));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            multiplicadorVelocidade = Math.max(0.5f, multiplicadorVelocidade - 0.25f);
            elevador.setVelocidadeMultiplicador(multiplicadorVelocidade);
            gerador.setIntervaloMedioMs((long) (2800 / multiplicadorVelocidade));
        }

        // Clique do mouse nos andares para gerar passageiro naquele andar
        if (Gdx.input.justTouched()) {
            float mouseX = Gdx.input.getX();
            float mouseY = 720 - Gdx.input.getY();

            // Área das plataformas dos andares
            if (mouseX >= 350 && mouseX <= 980 && mouseY >= 25 && mouseY <= 710) {
                int andarClicado = (int) ((mouseY - 20) / 90);
                if (andarClicado >= 0 && andarClicado < TOTAL_ANDARES) {
                    gerador.criarPassageiroNoAndar(andarClicado);
                }
            }

            // Clique no botão Auto-Spawn no painel
            if (mouseX >= 1020 && mouseX <= 1260 && mouseY >= 110 && mouseY <= 140) {
                gerador.alternarAutoSpawn();
            }
        }
    }

    /**
     * Renderização de formas (ShapeRenderer):
     * - Fundo translúcido do painel de telemetria
     * - Cabine interna iluminada do elevador quando as portas abrem
     * - Folhas de porta se abrindo/fechando
     * - Balões de destino dos passageiros
     */
    private void desenharGeometria() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // --- PAINEL LATERAL DE MONITORAMENTO ---
        shapeRenderer.setColor(0.06f, 0.09f, 0.16f, 0.88f);
        shapeRenderer.rect(1005, 15, 260, 690);

        // Barra de progresso / ocupação de capacidade
        float barX = 1025;
        float barY = 460;
        float barW = 220;
        float barH = 16;
        shapeRenderer.setColor(0.15f, 0.2f, 0.3f, 1f);
        shapeRenderer.rect(barX, barY, barW, barH);

        int aBordo = elevador.getQuantidadePassageiros();
        int cap = elevador.getCapacidade();
        float fracCap = (float) aBordo / cap;
        shapeRenderer.setColor(fracCap < 0.75f ? new Color(0.2f, 0.8f, 0.4f, 1f) : new Color(1.0f, 0.5f, 0.2f, 1f));
        shapeRenderer.rect(barX, barY, barW * fracCap, barH);

        // Botão Auto-Spawn
        boolean auto = gerador.isAutoSpawnHabilitado();
        shapeRenderer.setColor(auto ? new Color(0.15f, 0.5f, 0.25f, 0.9f) : new Color(0.5f, 0.2f, 0.2f, 0.9f));
        shapeRenderer.rect(1020, 110, 230, 28);

        // --- INTERIOR DA CABINE DO ELEVADOR (QUANDO PORTA ABERTA OU ABRINDO) ---
        float elevX = elevador.getPosicaoX();
        float elevY = elevador.getPosicaoY();
        float abertura = elevador.getAberturaPorta();

        if (abertura > 0.02f) {
            // Fundo iluminado da cabine interna
            shapeRenderer.setColor(0.85f, 0.86f, 0.88f, 1.0f);
            shapeRenderer.rect(elevX + 18, elevY + 4, 84, 72);

            // Piso da cabine
            shapeRenderer.setColor(0.25f, 0.28f, 0.35f, 1.0f);
            shapeRenderer.rect(elevX + 18, elevY + 4, 84, 8);

            // Teto iluminado
            shapeRenderer.setColor(1.0f, 0.98f, 0.80f, 0.6f);
            shapeRenderer.rect(elevX + 22, elevY + 70, 76, 5);
        }

        // --- BALÕES DE DESTINO DOS PASSAGEIROS AGUARDANDO NOS ANDARES ---
        for (int i = 0; i < TOTAL_ANDARES; i++) {
            List<Passageiro> fila = predio.getPassageirosNaFila(i);
            for (Passageiro p : fila) {
                desenharBalaoPassageiro(p);
            }
        }

        shapeRenderer.end();

        // --- BORDAS E LINHAS DECORATIVAS ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.25f, 0.60f, 0.95f, 0.65f);
        shapeRenderer.rect(1005, 15, 260, 690);

        shapeRenderer.setColor(0.2f, 0.7f, 0.9f, 0.8f);
        shapeRenderer.rect(1020, 110, 230, 28);

        // Se a porta estiver aberta ou em transição, desenha os painéis da porta deslizando
        if (abertura > 0.0f) {
            float portaW = 38f;
            float deslocamento = portaW * abertura;

            // Painel esquerdo deslizando para a esquerda
            shapeRenderer.setColor(0.65f, 0.68f, 0.72f, 1.0f);
            shapeRenderer.rect(elevX + 22 - deslocamento, elevY + 8, portaW, 66);

            // Painel direito deslizando para a direita
            shapeRenderer.rect(elevX + 60 + deslocamento, elevY + 8, portaW, 66);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void desenharBalaoPassageiro(Passageiro p) {
        float px = p.getPosX();
        float py = p.getPosY() + 52f;
        // Fundo do balãozinho de destino
        shapeRenderer.setColor(0.12f, 0.18f, 0.28f, 0.92f);
        shapeRenderer.rect(px - 14, py, 60, 18);
        // Indicador de cor do destino
        shapeRenderer.setColor(0.3f, 0.8f, 1.0f, 1.0f);
        shapeRenderer.rect(px - 14, py, 4, 18);
    }

    /**
     * Renderização de Sprites e Textos com SpriteBatch:
     */
    private void desenharSpritesETextos() {
        batch.begin();

        // 1. DESENHA O ELEVADOR
        float elevX = elevador.getPosicaoX();
        float elevY = elevador.getPosicaoY();
        float abertura = elevador.getAberturaPorta();

        if (abertura <= 0.05f) {
            // Porta totalmente fechada: desenha o sprite completo do elevador
            batch.draw(texElevador, elevX, elevY, elevador.getLargura(), elevador.getAltura());
        } else {
            // Porta aberta: passageiros a bordo são desenhados dentro da cabine
            List<Passageiro> aBordo = elevador.getCopiaPassageirosABordo();
            for (Passageiro p : aBordo) {
                int spriteIdx = Math.max(0, Math.min(5, p.getSpriteIndex() - 1));
                batch.draw(texPessoas[spriteIdx], p.getPosX(), p.getPosY(), 36, 46);

                // Mostra andar de destino sobre a cabeça do passageiro a bordo
                fontPequena.setColor(Color.YELLOW);
                fontPequena.draw(batch, "->" + p.getAndarDestino(), p.getPosX() + 4, p.getPosY() + 56);
            }

            // Moldura externa do elevador (topo e laterais)
            batch.setColor(1f, 1f, 1f, 0.95f);
            batch.draw(regElevadorTopo, elevX, elevY + 68, 120, 18);
            batch.draw(regElevadorEsq, elevX, elevY, 22, 86);
            batch.draw(regElevadorDir, elevX + 98, elevY, 22, 86);
            batch.setColor(Color.WHITE);
        }

        // 2. INDICADORES LUMINOSOS DE DIREÇÃO NO ELEVADOR
        if (elevador.getDirecao() == Elevador.Direcao.SUBINDO) {
            float pulso = 0.6f + 0.4f * (float) Math.sin(tempoAnimacao * 8);
            fontPadrao.setColor(0.3f, 1.0f, 0.3f, pulso);
            fontPadrao.draw(batch, "▲", elevX + 35, elevY + 78);
        } else if (elevador.getDirecao() == Elevador.Direcao.DESCENDO) {
            float pulso = 0.6f + 0.4f * (float) Math.sin(tempoAnimacao * 8);
            fontPadrao.setColor(1.0f, 0.3f, 0.3f, pulso);
            fontPadrao.draw(batch, "▼", elevX + 75, elevY + 78);
        }

        // 3. DESENHA OS PASSAGEIROS NAS FILAS DOS ANDARES
        for (int i = 0; i < TOTAL_ANDARES; i++) {
            List<Passageiro> fila = predio.getPassageirosNaFila(i);
            for (Passageiro p : fila) {
                int spriteIdx = Math.max(0, Math.min(5, p.getSpriteIndex() - 1));

                // Efeito suave de respiração/caminhada
                float bobY = (p.getEstado() == Passageiro.Estado.EMBARCANDO || p.getEstado() == Passageiro.Estado.DESEMBARCANDO)
                        ? (float) Math.sin(tempoAnimacao * 14) * 3f
                        : 0f;

                batch.draw(texPessoas[spriteIdx], p.getPosX(), p.getPosY() + bobY, 42, 52);

                // Texto do crachá do andar de destino
                fontPequena.setColor(Color.CYAN);
                fontPequena.draw(batch, "->" + p.getAndarDestino(), p.getPosX() - 6, p.getPosY() + 65);
            }
        }

        // 4. DESENHA PASSAGEIROS EM PROCESSO DE DESEMBARQUE / CONCLUÍDOS RECENTES
        List<Passageiro> concluidos = predio.getCopiaPassageirosConcluidos();
        int startIndex = Math.max(0, concluidos.size() - 6);
        for (int i = startIndex; i < concluidos.size(); i++) {
            Passageiro p = concluidos.get(i);
            if (p.getEstado() == Passageiro.Estado.CONCLUIDO && p.getPosX() < 900f) {
                int spriteIdx = Math.max(0, Math.min(5, p.getSpriteIndex() - 1));
                batch.setColor(1f, 1f, 1f, 0.75f);
                batch.draw(texPessoas[spriteIdx], p.getPosX(), p.getPosY(), 42, 52);
                fontPequena.setColor(Color.GREEN);
                fontPequena.draw(batch, "OK!", p.getPosX() + 8, p.getPosY() + 65);
                batch.setColor(Color.WHITE);
            }
        }

        // 5. RENDERIZAÇÃO DO PAINEL LATERAL (TELEMETRIA E CONTROLES)
        desenharPainelLateral();

        batch.end();
    }

    private void desenharPainelLateral() {
        float x = 1018;
        float y = 690;

        // Título Principal
        fontTitulo.setColor(new Color(0.35f, 0.85f, 1.0f, 1.0f));
        fontTitulo.draw(batch, "CONTROLE ELEVADOR", x + 5, y);
        y -= 22;

        fontPequena.setColor(Color.LIGHT_GRAY);
        fontPequena.draw(batch, "Sistemas Multithread & Semáforos", x + 5, y);
        y -= 25;

        // Linha divisória estética
        fontPequena.setColor(new Color(0.25f, 0.45f, 0.7f, 0.8f));
        fontPequena.draw(batch, "---------------------------------", x + 5, y);
        y -= 20;

        // STATUS DO ELEVADOR
        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, "Andar Atual: ", x + 5, y);
        fontTitulo.setColor(Color.YELLOW);
        fontTitulo.draw(batch, "" + elevador.getAndarAtual(), x + 115, y + 2);
        y -= 28;

        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, "Direção: ", x + 5, y);
        Elevador.Direcao dir = elevador.getDirecao();
        if (dir == Elevador.Direcao.SUBINDO) {
            fontPadrao.setColor(Color.GREEN);
            fontPadrao.draw(batch, "SUBINDO ▲", x + 85, y);
        } else if (dir == Elevador.Direcao.DESCENDO) {
            fontPadrao.setColor(new Color(1f, 0.4f, 0.3f, 1f));
            fontPadrao.draw(batch, "DESCENDO ▼", x + 85, y);
        } else {
            fontPadrao.setColor(Color.GRAY);
            fontPadrao.draw(batch, "PARADO ■", x + 85, y);
        }
        y -= 26;

        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, "Portas: ", x + 5, y);
        Elevador.EstadoPorta estPorta = elevador.getEstadoPorta();
        if (estPorta == Elevador.EstadoPorta.ABERTA) {
            fontPadrao.setColor(Color.GREEN);
        } else if (estPorta == Elevador.EstadoPorta.FECHADA) {
            fontPadrao.setColor(Color.CYAN);
        } else {
            fontPadrao.setColor(Color.ORANGE);
        }
        fontPadrao.draw(batch, estPorta.name(), x + 75, y);
        y -= 28;

        // Ocupação do Elevador (Buffer)
        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, "Ocupação: ", x + 5, y);
        fontPadrao.setColor(Color.YELLOW);
        fontPadrao.draw(batch, elevador.getQuantidadePassageiros() + " / " + elevador.getCapacidade() + " pessoas", x + 95, y);
        y -= 38; // compensa a barra desenhada no shapeRenderer

        // Tag de Bonificação
        fontPequena.setColor(new Color(0.3f, 1.0f, 0.5f, 1.0f));
        fontPequena.draw(batch, "[★ BÔNUS: CAPACIDADE > 1 ATIVADO]", x + 5, y);
        y -= 24;

        // Passageiros a Bordo
        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, "Passageiros a bordo:", x + 5, y);
        y -= 20;

        List<Passageiro> aBordo = elevador.getCopiaPassageirosABordo();
        if (aBordo.isEmpty()) {
            fontPequena.setColor(Color.GRAY);
            fontPequena.draw(batch, "(cabine vazia)", x + 20, y);
            y -= 20;
        } else {
            for (Passageiro p : aBordo) {
                fontPequena.setColor(Color.WHITE);
                fontPequena.draw(batch, "• #" + p.getId() + " (Origem: " + p.getAndarOrigem() + " -> Dest: " + p.getAndarDestino() + ")", x + 15, y);
                y -= 18;
            }
        }

        y -= 10;
        fontPequena.setColor(new Color(0.25f, 0.45f, 0.7f, 0.8f));
        fontPequena.draw(batch, "---------------------------------", x + 5, y);
        y -= 20;

        // ESTATÍSTICAS
        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, "Fila no Prédio:", x + 5, y);
        fontPadrao.setColor(Color.ORANGE);
        fontPadrao.draw(batch, "" + predio.getTotalPassageirosEmEspera() + " pessoas", x + 130, y);
        y -= 24;

        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, "Transportados:", x + 5, y);
        fontPadrao.setColor(Color.GREEN);
        fontPadrao.draw(batch, "" + elevador.getTotalPassageirosTransportados() + " pessoas", x + 130, y);
        y -= 26;

        fontPequena.setColor(new Color(0.4f, 0.9f, 0.9f, 1f));
        fontPequena.draw(batch, "Sincronização: 100% Semaphore", x + 5, y);
        y -= 18;
        fontPequena.draw(batch, "Restrição: Zero synchronized", x + 5, y);
        y -= 26;

        // BOTÃO AUTO-SPAWN
        boolean auto = gerador.isAutoSpawnHabilitado();
        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, auto ? "[A] Auto-Spawn: LIGADO" : "[A] Auto-Spawn: PAUSADO", x + 20, 128);

        // CONTROLES DE TECLADO
        float cy = 96;
        fontPequena.setColor(Color.LIGHT_GRAY);
        fontPequena.draw(batch, "[ESPAÇO] Criar passageiro aleatório", x + 5, cy);
        cy -= 18;
        fontPequena.draw(batch, "[0 a 7] Criar passageiro no andar", x + 5, cy);
        cy -= 18;
        fontPequena.draw(batch, "[Clique] Criar passageiro na plataforma", x + 5, cy);
        cy -= 18;
        fontPequena.draw(batch, "[P] Pausar | [+/-] Vel: " + String.format("%.2fx", multiplicadorVelocidade), x + 5, cy);
    }

    @Override
    public void dispose() {
        if (gerador != null) gerador.parar();
        if (predio != null) predio.encerrarSimulacao();

        batch.dispose();
        shapeRenderer.dispose();
        fontPadrao.dispose();
        fontTitulo.dispose();
        fontPequena.dispose();

        texFundo.dispose();
        texElevador.dispose();
        for (Texture t : texPessoas) {
            if (t != null) t.dispose();
        }
    }
}
