package quoridor.shared;

import java.io.Serializable;
import java.util.Objects;

public final class Barreira implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int linha;
    private final int coluna;
    private final Orientacao orientacao;
    private final int dono;

    public Barreira(int linha, int coluna, Orientacao orientacao) {
        this(linha, coluna, orientacao, -1);
    }

    public Barreira(int linha, int coluna, Orientacao orientacao, int dono) {
        this.linha = linha;
        this.coluna = coluna;
        this.orientacao = orientacao;
        this.dono = dono;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    public Orientacao getOrientacao() {
        return orientacao;
    }

    public int getDono() {
        return dono;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Barreira)) return false;
        Barreira b = (Barreira) o;
        return linha == b.linha && coluna == b.coluna && orientacao == b.orientacao;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linha, coluna, orientacao);
    }

    @Override
    public String toString() {
        return orientacao + "@(" + linha + "," + coluna + ")";
    }
}