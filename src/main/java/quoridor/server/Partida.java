package quoridor.server;

import quoridor.shared.Barreira;
import quoridor.shared.EstadoJogo;
import quoridor.shared.JogadaInvalidaException;
import quoridor.shared.Orientacao;
import quoridor.shared.Posicao;
import quoridor.shared.StatusPartida;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class Partida {

    public static final int TAMANHO = 9;
    public static final int BARREIRAS_INICIAIS = 10;

    private static final int[][] DIRECOES = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final Posicao[] peoes = new Posicao[2];
    private final Set<Barreira> barreiras = new LinkedHashSet<>();
    private final int[] restantes = new int[2];
    private final boolean[] revanche = new boolean[2];
    private int vez;
    private StatusPartida status;
    private int vencedor;
    private long versao;

    public Partida() {
        reiniciar();
    }


    public void reiniciar() {
        peoes[0] = new Posicao(8, 4); // jogador 0 sobe até a linha 0
        peoes[1] = new Posicao(0, 4); // jogador 1 desce até a linha 8
        barreiras.clear();
        restantes[0] = BARREIRAS_INICIAIS;
        restantes[1] = BARREIRAS_INICIAIS;
        revanche[0] = false;
        revanche[1] = false;
        vez = 0;
        status = StatusPartida.AGUARDANDO;
        vencedor = -1;
        versao++;
    }

    public void iniciar() {
        status = StatusPartida.EM_ANDAMENTO;
        versao++;
    }

    public void finalizarPorDesistencia(int jogadorVencedor) {
        status = StatusPartida.FINALIZADA;
        vencedor = jogadorVencedor;
        versao++;
    }

    public StatusPartida getStatus() {
        return status;
    }

    public EstadoJogo snapshot() {
        return new EstadoJogo(peoes, restantes, new ArrayList<>(barreiras), revanche,
                vez, status, vencedor, versao);
    }

    public void pedirRevanche(int jogador, boolean oponenteConectado) throws JogadaInvalidaException {
        if (status != StatusPartida.FINALIZADA) {
            throw new JogadaInvalidaException("A partida ainda não terminou.");
        }
        if (!oponenteConectado) {
            reiniciar();
            return;
        }
        revanche[jogador] = true;
        if (revanche[0] && revanche[1]) {
            reiniciar();
            iniciar();
        } else {
            versao++;
        }
    }

    public void cancelarRevanche() {
        revanche[0] = false;
        revanche[1] = false;
        versao++;
    }

    // jogadas

    public void mover(int jogador, int linha, int coluna) throws JogadaInvalidaException {
        validarVez(jogador);
        Posicao destino = new Posicao(linha, coluna);
        if (!movimentosLegais(jogador).contains(destino)) {
            throw new JogadaInvalidaException("Movimento inválido.");
        }

        peoes[jogador] = destino;
        if (linha == linhaAlvo(jogador)) {
            status = StatusPartida.FINALIZADA;
            vencedor = jogador;
        } else {
            vez = 1 - jogador;
        }
        versao++;
    }

    public void colocarBarreira(int jogador, int linha, int coluna, Orientacao orientacao)
            throws JogadaInvalidaException {
        validarVez(jogador);
        if (restantes[jogador] <= 0) {
            throw new JogadaInvalidaException("Você não tem mais barreiras.");
        }
        if (orientacao == null || linha < 0 || linha > TAMANHO - 2 || coluna < 0 || coluna > TAMANHO - 2) {
            throw new JogadaInvalidaException("Barreira fora do tabuleiro.");
        }

        Barreira nova = new Barreira(linha, coluna, orientacao, jogador);
        if (conflita(nova)) {
            throw new JogadaInvalidaException("A barreira se sobrepõe ou cruza outra barreira.");
        }

        barreiras.add(nova);
        if (!temCaminho(0) || !temCaminho(1)) {
            barreiras.remove(nova);
            throw new JogadaInvalidaException("Essa barreira fecharia todos os caminhos de um jogador.");
        }

        restantes[jogador]--;
        vez = 1 - jogador;
        versao++;
    }

    // regras

    private void validarVez(int jogador) throws JogadaInvalidaException {
        if (status != StatusPartida.EM_ANDAMENTO) {
            throw new JogadaInvalidaException("A partida não está em andamento.");
        }
        if (jogador != vez) {
            throw new JogadaInvalidaException("Não é a sua vez.");
        }
    }

    private static int linhaAlvo(int jogador) {
        return jogador == 0 ? 0 : TAMANHO - 1;
    }

    private static boolean dentro(int linha, int coluna) {
        return linha >= 0 && linha < TAMANHO && coluna >= 0 && coluna < TAMANHO;
    }

    private boolean tem(int linha, int coluna, Orientacao orientacao) {
        return barreiras.contains(new Barreira(linha, coluna, orientacao));
    }

    private boolean bloqueado(int l1, int c1, int l2, int c2) {
        if (c1 == c2) { // andando na vertical
            int l = Math.min(l1, l2);
            return tem(l, c1, Orientacao.HORIZONTAL) || tem(l, c1 - 1, Orientacao.HORIZONTAL);
        }
        int c = Math.min(c1, c2); // andando na horizontal
        return tem(l1, c, Orientacao.VERTICAL) || tem(l1 - 1, c, Orientacao.VERTICAL);
    }

    private boolean conflita(Barreira nova) {
        int l = nova.getLinha();
        int c = nova.getColuna();
        if (nova.getOrientacao() == Orientacao.HORIZONTAL) {
            return tem(l, c, Orientacao.HORIZONTAL) || tem(l, c, Orientacao.VERTICAL)
                    || tem(l, c - 1, Orientacao.HORIZONTAL) || tem(l, c + 1, Orientacao.HORIZONTAL);
        }
        return tem(l, c, Orientacao.VERTICAL) || tem(l, c, Orientacao.HORIZONTAL)
                || tem(l - 1, c, Orientacao.VERTICAL) || tem(l + 1, c, Orientacao.VERTICAL);
    }

    private boolean podeIr(Posicao de, Posicao para) {
        return dentro(para.getLinha(), para.getColuna())
                && !bloqueado(de.getLinha(), de.getColuna(), para.getLinha(), para.getColuna());
    }

    private Set<Posicao> movimentosLegais(int jogador) {
        Set<Posicao> legais = new HashSet<>();
        Posicao eu = peoes[jogador];
        Posicao outro = peoes[1 - jogador];

        for (int[] d : DIRECOES) {
            Posicao frente = new Posicao(eu.getLinha() + d[0], eu.getColuna() + d[1]);
            if (!podeIr(eu, frente)) continue;

            if (!frente.equals(outro)) {
                legais.add(frente);
                continue;
            }

            Posicao salto = new Posicao(frente.getLinha() + d[0], frente.getColuna() + d[1]);
            if (podeIr(frente, salto)) {
                legais.add(salto);
                continue;
            }
            for (int[] p : DIRECOES) {
                if (p[0] * d[0] + p[1] * d[1] != 0) continue;
                Posicao lado = new Posicao(frente.getLinha() + p[0], frente.getColuna() + p[1]);
                if (podeIr(frente, lado)) legais.add(lado);
            }
        }
        return legais;
    }

    private boolean temCaminho(int jogador) {
        Posicao inicio = peoes[jogador];
        int alvo = linhaAlvo(jogador);

        boolean[][] visitada = new boolean[TAMANHO][TAMANHO];
        ArrayDeque<int[]> fila = new ArrayDeque<>();
        fila.add(new int[]{inicio.getLinha(), inicio.getColuna()});
        visitada[inicio.getLinha()][inicio.getColuna()] = true;

        while (!fila.isEmpty()) {
            int[] atual = fila.poll();
            if (atual[0] == alvo) return true;

            for (int[] d : DIRECOES) {
                int nl = atual[0] + d[0];
                int nc = atual[1] + d[1];
                if (dentro(nl, nc) && !visitada[nl][nc] && !bloqueado(atual[0], atual[1], nl, nc)) {
                    visitada[nl][nc] = true;
                    fila.add(new int[]{nl, nc});
                }
            }
        }
        return false;
    }
}