# Imagem com o JDK (compilador + Java) na versão 17
FROM eclipse-temurin:17-jdk

# Pasta de trabalho dentro do container
WORKDIR /app

# Copia o código da aplicação
COPY Calculadora.java Testes.java ./

# Compila os arquivos .java (gera os .class)
RUN javac -encoding UTF-8 Calculadora.java Testes.java

# Ao rodar o container, executa a calculadora.
# Argumentos passados no "docker run" vão direto para o programa.
ENTRYPOINT ["java", "Calculadora"]
