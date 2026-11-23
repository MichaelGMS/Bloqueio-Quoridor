package quoridor.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClienteCallback extends Remote {

    void estadoAtualizado(EstadoJogo estado) throws RemoteException;

    void mensagem(String texto) throws RemoteException;
}