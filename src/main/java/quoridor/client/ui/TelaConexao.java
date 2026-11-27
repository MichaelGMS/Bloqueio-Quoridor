package quoridor.client.ui;

import quoridor.shared.JogadaInvalidaException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.UnknownHostException;
import java.util.concurrent.ExecutionException;

public class TelaConexao extends JFrame {

    @FunctionalInterface
    public interface AcaoConectar {
        void conectar(String host, int porta, String nome) throws Exception;
    }

    private final JTextField campoHost = new JTextField(System.getProperty("quoridor.host", "localhost"), 16);
    private final JTextField campoPorta = new JTextField("1099", 6);
    private final JTextField campoNome = new JTextField(System.getProperty("user.name", "Jogador"), 16);
    private final JButton botao = new JButton("Conectar");

    public TelaConexao(AcaoConectar acao) {
        super("Quoridor - Conectar ao servidor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        adicionarLinha(painel, c, 0, "IP do servidor:", campoHost);
        adicionarLinha(painel, c, 1, "Porta do registry:", campoPorta);
        adicionarLinha(painel, c, 2, "Seu nome:", campoNome);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        painel.add(botao, c);

        botao.addActionListener(e -> iniciarConexao(acao));
        getRootPane().setDefaultButton(botao);

        add(painel);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private static void adicionarLinha(JPanel p, GridBagConstraints c, int linha, String rotulo, JTextField campo) {
        c.gridx = 0;
        c.gridy = linha;
        p.add(new JLabel(rotulo), c);
        c.gridx = 1;
        p.add(campo, c);
    }

    private void iniciarConexao(AcaoConectar acao) {
        String host = campoHost.getText().trim();
        String nome = campoNome.getText().trim();
        int porta;
        try {
            porta = Integer.parseInt(campoPorta.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Porta inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (host.isEmpty() || nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha o IP e o nome.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        botao.setEnabled(false);
        botao.setText("Conectando...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                acao.conectar(host, porta, nome);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dispose(); // conectou: a TelaJogo assume
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable causa = ex instanceof ExecutionException ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(TelaConexao.this, explicar(causa, host, porta),
                            "Não foi possível conectar", JOptionPane.ERROR_MESSAGE);
                    botao.setEnabled(true);
                    botao.setText("Conectar");
                }
            }
        }.execute();
    }

    private static String explicar(Throwable t, String host, int porta) {
        if (t instanceof JogadaInvalidaException) {
            return t.getMessage();
        }
        if (t instanceof UnknownHostException) {
            return "Endereço desconhecido: " + host;
        }
        if (t instanceof ConnectException) {
            return "Sem resposta em " + host + ":" + porta + ".\n"
                    + "Confira se o servidor está rodando, se o IP está certo\n"
                    + "e se o firewall libera as portas 1099 e 1100.";
        }
        if (t instanceof NotBoundException) {
            return "O registry respondeu, mas o serviço Quoridor não está registrado.";
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}