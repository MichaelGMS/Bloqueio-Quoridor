package quoridor.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Posicao[] peoes;
    private final int[] barreirasRestantes;
    private final List<Barreira> barreiras;
    private final boolean[] querRevanche;
    private final int jogadorDaVez;
    private final StatusPartida status;
    private final int vencedor;
    private final long versao;

    public EstadoJogo(Posicao[] peoes, int[] barreirasRestantes, List<Barreira> barreiras, boolean[] querRevanche, int jogadorDaVez, StatusPartida status, int vencedor, long versao) {
        this.peoes = peoes.clone();
        this.barreirasRestantes = barreirasRestantes.clone();
        this.barreiras = Collections.unmodifiableList(new ArrayList<>(barreiras));
        this.querRevanche = querRevanche.clone();
        this.jogadorDaVez = jogadorDaVez;
        this.status = status;
        this.vencedor = vencedor;
        this.versao = versao;
    }

    public Posicao getPosicaoPeao(int jogador) {
        return peoes[jogador];
    }

    public int getBarreirasRestantes(int jogador) {
        return barreirasRestantes[jogador];
    }

    public List<Barreira> getBarreiras() {
        return barreiras;
    }

    public boolean getQuerRevanche(int jogador) {
        return querRevanche[jogador];
    }

    public int getJogadorDaVez() {
        return jogadorDaVez;
    }

    public StatusPartida getStatus() {
        return status;
    }

    public int getVencedor() {
        return vencedor;
    }

    public long getVersao() {
        return versao;
    }
}