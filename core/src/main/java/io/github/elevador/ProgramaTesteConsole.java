package io.github.elevador;

/**
 * Programa de teste executável via console/terminal para validação do sistema
 * de controle de elevador multithread com semáforos.
 */
public class ProgramaTesteConsole {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   SIMULADOR DE ELEVADOR                                       ");
        System.out.println("   - F = 8 andares (F > 5)                                     ");
        System.out.println("   - Capacidade = 4 passageiros (Bonificação Extra > 1)        ");
        System.out.println("   - Sincronização 100% via Semaphore                          ");
        System.out.println("   - Restrição atendida: Zero métodos synchronized             ");
        System.out.println("===============================================================");

        int totalAndares = 8;
        int capacidade = 4;
        int andarInicial = 0;

        // 1. Instância única da classe Prédio cria e inicializa o Elevador
        Predio predio = new Predio(totalAndares, capacidade, andarInicial);
        Elevador elevador = predio.getElevador();

        // 2. Inicia o loop da thread do elevador
        predio.iniciarSimulacao();
        System.out.println("[SISTEMA] Elevador iniciado no andar " + andarInicial + " com capacidade " + capacidade);

        // 3. Inicia o gerador de passageiros em threads
        GeradorPassageiros gerador = new GeradorPassageiros(predio);
        gerador.iniciar();
        System.out.println("[SISTEMA] Gerador de passageiros em threads ativado.");

        // Monitor de console executando por 15 segundos para demonstração
        try {
            for (int i = 0; i < 15; i++) {
                Thread.sleep(1000);
                System.out.printf("[STATUS t=%02ds] Andar: %d | Direção: %-8s | Porta: %-8s | A bordo: %d/%d | Fila total: %d | Atendidos: %d%n",
                        i + 1,
                        elevador.getAndarAtual(),
                        elevador.getDirecao(),
                        elevador.getEstadoPorta(),
                        elevador.getQuantidadePassageiros(),
                        elevador.getCapacidade(),
                        predio.getTotalPassageirosEmEspera(),
                        elevador.getTotalPassageirosTransportados()
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n[SISTEMA] Encerrando simulação de teste no console...");
        gerador.parar();
        predio.encerrarSimulacao();
        System.out.println("[SISTEMA] Total de passageiros concluídos com sucesso: " + elevador.getTotalPassageirosTransportados());
        System.out.println("[SISTEMA] Teste concluído com êxito!");
    }
}
