package quoridor.client.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

public class TelaRegras extends JDialog {

    private static final Color FUNDO = new Color(0x0B2545);

    public TelaRegras(Window dono) {
        super(dono, "Como jogar", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JButton ok = new JButton("Entendi, vamos jogar!");
        ok.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        ok.addActionListener(e -> dispose());

        JPanel rodape = new JPanel();
        rodape.setBackground(FUNDO);
        rodape.setBorder(BorderFactory.createEmptyBorder(8, 0, 16, 0));
        rodape.add(ok);

        add(new PainelRegras(), BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);

        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private static class PainelRegras extends JPanel {

        private static final int MARGEM = 28;
        private static final int GAP = 20;
        private static final int CARD_W = 261;
        private static final int CARD_Y = 96;
        private static final int CARD_H = 450;

        // mini tabuleiro
        private static final int CEL = 20;
        private static final int VAO = 4;
        private static final int PASSO = CEL + VAO;
        private static final int LADO = 9 * CEL + 8 * VAO;

        private static final Color[] PEAO = {new Color(0x2563EB), new Color(0xDC2626)};
        private static final Color[] CLARA = {new Color(0x60A5FA), new Color(0xF87171)};
        private static final Color CASA = new Color(0xE5C9A0);

        private static final String[] TITULOS = {
                "1  Posição inicial", "2  Mover ou bloquear", "3  Contornar o bloqueio", "4  Saltar e vencer"
        };
        private static final String[] TEXTOS = {
                "Cada jogador começa no centro de um lado do tabuleiro e tem 10 barreiras. "
                        + "O objetivo é chegar a qualquer casa do lado oposto.",
                "Na sua vez, mova o peão uma casa ou coloque uma barreira. "
                        + "O vermelho tenta atrapalhar o azul bloqueando o caminho dele.",
                "Barreiras não saem do lugar, mas o peão pode contorná-las. "
                        + "É proibido fechar por completo o caminho de qualquer jogador.",
                "Peões lado a lado? Pule por cima do oponente. "
                        + "Quem chegar primeiro ao lado oposto vence a partida!"
        };

        // peões: {jogador, linha, coluna}
        private static final int[][][] PEOES = {
                {{0, 8, 4}, {1, 0, 4}},
                {{0, 5, 4}, {1, 0, 4}},
                {{0, 5, 5}, {1, 0, 4}},
                {{0, 3, 5}, {1, 2, 5}}
        };
        // barreiras: {dono, linha, coluna, orientacao (0 = horizontal, 1 = vertical)}
        private static final int[][][] BARREIRAS = {
                {},
                {{1, 4, 3, 0}},
                {{1, 4, 3, 0}},
                {}
        };
        // setas: {linha1, coluna1, linha2, coluna2}
        private static final int[][][] SETAS = {
                {},
                {{5, 4, 4, 4}},
                {{5, 4, 5, 5}, {5, 5, 2, 5}},
                {{3, 5, 1, 5}}
        };

        PainelRegras() {
            setBackground(FUNDO);
            setPreferredSize(new Dimension(2 * MARGEM + 4 * CARD_W + 3 * GAP, 560));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            g.drawString("Como funciona uma partida de BLOQUEIO?", MARGEM, 48);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g.setColor(new Color(0xBFDBFE));
            g.drawString("Na sua vez, você pode mover seu peão ou colocar uma barreira.", MARGEM, 74);

            for (int i = 0; i < 4; i++) {
                int cx = MARGEM + i * (CARD_W + GAP);

                g.setColor(new Color(0x12355F));
                g.fillRoundRect(cx, CARD_Y, CARD_W, CARD_H, 20, 20);

                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                g.drawString(TITULOS[i], cx + 16, CARD_Y + 34);

                desenharMini(g, cx + (CARD_W - LADO) / 2, CARD_Y + 62, PEOES[i], BARREIRAS[i], SETAS[i]);

                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                g.setColor(new Color(0xD1D5DB));
                paragrafo(g, TEXTOS[i], cx + 16, CARD_Y + 62 + LADO + 38, CARD_W - 32);
            }
            g.dispose();
        }

        private void desenharMini(Graphics2D g, int x, int y, int[][] peoes, int[][] barreiras, int[][] setas) {
            // moldura e faixas de objetivo
            g.setColor(new Color(0x374151));
            g.fillRoundRect(x - 8, y - 8, LADO + 16, LADO + 16, 14, 14);
            g.setColor(PEAO[0]);
            g.fillRoundRect(x, y - 6, LADO, 4, 4, 4);
            g.setColor(PEAO[1]);
            g.fillRoundRect(x, y + LADO + 2, LADO, 4, 4, 4);

            // casas
            g.setColor(CASA);
            for (int l = 0; l < 9; l++) {
                for (int c = 0; c < 9; c++) {
                    g.fillRoundRect(x + c * PASSO, y + l * PASSO, CEL, CEL, 5, 5);
                }
            }

            // barreiras
            g.setStroke(new BasicStroke(1.2f));
            for (int[] b : barreiras) {
                boolean horizontal = b[3] == 0;
                int bx = x + b[2] * PASSO;
                int by = y + b[1] * PASSO;
                int rx = horizontal ? bx : bx + CEL;
                int ry = horizontal ? by + CEL : by;
                int rw = horizontal ? 2 * CEL + VAO : VAO;
                int rh = horizontal ? VAO : 2 * CEL + VAO;
                g.setColor(CLARA[b[0]]);
                g.fillRoundRect(rx, ry, rw, rh, 4, 4);
                g.setColor(PEAO[b[0]]);
                g.drawRoundRect(rx, ry, rw, rh, 4, 4);
            }

            // peões
            for (int[] p : peoes) {
                int px = x + p[2] * PASSO + 3;
                int py = y + p[1] * PASSO + 3;
                g.setColor(PEAO[p[0]]);
                g.fillOval(px, py, 14, 14);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval(px, py, 14, 14);
            }

            // setas por cima de tudo
            for (int[] s : setas) {
                seta(g,
                        x + s[1] * PASSO + CEL / 2.0, y + s[0] * PASSO + CEL / 2.0,
                        x + s[3] * PASSO + CEL / 2.0, y + s[2] * PASSO + CEL / 2.0);
            }
        }

        private void seta(Graphics2D g, double x1, double y1, double x2, double y2) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double comp = Math.hypot(dx, dy);
            double ux = dx / comp;
            double uy = dy / comp;

            double ax = x1 + ux * 10;
            double ay = y1 + uy * 10;
            double bx = x2 - ux * 12;
            double by = y2 - uy * 12;

            g.setColor(new Color(0xFACC15));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(ax, ay, bx, by));

            Path2D.Double ponta = new Path2D.Double();
            ponta.moveTo(bx + ux * 10, by + uy * 10);
            ponta.lineTo(bx - uy * 6, by + ux * 6);
            ponta.lineTo(bx + uy * 6, by - ux * 6);
            ponta.closePath();
            g.fill(ponta);
        }

        private void paragrafo(Graphics2D g, String texto, int x, int y, int larguraMax) {
            FontMetrics fm = g.getFontMetrics();
            StringBuilder linha = new StringBuilder();
            for (String palavra : texto.split(" ")) {
                String teste = linha.length() == 0 ? palavra : linha + " " + palavra;
                if (fm.stringWidth(teste) > larguraMax && linha.length() > 0) {
                    g.drawString(linha.toString(), x, y);
                    y += fm.getHeight();
                    linha = new StringBuilder(palavra);
                } else {
                    linha = new StringBuilder(teste);
                }
            }
            if (linha.length() > 0) g.drawString(linha.toString(), x, y);
        }
    }
}