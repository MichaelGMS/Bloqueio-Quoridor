package quoridor.server;

import quoridor.shared.QuoridorService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorApp {

    public static void main(String[] args) throws Exception {
        int portaRegistry = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        int portaObjeto = args.length > 1 ? Integer.parseInt(args[1]) : 1100;

        QuoridorServiceImpl servico = new QuoridorServiceImpl(portaObjeto);
        Registry registry = LocateRegistry.createRegistry(portaRegistry);
        registry.rebind(QuoridorService.NOME, servico);

        System.out.println("Servidor Quoridor no ar. Registry: " + portaRegistry
                + " | objeto remoto: " + portaObjeto
                + " | hostname RMI: " + System.getProperty("java.rmi.server.hostname", "(padrão)"));
    }
}