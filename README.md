# 🎤 Maria Mole Karaoke System

O **Maria Mole Karaoke** é uma solução digital completa para a gestão de pedidos de músicas em bares e eventos. O sistema permite que a experiência de karaokê seja totalmente integrada, desde a solicitação da música pelo cliente até a reprodução automática no palco.

O projeto utiliza a **API do YouTube (Google)** como motor de busca e reprodução, garantindo um catálogo vasto e sempre atualizado.

---

## 🚀 Tecnologias

### **Backend**
* **Java 17 & Spring Boot 3:** Estrutura robusta para processamento de pedidos e lógica de negócio.
* **Spring Data JPA:** Camada de persistência para comunicação com o banco de dados.
* **PostgreSQL:** Banco de dados relacional para armazenamento de utilizadores, histórico e filas.
* **Google API (YouTube):** Integração direta para busca e recuperação de faixas de vídeo.

### **Frontend**
* **Angular:** Framework para uma SPA (Single Page Application) rápida e responsiva.
* **Less:** Estilização avançada com foco em interfaces modernas e dinâmicas.
* **Reactive Forms:** Gestão de entradas de dados com validações em tempo real.

---

## 🖥️ Estrutura do Sistema

O sistema está dividido em três pilares principais:

### **1. Tela do Usuário (Cliente)**
* **Login Simplificado:** Acesso rápido apenas com Nome, E-mail e Telefone.
* **Busca de Músicas:** Interface integrada com a API do YouTube para encontrar faixas.
* **Solicitação de Pedidos:** Envio imediato para a fila do karaokê.
* **Reconhecimento Inteligente:** Identificação de utilizadores recorrentes via endereço IP e similaridade de nomes.

### **2. Tela Administrativa (ADM)**
* **Gestão da Fila:** Controle total sobre as músicas pendentes, permitindo reordenar ou remover pedidos.
* **Controle de Usuários:** Visualização e gestão dos clientes ativos no sistema.
* **Monitorização:** Acesso aos logs de atividade e estatísticas de uso.

### **3. Reprodutor (Player)**
* **Interface de Palco:** Reprodutor dedicado para a exibição dos vídeos e letras.
* **Sincronização Automática:** Atualização em tempo real conforme novos pedidos são aprovados ou a fila avança.

---

## 💡 Funcionalidades de Destaque

* **Integração com Google API:** Busca e carregamento de vídeos diretamente do YouTube.
* **Regras de Fila:** Impede que um mesmo utilizador monopolize a lista de reprodução, garantindo a rotatividade do palco.
* **Experiência Mobile-First:** Interface pensada para ser utilizada em smartphones, facilitando o uso pelos clientes nas mesas do bar.
* **Logs Estruturados:** Monitorização contínua de pedidos e erros de comunicação com APIs externas.

---

## 📝 Monitorização Técnica

O sistema utiliza logs padronizados para acompanhar o fluxo de operação:
* `[LOGIN]`: Registo de entradas e identificação de utilizadores.
* `[FILA]`: Logs de inclusão, remoção e alteração de estado dos pedidos.
* `[API-GOOGLE]`: Monitorização de chamadas e respostas da integração com o YouTube.
