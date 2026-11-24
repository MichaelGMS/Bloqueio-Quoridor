package quoridor.client;

import quoridor.client.ui.TelaConexao;
import quoridor.client.ui.TelaJogo;
import quoridor.client.ui.TelaRegras;
import quoridor.shared.EstadoJogo;
import quoridor.shared.QuoridorService;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClienteApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // segue com o visual padrão
            }
            new TelaRegras(null).setVisible(true);                  // modal: só segue depois de fechar
            new TelaConexao(ClienteApp::conectar).setVisible(true);
        });
    }

    /** Roda fora da thread da tela (dentro de um SwingWorker da TelaConexao). */
    private static void conectar(String host, int porta, String nome) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, porta);
        QuoridorService servico = (QuoridorService) registry.lookup(QuoridorService.NOME);

        // Porta fixa do callback, informada pela VM option -Dquoridor.portaCallback=1101.
        // Sem a opção, vale 0 (qualquer porta livre).
        int portaCallback = Integer.getInteger("quoridor.portaCallback", 0);
        ClienteCallbackImpl callback = new ClienteCallbackImpl(portaCallback);

        int id = servico.conectarJogador(nome, callback);
        EstadoJogo inicial = servico.obterEstadoDoJogo();

        SwingUtilities.invokeLater(() -> {
            TelaJogo tela = new TelaJogo(servico, id, nome);
            callback.setTela(tela);
            tela.atualizar(inicial);
            tela.setVisible(true);
        });
    }
}