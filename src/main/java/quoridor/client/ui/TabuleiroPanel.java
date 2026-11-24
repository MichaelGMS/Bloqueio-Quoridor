package quoridor.client.ui;

import quoridor.shared.Barreira;
import quoridor.shared.EstadoJogo;
import quoridor.shared.Orientacao;
import quoridor.shared.Posicao;
import quoridor.shared.StatusPartida;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TabuleiroPanel extends JPanel {

    public enum Modo { MOVER, BARREIRA_H, BARREIRA_V }

    public interface Listener {
        void aoMover(int linha, int coluna);
        void aoColocarBarreira(int linha, int coluna, Orientacao orientacao);
        void aoJogarNovamente();
        void aoPedirRegras();
    }

    private static final int N = 9;

    private static final Color FUNDO = new Color(0x111827);
    private static final Color DOCK = new Color(0x1F2937);
    private static final Color CASA = new Color(0xE5C9A0);
    private static final Color CASA_HOVER = new Color(0xF3DFB8);
    private static final Color CINZA = new Color(0x6B7280);
    private static final Color VERDE = new Color(0x4ADE80);
    private static final Color ROSA = new Color(0xFCA5A5);
    private static final Color[] PEAO = {new Color(0x2563EB), new Color(0xDC2626)};
    private static final Color[] BARREIRA = {new Color(0x60A5FA), new Color(0xF87171)};
    private static final String[] ROTULOS = {"Mover [1]", "Horizontal [2]", "Vertical [3]", "Regras [F1]"};

    private final int meuId;
    private final Timer temporizadorMensagem;

    private EstadoJogo estado;
    private Modo modo = Modo.MOVER;
    private Listener listener;
    private String mensagem;

    // geometria, recalculada a cada desenho porque a janela pode mudar de tamanho
    private int cel, vao, passo, lado, tabX, tabY, espessura, topoAltura, rodapeY, rodapeAltura;
    private final Rectangle[] botoes = new Rectangle[4];
    private Rectangle botaoRevanche = new Rectangle();

    private int mouseX = -1, mouseY = -1;
    private int hoverL = -1, hoverC = -1, hoverBotao = -1;
    private boolean hoverRevanche = false;

    public TabuleiroPanel(int meuId) {
        this.meuId = meuId;
        setBackground(FUNDO);
        setPreferredSize(new Dimension(1100, 800));

        temporizadorMensagem = new Timer(5000, e -> {
            mensagem = null;
            repaint();
        });
        temporizadorMensagem.setRepeats(false);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                calcularLayout();

                hoverBotao = fimDeJogo() ? -1 : botaoEm(mouseX, mouseY);
                hoverRevanche = fimDeJogo() && botaoRevanche.contains(mouseX, mouseY);
                boolean clicavel = hoverBotao >= 0 || (hoverRevanche && podePedirRevanche());
                setCursor(clicavel ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());

                calcularHover(mouseX, mouseY);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseX = -1;
                mouseY = -1;
                hoverBotao = -1;
                hoverRevanche = false;
                hoverL = -1;
                hoverC = -1;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                calcularLayout();

                if (fimDeJogo()) {
                    if (podePedirRevanche() && botaoRevanche.contains(mouseX, mouseY) && listener != null) {
                        listener.aoJogarNovamente();
                    }
                    return;
                }

                int botao = botaoEm(mouseX, mouseY);
                if (botao >= 0) {
                    acionarBotao(botao);
                    return;
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (modo != Modo.MOVER) girarBarreira();
                    return;
                }

                calcularHover(mouseX, mouseY);
                if (!minhaVez() || listener == null || hoverL < 0) return;

                if (modo == Modo.MOVER) {
                    listener.aoMover(hoverL, hoverC);
                } else {
                    Orientacao o = (modo == Modo.BARREIRA_H) ? Orientacao.HORIZONTAL : Orientacao.VERTICAL;
                    listener.aoColocarBarreira(hoverL, hoverC, o);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        atalho(KeyEvent.VK_1, () -> acionarBotao(0));
        atalho(KeyEvent.VK_2, () -> acionarBotao(1));
        atalho(KeyEvent.VK_3, () -> acionarBotao(2));
        atalho(KeyEvent.VK_F1, () -> acionarBotao(3));
        atalho(KeyEvent.VK_R, this::girarBarreira);
    }

    // ---------- usado pela TelaJogo ----------

    public void setListener(Listener listener) { this.listener = listener; }

    public void setEstado(EstadoJogo estado) {
        this.estado = estado;
        repaint();
    }

    /** Mostra um aviso que some sozinho em 5 segundos. Com null, limpa o aviso. */
    public void setMensagem(String texto) {
        if (texto == null || texto.isBlank()) {
            mensagem = null;
            temporizadorMensagem.stop();
        } else {
            mensagem = texto;
            temporizadorMensagem.restart();
        }
        repaint();
    }

    // ---------- interação ----------

    private void atalho(int tecla, Runnable acao) {
        String nome = "atalho-" + tecla;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla, 0), nome);
        getActionMap().put(nome, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acao.run();
            }
        });
    }

    private void acionarBotao(int i) {
        switch (i) {
            case 0: modo = Modo.MOVER; break;
            case 1: modo = Modo.BARREIRA_H; break;
            case 2: modo = Modo.BARREIRA_V; break;
            default:
                if (listener != null) listener.aoPedirRegras();
        }
        calcularHover(mouseX, mouseY);
        repaint();
    }

    private void girarBarreira() {
        modo = (modo == Modo.BARREIRA_H) ? Modo.BARREIRA_V : Modo.BARREIRA_H;
        calcularHover(mouseX, mouseY);
        repaint();
    }

    private boolean minhaVez() {
        return estado != null
                && estado.getStatus() == StatusPartida.EM_ANDAMENTO
                && estado.getJogadorDaVez() == meuId;
    }

    private boolean fimDeJogo() {
        return estado != null && estado.getStatus() == StatusPartida.FINALIZADA;
    }

    private boolean venci() {
        return fimDeJogo() && estado.getVencedor() == meuId;
    }

    private boolean podePedirRevanche() {
        return fimDeJogo() && !estado.getQuerRevanche(meuId);
    }

    private int botaoEm(int x, int y) {
        for (int i = 0; i < botoes.length; i++) {
            if (botoes[i] != null && botoes[i].contains(x, y)) return i;
        }
        return -1;
    }

    private void calcularHover(int x, int y) {
        int l = -1;
        int c = -1;
        if (x >= 0 && y >= 0) {
            if (modo == Modo.MOVER) {
                l = Math.floorDiv(y - tabY, passo);
                c = Math.floorDiv(x - tabX, passo);
                if (l < 0 || l >= N || c < 0 || c >= N) {
                    l = -1;
                    c = -1;
                }
            } else if (new Rectangle(tabX - passo / 2, tabY - passo / 2, lado + passo, lado + passo).contains(x, y)) {
                if (modo == Modo.BARREIRA_H) {
                    l = (int) Math.round((y - tabY + vao / 2.0) / passo) - 1;
                    c = Math.floorDiv(x - tabX, passo);
                } else {
                    l = Math.floorDiv(y - tabY, passo);
                    c = (int) Math.round((x - tabX + vao / 2.0) / passo) - 1;
                }
                l = limitar(l);
                c = limitar(c);
            }
        }
        hoverL = l;
        hoverC = c;
    }

    private int limitar(int v) { return Math.max(0, Math.min(N - 2, v)); }

    // ---------- geometria ----------

    private void calcularLayout() {
        int w = getWidth();
        int h = getHeight();

        topoAltura = Math.max(70, h / 10);
        rodapeAltura = Math.max(110, h / 8);
        int livreAltura = h - topoAltura - rodapeAltura - 24;
        int disponivel = Math.max(200, Math.min(w - 48, livreAltura));

        // lado = N*cel + (N-1)*vao, com vao = cel/5
        cel = Math.max(20, (int) (disponivel / (N + (N - 1) / 5.0)));
        vao = Math.max(4, cel / 5);
        passo = cel + vao;
        lado = N * cel + (N - 1) * vao;
        espessura = Math.max(4, vao / 2);

        tabX = (w - lado) / 2;
        tabY = topoAltura + (h - topoAltura - rodapeAltura - lado) / 2;

        rodapeY = h - rodapeAltura;
        int btn = Math.min(84, rodapeAltura - 44);
        int espaco = btn / 2;
        int x0 = (w - (4 * btn + 3 * espaco)) / 2;
        for (int i = 0; i < botoes.length; i++) {
            botoes[i] = new Rectangle(x0 + i * (btn + espaco), rodapeY + 8, btn, btn);
        }

        // botão "Jogar novamente", usado no lugar dos ícones quando a partida termina
        int larguraRevanche = Math.min(w - 48, 340);
        int alturaRevanche = Math.min(64, rodapeAltura - 50);
        botaoRevanche = new Rectangle((w - larguraRevanche) / 2,
                rodapeY + rodapeAltura - alturaRevanche - 18, larguraRevanche, alturaRevanche);
    }

    private Rectangle retangulo(int l, int c, Orientacao o) {
        int x = tabX + c * passo;
        int y = tabY + l * passo;
        return o == Orientacao.HORIZONTAL
                ? new Rectangle(x, y + cel, 2 * cel + vao, vao)
                : new Rectangle(x + cel, y, vao, 2 * cel + vao);
    }

    // ---------- desenho ----------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        calcularLayout();

        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        desenharTopo(g);
        desenharTabuleiro(g);
        desenharRodape(g);
        g.dispose();
    }

    private String textoStatus() {
        if (estado == null || estado.getStatus() == StatusPartida.AGUARDANDO) {
            return "Aguardando oponente...";
        }
        if (estado.getStatus() == StatusPartida.FINALIZADA) {
            return venci() ? "Você venceu!" : "Você perdeu.";
        }
        return estado.getJogadorDaVez() == meuId ? "Sua vez!" : "Vez do oponente";
    }

    private void desenharTopo(Graphics2D g) {
        int w = getWidth();
        int tamanho = Math.max(16, topoAltura / 4);
        int linhaY = (int) (topoAltura * 0.40);

        // o resultado da partida aparece maior e colorido
        Color corStatus = Color.WHITE;
        if (minhaVez() || venci()) {
            corStatus = VERDE;
        } else if (fimDeJogo()) {
            corStatus = ROSA;
        }
        g.setFont(fonte(Font.BOLD, tamanho + (fimDeJogo() ? 14 : 8)));
        g.setColor(corStatus);
        textoCentral(g, textoStatus(), w / 2, linhaY);

        if (estado != null) {
            g.setFont(fonte(Font.BOLD, tamanho));
            FontMetrics fm = g.getFontMetrics();
            int r = tamanho / 2;

            String meu = "Você: " + estado.getBarreirasRestantes(meuId) + " barreiras";
            desenharPeao(g, 28 + r, linhaY, r, PEAO[meuId]);
            g.setColor(Color.WHITE);
            textoEsquerda(g, meu, 28 + 2 * r + 10, linhaY);

            String outro = "Oponente: " + estado.getBarreirasRestantes(1 - meuId) + " barreiras";
            int xTexto = w - 28 - fm.stringWidth(outro);
            g.setColor(Color.WHITE);
            textoEsquerda(g, outro, xTexto, linhaY);
            desenharPeao(g, xTexto - 10 - r, linhaY, r, PEAO[1 - meuId]);
        }

        if (mensagem != null) {
            g.setFont(fonte(Font.BOLD, Math.max(14, tamanho - 2)));
            g.setColor(ROSA);
            textoCentral(g, mensagem, w / 2, (int) (topoAltura * 0.84));
        }
    }

    private void desenharTabuleiro(Graphics2D g) {
        // faixas de objetivo: o azul chega na linha de cima, o vermelho na de baixo
        g.setColor(PEAO[0]);
        g.fillRoundRect(tabX, tabY - espessura - 4, lado, espessura, espessura, espessura);
        g.setColor(PEAO[1]);
        g.fillRoundRect(tabX, tabY + lado + 4, lado, espessura, espessura, espessura);

        boolean destacarCasa = minhaVez() && modo == Modo.MOVER;
        int arco = Math.max(6, cel / 5);
        for (int l = 0; l < N; l++) {
            for (int c = 0; c < N; c++) {
                boolean destaque = destacarCasa && l == hoverL && c == hoverC;
                g.setColor(destaque ? CASA_HOVER : CASA);
                g.fillRoundRect(tabX + c * passo, tabY + l * passo, cel, cel, arco, arco);
            }
        }

        if (estado == null) return;

        // cada barreira usa a cor de quem a colocou
        g.setStroke(new BasicStroke(1.5f));
        for (Barreira b : estado.getBarreiras()) {
            Rectangle r = retangulo(b.getLinha(), b.getColuna(), b.getOrientacao());
            g.setColor(BARREIRA[b.getDono()]);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(PEAO[b.getDono()]);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }

        // prévia da barreira, na minha cor
        if (minhaVez() && modo != Modo.MOVER && hoverL >= 0 && estado.getBarreirasRestantes(meuId) > 0) {
            Orientacao o = (modo == Modo.BARREIRA_H) ? Orientacao.HORIZONTAL : Orientacao.VERTICAL;
            Rectangle r = retangulo(hoverL, hoverC, o);
            Color base = BARREIRA[meuId];
            g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 140));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }

        int raio = cel / 2 - Math.max(4, cel / 8);
        for (int j = 0; j < 2; j++) {
            Posicao p = estado.getPosicaoPeao(j);
            if (p == null) continue;
            int cx = tabX + p.getColuna() * passo + cel / 2;
            int cy = tabY + p.getLinha() * passo + cel / 2;
            desenharPeao(g, cx, cy, raio, PEAO[j]);
        }
    }

    private void desenharRodape(Graphics2D g) {
        g.setColor(DOCK);
        g.fillRect(0, rodapeY, getWidth(), rodapeAltura);

        if (fimDeJogo()) {
            desenharRevanche(g);
            return;
        }

        Color minha = PEAO[meuId];
        Color clara = BARREIRA[meuId];
        int restantes = estado == null ? 0 : estado.getBarreirasRestantes(meuId);

        for (int i = 0; i < botoes.length; i++) {
            Rectangle r = botoes[i];
            boolean selecionado = (i == 0 && modo == Modo.MOVER)
                    || (i == 1 && modo == Modo.BARREIRA_H)
                    || (i == 2 && modo == Modo.BARREIRA_V);

            g.setColor(selecionado ? new Color(0x4B5563)
                    : (i == hoverBotao ? new Color(0x374151) : new Color(0x2B3544)));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 18, 18);
            g.setStroke(new BasicStroke(selecionado ? 3.5f : 1.5f));
            g.setColor(selecionado ? clara : CINZA);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 18, 18);

            int cx = r.x + r.width / 2;
            int cy = r.y + r.height / 2;
            boolean semBarreiras = (i == 1 || i == 2) && restantes == 0;
            int comp = (int) (r.width * 0.62);
            int esp = Math.max(6, (int) (r.width * 0.14));

            switch (i) {
                case 0:
                    desenharPeao(g, cx, cy, r.width / 4, minha);
                    break;
                case 1:
                    g.setColor(semBarreiras ? CINZA : clara);
                    g.fillRoundRect(cx - comp / 2, cy - esp / 2, comp, esp, 6, 6);
                    break;
                case 2:
                    g.setColor(semBarreiras ? CINZA : clara);
                    g.fillRoundRect(cx - esp / 2, cy - comp / 2, esp, comp, 6, 6);
                    break;
                default:
                    g.setColor(Color.WHITE);
                    g.setFont(fonte(Font.BOLD, r.width / 2));
                    textoCentral(g, "?", cx, cy);
            }

            if (i == 1 || i == 2) {
                g.setFont(fonte(Font.BOLD, 13));
                g.setColor(Color.WHITE);
                String n = "×" + restantes;
                int largura = g.getFontMetrics().stringWidth(n);
                textoEsquerda(g, n, r.x + r.width - 8 - largura, r.y + r.height - 14);
            }

            g.setFont(fonte(Font.PLAIN, 14));
            g.setColor(new Color(0xD1D5DB));
            textoCentral(g, ROTULOS[i], cx, r.y + r.height + 16);
        }
    }

    /** Fim de jogo: no lugar dos ícones, aparece o botão para jogar de novo. */
    private void desenharRevanche(Graphics2D g) {
        boolean aguardando = estado.getQuerRevanche(meuId);

        if (!aguardando && estado.getQuerRevanche(1 - meuId)) {
            g.setFont(fonte(Font.BOLD, 16));
            g.setColor(VERDE);
            textoCentral(g, "O oponente quer jogar novamente!", getWidth() / 2, rodapeY + 16);
        }

        Rectangle r = botaoRevanche;
        g.setColor(aguardando ? new Color(0x4B5563)
                : (hoverRevanche ? BARREIRA[meuId] : PEAO[meuId]));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 22, 22);

        g.setColor(Color.WHITE);
        g.setFont(fonte(Font.BOLD, Math.max(16, r.height / 3)));
        textoCentral(g, aguardando ? "Aguardando oponente..." : "Jogar novamente",
                r.x + r.width / 2, r.y + r.height / 2);
    }

    private void desenharPeao(Graphics2D g, int cx, int cy, int raio, Color cor) {
        g.setColor(cor);
        g.fillOval(cx - raio, cy - raio, raio * 2, raio * 2);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(cx - raio, cy - raio, raio * 2, raio * 2);
    }

    private Font fonte(int estilo, int tamanho) {
        return new Font(Font.SANS_SERIF, estilo, tamanho);
    }

    private void textoCentral(Graphics2D g, String texto, int cx, int cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, cx - fm.stringWidth(texto) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
    }

    private void textoEsquerda(Graphics2D g, String texto, int x, int cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, x, cy + (fm.getAscent() - fm.getDescent()) / 2);
    }
}