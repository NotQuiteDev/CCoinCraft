# **CCC (Chan Coin Craft)**

### **Overview**
**CCC** is a planned Minecraft plugin designed to introduce a Bitcoin mining and trading system. Players will mine in-game blocks to earn Bitcoin (Satoshi), trade it with others, and interact with a server-wide economy. This project aims to bring dynamic and engaging economic gameplay to Minecraft servers.

---

### **Key Features**
- **Bitcoin Mining**:
  - Players can mine specific blocks (e.g., coal, iron, gold) to earn Bitcoin rewards.
  - Mining probabilities and rewards will be fully customizable via configuration files.

- **Trading System**:
  - Commands for buying, selling, and transferring Bitcoin between players.
  - Integration with VaultAPI to enable seamless currency exchanges with the server economy.

- **Real-Time Notifications**:
  - Mining results and transaction logs will be displayed instantly.
  - User-friendly messages to keep players informed.

- **Data Management**:
  - Transaction history, player balances, and administrative actions will be stored using SQLite.

---

### **Planned Features**
- **Customizable Economy**: Admins will have full control over mining probabilities, Bitcoin rewards, and in-game prices.
- **Discord Integration**:
  - Notifications for mining progress and transactions.
  - Real-time Bitcoin price updates directly in Discord channels.

---

### **Usage (Planned)**
1. Add the `CCoinCraft.jar` file to the `plugins` folder on your Minecraft server.
2. Restart the server to load the plugin.
3. Configure mining probabilities and trading rules in `config.yml`.
4. Enable players to mine, trade, and interact with the server economy.

---

### **Technical Details**
- **Minecraft Version**: Planned support for 1.13+ (testing up to 1.19).
- **Programming Language**: Java (using Gradle for dependency management).
- **Dependencies**:
  - VaultAPI for server economy integration.
  - SQLite for database management.

---

### **Development Plan**
- **Phase 1**: Develop core mining mechanics and reward systems.
- **Phase 2**: Implement trading commands and VaultAPI integration.
- **Phase 3**: Add data management with SQLite for transactions and balances.
- **Phase 4**: Extend functionality with Discord integration and advanced market features.

---

# **CCC (찬 코인 크래프트)**

### **개요** 🎮
**CCC**는 마인크래프트 서버에서 비트코인 채굴과 거래 시스템을 구현하는 플러그인 기획입니다. 플레이어가 광물을 채굴하여 비트코인을 획득하고, 이를 서버 경제와 통합된 거래 시스템에서 사용할 수 있도록 설계되었습니다.

---

### **주요 기능** ⚒️
- **비트코인 채굴**:
  - 특정 광물(예: 석탄, 철, 금)을 채굴하면 비트코인을 보상으로 획득.
  - 채굴 확률과 보상은 설정 파일(`config.yml`)에서 조정 가능.

- **거래 시스템**:
  - 플레이어 간 비트코인 구매, 판매, 전송 명령어 제공.
  - VaultAPI와 연동하여 서버 경제 시스템과 통합.

- **실시간 알림**:
  - 채굴 결과와 거래 내역을 실시간으로 표시.
  - 이해하기 쉬운 메시지로 사용자 편의성 향상.

- **데이터 관리**:
  - SQLite를 사용하여 거래 내역, 플레이어 잔고, 관리자 명령어 기록 등을 저장.

---

### **예정된 기능** ✨
- **커스터마이즈 가능한 경제**:
  - 관리자 전용 설정으로 채굴 확률, 보상, 거래 규칙 조정 가능.
- **디스코드 연동**:
  - 채굴 진행 상황 및 거래 알림 발송.
  - 디스코드를 통해 실시간 비트코인 시세 확인.

---

### **사용 방법 (예정)** 🛠️
1. `CCoinCraft.jar` 파일을 서버의 `plugins` 폴더에 추가.
2. 서버를 재시작하여 플러그인 활성화.
3. `config.yml` 파일에서 채굴 확률과 거래 규칙을 설정.
4. 플레이어들이 채굴 및 거래를 통해 서버 경제를 활용.

---

### **기술적 세부 사항** 🖥️
- **마인크래프트 버전**: 1.13 이상 (1.19까지 테스트 예정).
- **개발 언어**: Java (Gradle을 사용한 의존성 관리).
- **의존성**:
  - VaultAPI (서버 경제 연동).
  - SQLite (데이터베이스 관리).

---

### **개발 단계** 🚀
1. **1단계**: 채굴 메커니즘 및 보상 시스템 개발.
2. **2단계**: 거래 명령어 및 VaultAPI 통합.
3. **3단계**: SQLite를 활용한 데이터 관리 구현.
4. **4단계**: 디스코드 연동 및 고급 시장 기능 추가.
