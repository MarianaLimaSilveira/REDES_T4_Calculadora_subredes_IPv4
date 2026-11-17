/*
 * Calculadora de sub-redes IPv4 (TINF - Trabalho T4)
 *
 * Recebe um endereço IPv4 e uma máscara (/20 ou 255.255.240.0) e mostra
 * as informações da rede. Todos os cálculos são feitos com operações de bits;
 * não usamos nenhuma biblioteca de rede (como java.net.InetAddress).
 *
 * Ideia central: um IPv4 é um número de 32 bits. Exemplo:
 *     172.16.45.200 -> 10101100.00010000.00101101.11001000
 *
 * Usamos "long" (64 bits) porque o "int" do Java tem sinal e
 * não guarda bem números de 32 bits como 255.255.255.255.
 *
 * Uso:
 *     java Calculadora                      (modo interativo, pergunta os dados)
 *     java Calculadora 172.16.45.200 /20    (uma consulta direta)
 */

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Calculadora {

    // 32 bits todos em 1 (usado para "limitar" os números a 32 bits)
    static final long TODOS_UNS = 0xFFFFFFFFL;

    // Guarda o resultado dos cálculos. Rede e broadcast ficam null quando "não há".
    static class Resultado {
        long ip;
        int prefixo;
        long mascara;
        Long rede;
        Long broadcast;
        long hosts;
        long primeiro;
        long ultimo;
    }

    // -----------------------------------------------------------------------
    // CONVERSÕES
    // -----------------------------------------------------------------------

    /** Converte "172.16.45.200" em um número de 32 bits.
     *  Lança IllegalArgumentException se o formato for inválido. */
    static long ipParaInteiro(String texto) {
        // split com -1 mantém partes vazias (assim "1.2.3." é detectado como erro)
        String[] partes = texto.trim().split("\\.", -1);

        // Precisa ter exatamente 4 octetos (192.168.1 é inválido)
        if (partes.length != 4) {
            throw new IllegalArgumentException("endereço deve ter 4 octetos separados por ponto");
        }

        long numero = 0;
        for (String parte : partes) {
            // Cada octeto deve conter só dígitos (rejeita vazio, letras, sinais)
            if (!parte.matches("\\d+")) {
                throw new IllegalArgumentException("octeto inválido: '" + parte + "'");
            }
            // Mais de 3 dígitos já passa de 255 (e evita estourar o parseInt)
            if (parte.length() > 3 || Integer.parseInt(parte) > 255) {
                throw new IllegalArgumentException("octeto fora do intervalo 0-255: " + parte);
            }
            int octeto = Integer.parseInt(parte);
            // Desloca o que já temos 8 bits para a esquerda e encaixa o novo octeto
            numero = (numero << 8) | octeto;
        }
        return numero;
    }

    /** Converte um número de 32 bits em "172.16.45.200". */
    static String inteiroParaIp(long numero) {
        // Move o octeto desejado para o final e isola os 8 bits com & 255
        return ((numero >> 24) & 255) + "." + ((numero >> 16) & 255) + "."
             + ((numero >> 8) & 255) + "." + (numero & 255);
    }

    /** Converte em binário com 32 bits, separado em 4 grupos de 8. */
    static String paraBinario(long numero) {
        // Completa com zeros à esquerda até ter 32 dígitos
        String bits = String.format("%32s", Long.toBinaryString(numero)).replace(' ', '0');
        return bits.substring(0, 8) + "." + bits.substring(8, 16) + "."
             + bits.substring(16, 24) + "." + bits.substring(24, 32);
    }

    // -----------------------------------------------------------------------
    // MÁSCARA
    // -----------------------------------------------------------------------

    /** Prefixo /20 -> máscara com 20 bits 1 seguidos de 12 bits 0.
     *  Exemplo: /20 = 11111111.11111111.11110000.00000000 */
    static long prefixoParaMascara(int prefixo) {
        return (TODOS_UNS << (32 - prefixo)) & TODOS_UNS;
    }

    /** Aceita "/20", "20" ou "255.255.240.0" e devolve o prefixo (0 a 32).
     *  Lança IllegalArgumentException se a máscara for inválida. */
    static int lerMascara(String texto) {
        texto = texto.trim();

        // Caso 1: notação de prefixo (/20 ou apenas 20)
        if (texto.startsWith("/") || texto.matches("\\d+")) {
            String numero = texto.startsWith("/") ? texto.substring(1) : texto;
            if (!numero.matches("\\d+")) {
                throw new IllegalArgumentException("prefixo inválido: '" + texto + "'");
            }
            if (numero.length() > 2 || Integer.parseInt(numero) > 32) {
                throw new IllegalArgumentException("prefixo fora do intervalo 0-32: " + numero);
            }
            return Integer.parseInt(numero);
        }

        // Caso 2: notação decimal pontuada (255.255.240.0)
        long mascara = ipParaInteiro(texto);

        // Uma máscara válida tem os bits 1 seguidos (contíguos) e depois só 0.
        // Invertendo os bits, sobra algo como 000...0111...1 (um número 2^k - 1).
        // Um número 2^k - 1 AND (ele + 1) é sempre 0. Se não for 0, tem "buraco".
        long invertida = ~mascara & TODOS_UNS;
        if ((invertida & (invertida + 1)) != 0) {
            throw new IllegalArgumentException("máscara inválida: os bits 1 não são contíguos");
        }

        // Prefixo = 32 menos a quantidade de bits de host (os zeros da máscara)
        int bitsDeHost = 64 - Long.numberOfLeadingZeros(invertida);
        return 32 - bitsDeHost;
    }

    // -----------------------------------------------------------------------
    // CÁLCULO PRINCIPAL
    // -----------------------------------------------------------------------

    /** Recebe o IP (número de 32 bits) e o prefixo e devolve todos os resultados. */
    static Resultado calcular(long ip, int prefixo) {
        Resultado r = new Resultado();
        r.ip = ip;
        r.prefixo = prefixo;
        r.mascara = prefixoParaMascara(prefixo);

        // Rede = IP AND máscara (zera os bits de host)
        long rede = ip & r.mascara;
        // Broadcast = rede OR (máscara invertida) (coloca 1 em todos os bits de host)
        long broadcast = rede | (~r.mascara & TODOS_UNS);

        if (prefixo == 32) {
            // /32: host único, não há rede nem broadcast
            r.hosts = 1;
            r.primeiro = ip;
            r.ultimo = ip;
        } else if (prefixo == 31) {
            // /31: enlace ponto a ponto (RFC 3021), 2 hosts, sem rede nem broadcast
            r.hosts = 2;
            r.primeiro = rede;
            r.ultimo = broadcast;
        } else {
            // Caso normal: tira 2 endereços (o da rede e o de broadcast)
            r.rede = rede;
            r.broadcast = broadcast;
            r.hosts = (1L << (32 - prefixo)) - 2;
            r.primeiro = rede + 1;
            r.ultimo = broadcast - 1;
        }
        return r;
    }

    /** Imprime o resultado no terminal. */
    static void mostrar(Resultado r) {
        String rede = (r.rede != null) ? inteiroParaIp(r.rede) : "não há";
        String broadcast = (r.broadcast != null) ? inteiroParaIp(r.broadcast) : "não há";

        // Se só existe 1 endereço, a faixa é ele mesmo
        String faixa = (r.primeiro == r.ultimo)
                ? inteiroParaIp(r.primeiro)
                : inteiroParaIp(r.primeiro) + " a " + inteiroParaIp(r.ultimo);

        System.out.println();
        System.out.println("Endereço IP (binário) : " + paraBinario(r.ip));
        System.out.println("Máscara (prefixo)     : /" + r.prefixo);
        System.out.println("Máscara (decimal)     : " + inteiroParaIp(r.mascara));
        System.out.println("Máscara (binário)     : " + paraBinario(r.mascara));
        System.out.println("Endereço da rede      : " + rede);
        System.out.println("Endereço de broadcast : " + broadcast);
        System.out.println("Quantidade de hosts   : " + r.hosts);
        System.out.println("Faixa de hosts        : " + faixa);
        System.out.println();
    }

    /** Valida as entradas, calcula e mostra. Devolve true se deu certo. */
    static boolean consultar(String textoIp, String textoMascara) {
        try {
            long ip = ipParaInteiro(textoIp);
            int prefixo = lerMascara(textoMascara);
            mostrar(calcular(ip, prefixo));
            return true;
        } catch (IllegalArgumentException erro) {
            // Entrada inválida: mostra a mensagem e NÃO encerra o programa
            System.out.println("\nErro: " + erro.getMessage() + "\n");
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // INTERFACE NO TERMINAL
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        // Força a saída em UTF-8 para os acentos aparecerem certos em qualquer terminal/Docker
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        // Modo direto: java Calculadora 172.16.45.200 /20
        if (args.length == 2) {
            System.exit(consultar(args[0], args[1]) ? 0 : 1);
        }
        if (args.length != 0) {
            System.out.println("Uso: java Calculadora [IP] [MASCARA]");
            System.exit(1);
        }

        // Modo interativo: pergunta, calcula e repete até o usuário sair
        Scanner teclado = new Scanner(System.in);
        System.out.println("==== Calculadora de sub-redes IPv4 =====");
        System.out.println("Máscara: /20 ou 255.255.240.0   |   Digite 'X' para encerrar");
        while (true) {
            System.out.print("\nEndereço IPv4: ");
            // hasNextLine é falso quando a entrada acaba (Ctrl+D): encerra com educação
            if (!teclado.hasNextLine()) break;
            String textoIp = teclado.nextLine();
            if (textoIp.trim().equalsIgnoreCase("X")) break;

            System.out.print("Máscara: ");
            if (!teclado.hasNextLine()) break;
            String textoMascara = teclado.nextLine();
            if (textoMascara.trim().equalsIgnoreCase("X")) break;

            consultar(textoIp, textoMascara);
        }
        System.out.println("\nAté mais!");
    }
}













//ideai do professor, passar os ip pra binário primeiro pq fica mais fácil