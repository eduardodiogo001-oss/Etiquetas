# 🏷️ Etiquetas de Validade — App Android

App Android para geração e impressão de etiquetas térmicas de validade de alimentos, com integração direta com impressoras **Elgin** (L42, L42 Pro e similares).

---

## 📱 Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| **Banco de dados de produtos** | Cadastro com nome, dias de validade e categoria |
| **Cálculo automático** | Soma a data de hoje + dias de validade do produto |
| **Busca rápida** | Filtra produtos por nome em tempo real |
| **Prévia da etiqueta** | Mostra abertura e validade antes de imprimir |
| **Seletor de cópias** | NumberPicker de 1 a 50 cópias |
| **Impressão Elgin** | Via Wi-Fi (TCP/IP), Bluetooth ou USB |
| **Produtos pré-cadastrados** | 25 itens comuns já inclusos na primeira abertura |
| **CRUD completo** | Adicionar, editar, excluir produtos |

---

## 🚀 Como abrir no Android Studio

1. Abra o **Android Studio** (Hedgehog ou superior)
2. **File → Open** → selecione a pasta `EtiquetaValidade`
3. Aguarde o Gradle sincronizar
4. Conecte seu dispositivo Android (Android 8.0+) via USB
5. Clique em **Run ▶**

---

## 🖨️ Configuração da Impressora Elgin

### Opção 1 — Wi-Fi / TCP-IP (recomendado)
1. No app, toque no ícone ⚙️ (canto superior direito)
2. Selecione **Wi-Fi / TCP-IP**
3. Informe o **IP** da impressora (ex: `192.168.1.100`) e a **porta** (padrão `9100`)
4. Ajuste o tamanho da etiqueta em mm
5. Salve

> **Como descobrir o IP da Elgin:** Segure o botão FEED da impressora por 3–5 segundos ao ligar; ela imprime um auto-teste com o IP da rede.

### Opção 2 — Bluetooth
1. Pareie a impressora Elgin com o celular nas **Configurações do Android**
2. No app → ⚙️ → Bluetooth → **Listar dispositivos pareados**
3. Selecione a impressora

### Opção 3 — USB
1. Conecte a impressora via cabo USB-OTG
2. No app → ⚙️ → USB
3. Na primeira impressão, o Android pedirá permissão — conceda

---

## 📦 SDK Elgin (opcional — para recursos avançados)

O app usa **comandos ZPL II** diretamente, que são compatíveis com as impressoras Elgin L42/L42 Pro.

Se a Elgin fornecer o arquivo `.aar` do SDK próprio deles:
1. Coloque o arquivo `ElginPrinter.aar` em `app/libs/`
2. No `app/build.gradle`, descomente a linha:
   ```groovy
   implementation(name: 'ElginPrintSDK', ext: 'aar')
   ```
3. Substitua os métodos de impressão em `ElginPrintManager.kt` pelas chamadas do SDK

---

## 🗄️ Banco de Dados

O app usa **Room (SQLite)** local. Produtos pré-cadastrados na primeira abertura:

| Produto | Dias |
|---|---|
| Ketchup | 7 |
| Maionese | 5 |
| Frango cozido | 3 |
| Arroz cozido | 3 |
| Queijo fatiado | 5 |
| ... e mais 20 | — |

---

## 📐 Estrutura de Arquivos

```
EtiquetaValidade/
├── app/src/main/
│   ├── java/com/etiqueta/validade/
│   │   ├── data/
│   │   │   ├── Produto.kt          ← Entidade Room
│   │   │   ├── ProdutoDao.kt       ← Queries SQL
│   │   │   ├── AppDatabase.kt      ← Banco + pré-cadastro
│   │   │   └── ProdutoRepository.kt
│   │   ├── print/
│   │   │   └── ElginPrintManager.kt ← Toda lógica de impressão ZPL
│   │   └── ui/
│   │       ├── MainActivity.kt     ← Tela principal (lista + busca)
│   │       ├── MainViewModel.kt    ← Lógica de negócio
│   │       ├── ProdutoAdapter.kt   ← RecyclerView
│   │       ├── CadastroProdutoActivity.kt
│   │       └── ConfiguracaoActivity.kt ← IP / BT / USB / tamanho etiqueta
│   └── res/
│       ├── layout/                 ← Todos os XMLs de tela
│       ├── values/                 ← Cores, strings, temas
│       └── xml/device_filter.xml   ← Filtro USB Elgin
```

---

## 🔧 Personalizar o Layout da Etiqueta

Edite o método `buildZpl()` em `ElginPrintManager.kt`.

Referência rápida ZPL II:
- `^FO x,y` → posição
- `^A0N,h,w` → fonte e tamanho
- `^FD texto ^FS` → texto
- `^GB w,h,t` → linha/retângulo
- `^PW` → largura em dots (203 dpi: 1mm = ~8 dots)
- `^LL` → comprimento em dots

---

## 📋 Permissões Android

| Permissão | Para que |
|---|---|
| `BLUETOOTH_CONNECT` | Conectar impressora BT |
| `BLUETOOTH_SCAN` | Listar dispositivos |
| `INTERNET` | Impressão via TCP/IP |
| `USB_PERMISSION` | Impressão via USB |

---

## ❓ Problemas comuns

**"Nenhuma impressora USB encontrada"** → Verifique se o cabo suporta USB Host (OTG). Conceda permissão quando o Android perguntar.

**Erro TCP/IP** → Confirme que o celular e a impressora estão na mesma rede Wi-Fi. Verifique o IP no auto-teste da impressora.

**Etiqueta não imprime no tamanho certo** → Ajuste Largura e Altura em mm nas configurações do app de acordo com o rolo de etiqueta instalado.
