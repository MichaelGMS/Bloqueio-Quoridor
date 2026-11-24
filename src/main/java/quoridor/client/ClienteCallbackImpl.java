package quoridor.client;

import quoridor.client.ui.TelaJogo;
import quoridor.shared.ClienteCallback;
import quoridor.shared.EstadoJogo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallback {

    private static final long serialVersionUID = 1L;

    private TelaJogo tela;
    private EstadoJogo pendente;      // estado que chegou antes de a tela existir
    private String mensagemPendente;

    /** Porta em que o servidor vai chamar este cliente. Com 0, o Java escolhe uma porta livre. */
    public ClienteCallbackImpl(int porta) throws RemoteException {
        super(porta);
    }

    /** A tela só existe depois de conectarJogador devolver o id; então ela é ligada depois. */
    public synchronized void setTela(TelaJogo tela) {
        this.tela = tela;
        if (pendente != null) {
            tela.atualizar(pendente);
            pendente = null;
        }
        if (mensagemPendente != null) {
            tela.mostrarMensagem(mensagemPendente);
            mensagemPendente = null;
        }
    }

    @Override
    public synchronized void estadoAtualizado(EstadoJogo estado) throws RemoteException {
        if (tela == null) {
            pendente = estado;
        } else {
            tela.atualizar(estado);
        }
    }

    @Override
    public synchronized void mensagem(String texto) throws RemoteException {
        if (tela == null) {
            mensagemPendente = texto;
        } else {
            tela.mostrarMensagem(texto);
        }
    }
}