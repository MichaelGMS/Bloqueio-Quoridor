package quoridor.client.ui;

import quoridor.shared.EstadoJogo;
import quoridor.shared.JogadaInvalidaException;
import quoridor.shared.Orientacao;
import quoridor.shared.QuoridorService;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

public class TelaJogo extends JFrame implements TabuleiroPanel.Listener {

    @FunctionalInterface
    private interface Acao {
        void executar() throws Exception;
    }

    private final QuoridorService servico;
    private final int meuId;
    private final TabuleiroPanel tabuleiro;

    private long ultimaVersao = -1;
    private boolean telaCheia = false;

    public TelaJogo(QuoridorService servico, int meuId, String nome) {
        super("Quoridor - " + nome + " (Jogador " + (meuId + 1) + ")");
        this.servico = servico;
        this.meuId = meuId;

        tabuleiro = new TabuleiroPanel(meuId);
        tabuleiro.setListener(this);
        setContentPane(tabuleiro);

        tabuleiro.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "telaCheia");
        tabuleiro.getActionMap().put("telaCheia", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alternarTelaCheia();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sair();
            }
        });

        setMinimumSize(new Dimension(900, 700));
        setSize(1200, 850);
        setLocationRelativeTo(null);
        // Para testar dois clientes no mesmo PC, use a VM option -Dquoridor.janela=true
        if (!Boolean.getBoolean("quoridor.janela")) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    /** Chamado pelo callback do cliente; pode vir de qualquer thread. */
    public void atualizar(EstadoJogo e) {
        SwingUtilities.invokeLater(() -> {
            if (e.getVersao() < ultimaVersao) return; // estado velho, ignora
            ultimaVersao = e.getVersao();
            tabuleiro.setEstado(e);
        });
    }

    /** Chamado pelo callback para avisos do servidor. */
    public void mostrarMensagem(String texto) {
        SwingUtilities.invokeLater(() -> tabuleiro.setMensagem(texto));
    }

    // ---------- ações do jogador ----------

    @Override
    public void aoMover(int linha, int coluna) {
        executar(() -> servico.moverPeao(meuId, linha, coluna));
    }

    @Override
    public void aoColocarBarreira(int linha, int coluna, Orientacao orientacao) {
        executar(() -> servico.colocarBarreira(meuId, linha, coluna, orientacao));
    }

    @Override
    public void aoJogarNovamente() {
        executar(() -> servico.jogarNovamente(meuId));
    }

    @Override
    public void aoPedirRegras() {
        new TelaRegras(this).setVisible(true);
    }

    /** A chamada remota roda fora da thread da tela para não travar a interface. */
    private void executar(Acao acao) {
        tabuleiro.setMensagem(null);
        new Thread(() -> {
            try {
                acao.executar();
                atualizar(servico.obterEstadoDoJogo());
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> tratarErro(ex));
            }
        }).start();
    }

    private void tratarErro(Throwable causa) {
        if (causa instanceof JogadaInvalidaException) {
            tabuleiro.setMensagem(causa.getMessage());
        } else if (causa instanceof RemoteException) {
            JOptionPane.showMessageDialog(this,
                    "Conexão com o servidor perdida.\n" + causa.getMessage(),
                    "Erro de comunicação", JOptionPane.ERROR_MESSAGE);
        } else {
            tabuleiro.setMensagem("Erro: " + causa);
        }
    }

    private void alternarTelaCheia() {
        GraphicsDevice tela = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!telaCheia) {
            dispose();
            setUndecorated(true);
            tela.setFullScreenWindow(this);
            setVisible(true);
        } else {
            tela.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setVisible(true);
        }
        telaCheia = !telaCheia;
    }

    private void sair() {
        try {
            servico.desconectar(meuId);
        } catch (Exception ignorada) {
            // o servidor pode já ter caído; fecha mesmo assim
        }
        dispose();
        System.exit(0);
    }
}