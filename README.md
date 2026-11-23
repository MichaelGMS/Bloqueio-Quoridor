# Bloqueio-Quoridor

Versão evoluída do projeto Quoridor (Quoridor-master), feita para a disciplina de
Evolução de Software.

- Java 21 e Maven
- Cliente e servidor com Java RMI (dois jogadores em rede)
- Interface em Swing

## Como rodar

Compilar:

    mvn compile

Servidor (portas padrão: registry 1099, objeto remoto 1100):

    java -cp target/classes quoridor.server.ServidorApp

Cliente (um por jogador):

    java -cp target/classes quoridor.client.ClienteApp

Para testar dois clientes no mesmo computador, use `-Dquoridor.janela=true` e portas
de callback diferentes (`-Dquoridor.portaCallback=1101`, `1102`).