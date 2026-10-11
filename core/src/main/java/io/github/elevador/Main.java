package io.github.elevador;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.File;
import java.util.List;

public class Main extends ApplicationAdapter {

    private Predio predio;
    private Elevador elevador;
    private GeradorPassageiros gerador;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont fontPadrao;
    private BitmapFont fontCategoria;
    private BitmapFont fontPequena;

    private Texture texFundo;
    private Texture texElevador;
    private Texture texPortaL;
    private Texture texPortaR;
    private TextureRegion regPortaL;
    private TextureRegion regPortaR;
    private TextureRegion regElevadorTopo;
    private TextureRegion regElevadorEsq;
    private TextureRegion regElevadorDir;
    private Texture[] texPessoas;

    private float tempoAnimacao = 0f;
    private boolean pausado = false;
    private float multiplicadorVelocidade = 1.0f;

    public static FileHandle resolverArquivo(String caminho) {
        FileHandle fh = Gdx.files.internal(caminho);
        if (fh.exists()) return fh;

        fh = Gdx.files.internal("assets/" + caminho);
        if (fh.exists()) return fh;

        fh = Gdx.files.internal("../assets/" + caminho);
        if (fh.exists()) return fh;

        fh = Gdx.files.classpath(caminho);
        if (fh.exists()) return fh;

        fh = Gdx.files.classpath("assets/" + caminho);
        if (fh.exists()) return fh;

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

        return Gdx.files.internal(caminho);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        fontPadrao = new BitmapFont();
        fontPadrao.getData().setScale(Config.ESCALA_FONTE_PADRAO);
        fontPadrao.setColor(Color.WHITE);

        fontCategoria = new BitmapFont();
        fontCategoria.getData().setScale(Config.ESCALA_FONTE_CATEGORIA);
        fontCategoria.setColor(Color.WHITE);

        fontPequena = new BitmapFont();
        fontPequena.getData().setScale(Config.ESCALA_FONTE_PEQUENA);
        fontPequena.setColor(Color.WHITE);

        texFundo = new Texture(resolverArquivo("background1280x720.png"));
        texElevador = new Texture(resolverArquivo("elevador.png"));

        texPortaL = new Texture(resolverArquivo("elevadorPortaL.png"));
        texPortaR = new Texture(resolverArquivo("elevadorPortaR.png"));
        regPortaL = new TextureRegion(texPortaL);
        regPortaR = new TextureRegion(texPortaR);
        regPortaR.flip(true, false);

        int ew = texElevador.getWidth();
        int eh = texElevador.getHeight();
        regElevadorTopo = new TextureRegion(texElevador, 0, 0, ew, (int) (eh * 0.28f));
        regElevadorEsq = new TextureRegion(texElevador, 0, 0, (int) (ew * 0.20f), eh);
        regElevadorDir = new TextureRegion(texElevador, (int) (ew * 0.80f), 0, (int) (ew * 0.20f), eh);

        texPessoas = new Texture[6];
        for (int i = 0; i < 6; i++) {
            texPessoas[i] = new Texture(resolverArquivo("pessoa" + (i + 1) + ".png"));
        }

        predio = new Predio(Config.TOTAL_ANDARES, Config.CAPACIDADE_PADRAO, Config.ANDAR_INICIAL);
        elevador = predio.getElevador();

        predio.iniciarSimulacao();

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

        ScreenUtils.clear(0.08f, 0.10f, 0.15f, 1f);

        batch.begin();
        batch.draw(texFundo, 0, 0, Config.LARGURA_TELA, Config.ALTURA_TELA);
        batch.end();

        desenharGeometria();
        desenharSpritesETextos();
    }

    private void processarEntradas() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            gerador.criarPassageiroAleatorio();
        }

        for (int i = 0; i < Config.TOTAL_ANDARES; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + i)) {
                gerador.criarPassageiroNoAndar(i);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            gerador.alternarAutoSpawn();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            pausado = !pausado;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.PLUS) || Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) {
            multiplicadorVelocidade = Math.min(3.0f, multiplicadorVelocidade + 0.25f);
            elevador.setVelocidadeMultiplicador(multiplicadorVelocidade);
            gerador.setIntervaloMedioMs((long) (Config.INTERVALO_GERACAO_MS / multiplicadorVelocidade));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            multiplicadorVelocidade = Math.max(0.5f, multiplicadorVelocidade - 0.25f);
            elevador.setVelocidadeMultiplicador(multiplicadorVelocidade);
            gerador.setIntervaloMedioMs((long) (Config.INTERVALO_GERACAO_MS / multiplicadorVelocidade));
        }

        if (Gdx.input.justTouched()) {
            float mouseX = Gdx.input.getX();
            float mouseY = Config.ALTURA_TELA - Gdx.input.getY();

            if (mouseX >= 350 && mouseX <= 980 && mouseY >= 25 && mouseY <= 710) {
                int andarClicado = (int) ((mouseY - Config.Y_BASE_ANDAR_0) / Config.ESPACAMENTO_ANDAR);
                if (andarClicado >= 0 && andarClicado < Config.TOTAL_ANDARES) {
                    gerador.criarPassageiroNoAndar(andarClicado);
                }
            }
        }
    }

    private void desenharGeometria() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Painel lateral a direita: plano, solido, simples e sem firula
        shapeRenderer.setColor(0.12f, 0.12f, 0.14f, 0.95f);
        shapeRenderer.rect(Config.PAINEL_BG_X, Config.PAINEL_BG_Y, Config.PAINEL_BG_LARGURA, Config.PAINEL_BG_ALTURA);

        // Interior da cabine do elevador
        float elevX = elevador.getPosicaoX();
        float elevY = elevador.getPosicaoY();
        shapeRenderer.setColor(0.78f, 0.78f, 0.80f, 1.0f);
        shapeRenderer.rect(elevX + Config.CABINE_OFFSET_X, elevY + Config.CABINE_OFFSET_Y, Config.CABINE_LARGURA, Config.CABINE_ALTURA);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void desenharSpritesETextos() {
        batch.begin();

        float elevX = elevador.getPosicaoX();
        float elevY = elevador.getPosicaoY();
        float elevW = elevador.getLargura();
        float elevH = elevador.getAltura();
        float abertura = elevador.getAberturaPorta();

        // 1. DESENHA OS PASSAGEIROS DENTRO DA CABINE
        List<Passageiro> aBordo = elevador.getCopiaPassageirosABordo();
        for (Passageiro p : aBordo) {
            int spriteIdx = Math.max(0, Math.min(5, p.getSpriteIndex() - 1));
            batch.draw(texPessoas[spriteIdx], p.getPosX(), p.getPosY(), Config.PASSAGEIRO_CABINE_LARGURA, Config.PASSAGEIRO_CABINE_ALTURA);
            fontPequena.setColor(Color.WHITE);
            fontPequena.draw(batch, Config.TEXTO_DESTINO_PREFIXO + p.getAndarDestino(),
                    p.getPosX() + Config.TEXTO_DESTINO_CABINE_OFFSET_X,
                    p.getPosY() + Config.TEXTO_DESTINO_CABINE_OFFSET_Y);
        }

        // 2. DESENHA AS PORTAS INDIVIDUAIS DESLIZANDO (dimensoes e posicoes separadas do elevador)
        float portaBaseX = elevX + Config.PORTA_OFFSET_X;
        float portaBaseY = elevY + Config.PORTA_OFFSET_Y;
        float portaW = Config.PORTA_LARGURA;
        float portaH = Config.PORTA_ALTURA;
        float slide = Config.PORTA_SLIDE_MAX * abertura;

        batch.draw(regPortaL, portaBaseX + Config.PORTA_ESQ_OFFSET_X - slide, portaBaseY, portaW, portaH);
        batch.draw(regPortaR, portaBaseX + Config.PORTA_DIR_OFFSET_X + slide, portaBaseY, portaW, portaH);

        // 3. DESENHA A MOLDURA EXTERNA DO ELEVADOR (TOPO E LATERAIS)
        batch.draw(regElevadorTopo, elevX, elevY + Config.MOLDURA_TOPO_OFFSET_Y, Config.MOLDURA_TOPO_LARGURA, Config.MOLDURA_TOPO_ALTURA);
        batch.draw(regElevadorEsq, elevX, elevY, Config.MOLDURA_ESQ_LARGURA, Config.MOLDURA_ESQ_ALTURA);
        batch.draw(regElevadorDir, elevX + Config.MOLDURA_DIR_OFFSET_X, elevY, Config.MOLDURA_DIR_LARGURA, Config.MOLDURA_DIR_ALTURA);

        // 4. DESENHA OS PASSAGEIROS NAS FILAS DOS ANDARES
        for (int i = 0; i < Config.TOTAL_ANDARES; i++) {
            List<Passageiro> fila = predio.getPassageirosNaFila(i);
            for (Passageiro p : fila) {
                int spriteIdx = Math.max(0, Math.min(5, p.getSpriteIndex() - 1));
                float bobY = (p.getEstado() == Passageiro.Estado.EMBARCANDO || p.getEstado() == Passageiro.Estado.DESEMBARCANDO)
                        ? (float) Math.sin(tempoAnimacao * 14) * 3f
                        : 0f;

                batch.draw(texPessoas[spriteIdx], p.getPosX(), p.getPosY() + bobY, Config.PASSAGEIRO_LARGURA, Config.PASSAGEIRO_ALTURA);

                // Destino do passageiro em texto simples, mesma cor
                fontPequena.setColor(Color.WHITE);
                fontPequena.draw(batch, Config.TEXTO_DESTINO_PREFIXO + p.getAndarDestino(),
                        p.getPosX() + Config.TEXTO_DESTINO_OFFSET_X,
                        p.getPosY() + Config.TEXTO_DESTINO_OFFSET_Y);
            }
        }

        // 5. PASSAGEIROS DESEMBARCANDO / CONCLUIDOS
        List<Passageiro> concluidos = predio.getCopiaPassageirosConcluidos();
        for (Passageiro p : concluidos) {
            if (p.getPosX() < Config.SAIDA_CORREDOR_X) {
                int spriteIdx = Math.max(0, Math.min(5, p.getSpriteIndex() - 1));
                float bobY = (p.getEstado() == Passageiro.Estado.DESEMBARCANDO)
                        ? (float) Math.sin(tempoAnimacao * 14) * 3f
                        : 0f;
                batch.draw(texPessoas[spriteIdx], p.getPosX(), p.getPosY() + bobY, Config.PASSAGEIRO_LARGURA, Config.PASSAGEIRO_ALTURA);
            }
        }

        // 6. PAINEL LATERAL A DIREITA (SEPARADO EM CATEGORIAS, MESMA COR BRANCA, SEM FIRULA)
        desenharPainelLateral();

        batch.end();
    }

    private void desenharPainelLateral() {
        float x = Config.PAINEL_TEXTO_X;
        float y = Config.PAINEL_TEXTO_Y_INICIAL;

        // Titulo
        fontCategoria.setColor(Color.WHITE);
        fontCategoria.draw(batch, Config.TITULO_APLICACAO, x, y);
        y -= Config.ESPACAMENTO_ENTRE_CATEGORIAS;

        // CATEGORIA: ELEVADOR
        fontCategoria.setColor(Color.WHITE);
        fontCategoria.draw(batch, Config.CATEGORIA_ELEVADOR, x, y);
        y -= Config.ESPACAMENTO_TITULO_CATEGORIA;

        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, Config.TEXTO_ANDAR_ATUAL + elevador.getAndarAtual(), x, y);
        y -= Config.ESPACAMENTO_LINHA_TEXTO;

        fontPadrao.draw(batch, Config.TEXTO_ESTADO_PORTA + elevador.getEstadoPorta(), x, y);
        y -= Config.ESPACAMENTO_LINHA_TEXTO;

        fontPadrao.draw(batch, Config.TEXTO_DIRECAO + elevador.getDirecao(), x, y);
        y -= Config.ESPACAMENTO_ENTRE_CATEGORIAS;

        // CATEGORIA: PASSAGEIROS
        fontCategoria.setColor(Color.WHITE);
        fontCategoria.draw(batch, Config.CATEGORIA_PASSAGEIROS, x, y);
        y -= Config.ESPACAMENTO_TITULO_CATEGORIA;

        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, Config.TEXTO_PASSAGEIROS + elevador.getQuantidadePassageiros() + " / " + elevador.getCapacidade(), x, y);
        y -= Config.ESPACAMENTO_LINHA_TEXTO;

        fontPadrao.draw(batch, Config.TEXTO_TRANSPORTADOS + elevador.getTotalPassageirosTransportados(), x, y);
        y -= Config.ESPACAMENTO_LINHA_TEXTO;

        fontPadrao.draw(batch, Config.TEXTO_FILA_PREDIO + predio.getTotalPassageirosEmEspera(), x, y);
        y -= Config.ESPACAMENTO_LINHA_TEXTO;

        fontPadrao.draw(batch, Config.TEXTO_LIMITE_POR_ANDAR + Config.LIMITE_PASSAGEIROS_POR_ANDAR, x, y);
        y -= Config.ESPACAMENTO_ENTRE_CATEGORIAS;

        // CATEGORIA: ATALHOS
        fontCategoria.setColor(Color.WHITE);
        fontCategoria.draw(batch, Config.CATEGORIA_ATALHOS, x, y);
        y -= Config.ESPACAMENTO_TITULO_CATEGORIA;

        fontPadrao.setColor(Color.WHITE);
        fontPadrao.draw(batch, Config.ATALHO_ESPACO, x, y);
        y -= Config.ESPACAMENTO_LINHA_TEXTO;

        fontPadrao.draw(batch, Config.ATALHO_NUMEROS, x, y);
        y -= Config.ESPACAMENTO_LINHA_TEXTO;

        fontPadrao.draw(batch, Config.ATALHO_SPAWN, x, y);
        y -= Config.ESPACAMENTO_LINHA_TEXTO;

        fontPadrao.draw(batch, Config.ATALHO_PAUSAR, x, y);
        y -= Config.ESPACAMENTO_LINHA_TEXTO;

        fontPadrao.draw(batch, Config.ATALHO_VELOCIDADE, x, y);
    }

    @Override
    public void dispose() {
        if (gerador != null) gerador.parar();
        if (predio != null) predio.encerrarSimulacao();

        batch.dispose();
        shapeRenderer.dispose();
        fontPadrao.dispose();
        fontCategoria.dispose();
        fontPequena.dispose();

        texFundo.dispose();
        texElevador.dispose();
        if (texPortaL != null) texPortaL.dispose();
        if (texPortaR != null) texPortaR.dispose();
        for (Texture t : texPessoas) {
            if (t != null) t.dispose();
        }
    }
}
