package quoridor.server;

import quoridor.shared.ClienteCallback;
import quoridor.shared.EstadoJogo;
import quoridor.shared.JogadaInvalidaException;
import quoridor.shared.Orientacao;
import quoridor.shared.QuoridorService;
import quoridor.shared.StatusPartida;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class QuoridorServiceImpl extends UnicastRemoteObject implements QuoridorService {

    private static final long serialVersionUID = 1L;

    private interface Jogada {
        void aplicar() throws JogadaInvalidaException;
    }

    // O estado abaixo só é lido ou alterado com a trava adquirida.
    private final ReentrantLock trava = new ReentrantLock(true);
    private final Partida partida = new Partida();
    private final ClienteCallback[] callbacks = new ClienteCallback[2];
    private final String[] nomes = new String[2];

    // Uma thread só envia as notificações, em ordem e fora da trava.
    private final ExecutorService notificador = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "notificador");
        t.setDaemon(true);
        return t;
    });

    public QuoridorServiceImpl(int portaObjeto) throws RemoteException {
        super(portaObjeto);
    }

    // ---------- métodos remotos ----------

    @Override
    public int conectarJogador(String nome, ClienteCallback callback)
            throws RemoteException, JogadaInvalidaException {
        if (nome == null || nome.isBlank() || callback == null) {
            throw new JogadaInvalidaException("Nome ou callback inválido.");
        }

        int id;
        String mensagem = null;
        EstadoJogo estado;
        ClienteCallback[] destinos;

        trava.lock();
        try {
            if (partida.getStatus() == StatusPartida.FINALIZADA) {
                throw new JogadaInvalidaException("A partida anterior terminou. Tente de novo em instantes.");
            }
            id = callbacks[0] == null ? 0 : (callbacks[1] == null ? 1 : -1);
            if (id < 0) {
                throw new JogadaInvalidaException("A partida já tem dois jogadores.");
            }
            callbacks[id] = callback;
            nomes[id] = nome.trim();

            if (callbacks[0] != null && callbacks[1] != null) {
                partida.iniciar();
                mensagem = "Partida iniciada: " + nomes[0] + " (azul) x " + nomes[1] + " (vermelho).";
            }
            estado = partida.snapshot();
            destinos = callbacks.clone();
        } finally {
            trava.unlock();
        }

        publicar(estado, destinos, mensagem);
        return id;
    }

    @Override
    public void moverPeao(int jogador, int linha, int coluna)
            throws RemoteException, JogadaInvalidaException {
        jogar(jogador, () -> partida.mover(jogador, linha, coluna));
    }

    @Override
    public void colocarBarreira(int jogador, int linha, int coluna, Orientacao orientacao)
            throws RemoteException, JogadaInvalidaException {
        jogar(jogador, () -> partida.colocarBarreira(jogador, linha, coluna, orientacao));
    }

    @Override
    public void jogarNovamente(int jogador) throws RemoteException, JogadaInvalidaException {
        jogar(jogador, () -> partida.pedirRevanche(jogador, callbacks[1 - jogador] != null));
    }

    @Override
    public EstadoJogo obterEstadoDoJogo() throws RemoteException {
        trava.lock();
        try {
            return partida.snapshot();
        } finally {
            trava.unlock();
        }
    }

    @Override
    public void desconectar(int jogador) throws RemoteException {
        removerJogador(jogador, null);
    }

    // ---------- apoio interno ----------

    /** Valida o jogador, aplica a ação dentro da trava e avisa os clientes depois de soltá-la. */
    private void jogar(int jogador, Jogada jogada) throws JogadaInvalidaException {
        EstadoJogo estado;
        ClienteCallback[] destinos;

        trava.lock();
        try {
            if (jogador < 0 || jogador > 1 || callbacks[jogador] == null) {
                throw new JogadaInvalidaException("Jogador não conectado.");
            }
            jogada.aplicar();
            estado = partida.snapshot();
            destinos = callbacks.clone();
        } finally {
            trava.unlock();
        }
        publicar(estado, destinos, null);
    }

    /**
     * Remove um jogador. Se "somenteSe" não for nulo, só remove quando o callback
     * registrado for exatamente aquele (evita remover quem reconectou).
     */
    private void removerJogador(int jogador, ClienteCallback somenteSe) {
        String mensagem = null;
        EstadoJogo estado;
        ClienteCallback[] destinos;

        trava.lock();
        try {
            if (jogador < 0 || jogador > 1 || callbacks[jogador] == null) return;
            if (somenteSe != null && callbacks[jogador] != somenteSe) return;

            String nome = nomes[jogador];
            callbacks[jogador] = null;
            nomes[jogador] = null;
            partida.cancelarRevanche(); // quem saiu não pode mais aceitar a revanche

            if (partida.getStatus() == StatusPartida.EM_ANDAMENTO) {
                partida.finalizarPorDesistencia(1 - jogador);
                mensagem = nome + " desconectou. Você venceu por W.O.";
            }
            if (callbacks[0] == null && callbacks[1] == null) {
                partida.reiniciar(); // sala vazia: começa uma partida nova
                return;
            }
            estado = partida.snapshot();
            destinos = callbacks.clone();
        } finally {
            trava.unlock();
        }
        publicar(estado, destinos, mensagem);
    }

    /** Agenda o envio dos callbacks. Nunca é chamado com a trava presa. */
    private void publicar(EstadoJogo estado, ClienteCallback[] destinos, String mensagem) {
        notificador.submit(() -> {
            for (int i = 0; i < destinos.length; i++) {
                ClienteCallback cb = destinos[i];
                if (cb == null) continue;
                try {
                    cb.estadoAtualizado(estado);
                    if (mensagem != null) cb.mensagem(mensagem);
                } catch (RemoteException e) {
                    System.err.println("Cliente " + i + " inalcançável: " + e.getMessage());
                    removerJogador(i, cb);
                }
            }
        });
    }
}