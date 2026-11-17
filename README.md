# Calculadora de sub-redes IPv4 (Java)

Aplicação de terminal em Java (sem bibliotecas externas) que recebe um endereço
IPv4 e uma máscara e mostra: IP em binário, máscara (prefixo, decimal e binário),
endereço de rede, broadcast, quantidade de hosts e faixa de hosts.
Os cálculos são feitos com operações de bits, sem bibliotecas de rede.

Imagem no Docker Hub: `SEU_USUARIO/calc-subredes-java`

## Como executar (a partir do Docker Hub)

### 1. Pré-requisito

Ter o **Docker** instalado e aberto (no Windows/Mac, o Docker Desktop precisa estar
em execução). Confira com:

```bash
docker --version
```

Não é preciso ter Java instalado nem baixar o código: a imagem já contém tudo.

### 2. Modo interativo

A aplicação pergunta o IP e a máscara, mostra o resultado e repete.
Digite `sair` para encerrar.

```bash
docker run -it --rm SEU_USUARIO/calc-subredes-java
```

Na primeira vez, o Docker baixa a imagem do Docker Hub automaticamente.
O `-it` é obrigatório neste modo (permite digitar no terminal).

### 3. Consulta direta

Informe o IP e a máscara no próprio comando. A máscara pode ser em prefixo
ou em decimal:

```bash
docker run --rm SEU_USUARIO/calc-subredes-java 172.16.45.200 /20
docker run --rm SEU_USUARIO/calc-subredes-java 172.16.45.200 255.255.240.0
```

### Exemplo de saída

```
Endereço IP (binário) : 10101100.00010000.00101101.11001000
Máscara (prefixo)     : /20
Máscara (decimal)     : 255.255.240.0
Máscara (binário)     : 11111111.11111111.11110000.00000000
Endereço da rede      : 172.16.32.0
Endereço de broadcast : 172.16.47.255
Quantidade de hosts   : 4094
Faixa de hosts        : 172.16.32.1 a 172.16.47.254
```

### Entradas inválidas

São recusadas com mensagem de erro, sem encerrar a aplicação:
`300.10.0.1`, `192.168.1`, `/33`, `255.0.255.0`.

### Casos especiais

- `/31`: 2 hosts, sem endereço de rede nem de broadcast (RFC 3021)
- `/32`: host único, sem endereço de rede nem de broadcast

## Rodar os testes automáticos

Executa os casos de teste do enunciado dentro do container:

```bash
docker run --rm --entrypoint java SEU_USUARIO/calc-subredes-java Testes
```

Deve aparecer `Todos os testes passaram!`.

## Gerar e publicar a imagem (para quem for reproduzir)

Dentro da pasta do projeto:

```bash
docker build -t SEU_USUARIO/calc-subredes-java .
docker login
docker push SEU_USUARIO/calc-subredes-java
```

## Executar sem Docker (opcional)

Precisa do JDK 17 ou superior:

```bash
javac -encoding UTF-8 Calculadora.java Testes.java
java Calculadora
```

## Estrutura

- `Calculadora.java`: código da aplicação (comentado)
- `Testes.java`: testes com os casos do enunciado
- `Dockerfile`: compila o código e gera a imagem Docker
- `README.md`: este arquivo
