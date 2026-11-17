/*
 * Testes automáticos com os casos do PDF.
 * Compile e rode: javac -encoding UTF-8 *.java && java Testes
 */
public class Testes {

    // {ip, máscara, rede, broadcast, hosts, primeiro, último}  (null = "não há")
    static final String[][] CASOS = {
        {"192.168.1.10",   "255.255.255.0",   "192.168.1.0",    "192.168.1.255",   "254",      "192.168.1.1",    "192.168.1.254"},
        {"10.20.30.40",    "255.0.0.0",       "10.0.0.0",       "10.255.255.255",  "16777214", "10.0.0.1",       "10.255.255.254"},
        {"200.135.80.130", "255.255.255.224", "200.135.80.128", "200.135.80.159",  "30",       "200.135.80.129", "200.135.80.158"},
        {"172.31.5.9",     "255.255.254.0",   "172.31.4.0",     "172.31.5.255",    "510",      "172.31.4.1",     "172.31.5.254"},
        {"192.168.100.5",  "255.255.255.254", null,             null,              "2",        "192.168.100.4",  "192.168.100.5"},
        {"8.8.8.8",        "255.255.255.255", null,             null,              "1",        "8.8.8.8",        "8.8.8.8"},
        {"172.16.45.200",  "/20",             "172.16.32.0",    "172.16.47.255",   "4094",     "172.16.32.1",    "172.16.47.254"},
    };

    // Pares {ip, máscara} que devem ser recusados
    static final String[][] INVALIDOS = {
        {"300.10.0.1", "/24"}, {"192.168.1", "/24"},
        {"10.0.0.1", "/33"},   {"10.0.0.1", "255.0.255.0"},
    };

    static String ipOuNull(Long n) {
        return (n == null) ? null : Calculadora.inteiroParaIp(n);
    }

    public static void main(String[] args) {
        for (String[] c : CASOS) {
            Calculadora.Resultado r = Calculadora.calcular(
                    Calculadora.ipParaInteiro(c[0]), Calculadora.lerMascara(c[1]));

            boolean ok = java.util.Objects.equals(ipOuNull(r.rede), c[2])
                    && java.util.Objects.equals(ipOuNull(r.broadcast), c[3])
                    && String.valueOf(r.hosts).equals(c[4])
                    && Calculadora.inteiroParaIp(r.primeiro).equals(c[5])
                    && Calculadora.inteiroParaIp(r.ultimo).equals(c[6]);

            if (!ok) throw new RuntimeException("FALHOU: " + c[0] + " " + c[1]);
            System.out.println("OK  " + c[0] + " " + c[1]);
        }

        for (String[] c : INVALIDOS) {
            try {
                Calculadora.lerMascara(c[1]);
                Calculadora.ipParaInteiro(c[0]);
                throw new RuntimeException("deveria recusar: " + c[0] + " " + c[1]);
            } catch (IllegalArgumentException esperado) {
                System.out.println("OK  recusou " + c[0] + " " + c[1]);
            }
        }
        System.out.println("\nTodos os testes passaram!");
    }
}
