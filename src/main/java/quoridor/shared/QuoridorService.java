package quoridor.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface QuoridorService extends Remote {

    String NOME = "QuoridorService";

    int conectarJogador(String nome, ClienteCallback callback)
            throws RemoteException, JogadaInvalidaException;

    void moverPeao(int jogador, int linha, int coluna)
            throws RemoteException, JogadaInvalidaException;

    void colocarBarreira(int jogador, int linha, int coluna, Orientacao orientacao)
            throws RemoteException, JogadaInvalidaException;


    void jogarNovamente(int jogador) throws RemoteException, JogadaInvalidaException;

    EstadoJogo obterEstadoDoJogo() throws RemoteException;

    void desconectar(int jogador) throws RemoteException;
}