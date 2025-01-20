# 💎 비트코인 채굴기 플러그인 개발 기획서 (Chanhyeok Coin Craft, CCC)

Chanhyeok Coin Craft (CCC)는 마인크래프트 서버와 디스코드를 연동하여 사용자들이 게임 내에서 비트코인을 채굴하고 거래할 수 있는 시스템을 개발하는 프로젝트입니다. 이 플러그인은 Spigot 기반의 마인크래프트 1.13+ 서버에서 원활하게 작동하며, Java를 사용하여 안정적인 비트코인 채굴 메커니즘과 서버 경제 시스템과의 통합을 구현하였습니다.

---

## 📋 프로젝트 개요

- **프로젝트 명**: 디스코드 연동 마인크래프트 비트코인 채굴 플러그인
- **목표**: 마인크래프트 서버와 디스코드를 연동하여 사용자들이 비트코인을 광물 채굴을 통해 비트코인 거래 및 채굴하는 시스템 개발
- **버전**: 마인크래프트 1.13+ 지원, Spigot 기반 서버 환경 최적화. 최신 Material API 활용 가능성을 고려하여 1.19까지 확장 호환성 테스트 예정

---

## 🛠 기술 스택

- **프로그래밍 언어**: Java
- **서버 플랫폼**: Spigot
- **데이터베이스**: SQLite
- **API 및 라이브러리**: VaultAPI, Discord API (추후)
- **버전 관리**: Git

---

## 🚀 주요 기능

### 1. **비트코인 채굴 시스템**
- **광물 채굴 시 비트코인 획득**: 플레이어가 다양한 광물 블록(석탄, 철, 금, 다이아몬드, 에메랄드 등)을 채굴할 때마다 설정된 확률에 따라 비트코인(사토시) 획득
- **광물별 확률 및 보상 설정 가능**: 각 광물별로 획득 확률과 사토시 수량을 `config.yml` 파일에서 조정 가능

### 2. **명령어 및 인터페이스**
- **비트코인 관련 명령어**: 잔액 확인, 구매/판매, 송금 등의 기능 제공
- **관리자 전용 명령어**: 플레이어의 비트코인 잔고를 조작할 수 있는 `give`, `set`, `take` 명령어 제공

### 3. **데이터베이스 연동**
- **SQLite 사용**: 거래 기록, 플레이어 정보, 보유 코인 수량 등을 안전하게 관리
- **큐 시스템 도입**: 데이터베이스 접근 시 안정성을 확보하기 위해 큐(queue) 시스템 사용

### 4. **VaultAPI 연동**
- **서버 경제 시스템과 통합**: 채굴한 비트코인을 서버 통화로 환전 가능

### 5. **디스코드 연동 (추후 구현)**
- **알림 및 관리 기능**: 채굴 진행 상황, 환전 내역, 구매/판매 이벤트 등을 디스코드로 실시간 알림
- **관리자 전용 명령어 지원**: 플레이어 채굴 금지, 비트코인 지급 등

---

## 🔧 세부 기능 설명

### 🛡 Admin 전용 명령어 (give, set, take)

#### **명령어 개요**
Admin 전용 명령어는 게임 내 비트코인 잔고를 직접 조작할 수 있는 기능을 제공합니다. 이러한 명령어는 관리자에게만 허용되며, **MessageManager**를 통해 다국어 지원 메시지를 출력할 수 있도록 설계되었습니다.

---

#### **명령어 형식**

1. **비트코인 지급 (Give)**
    ```php
    /ccc btc give <플레이어> <수량>
    ```
    - 특정 플레이어에게 지정된 수량의 비트코인을 지급

2. **비트코인 설정 (Set)**
    ```arduino
    /ccc btc set <플레이어> <수량>
    ```
    - 특정 플레이어의 비트코인 잔고를 설정된 수량으로 변경

3. **비트코인 회수 (Take)**
    ```php
    /ccc btc take <플레이어> <수량>
    ```
    - 특정 플레이어의 비트코인 잔고에서 지정된 수량을 차감 (수량 초과 시 전부 차감)

---

#### **메시지 출력 형식 (MessageManager 활용)**

**성공 메시지**
- **지급 성공 시 (Give)**
    ```jsx
    give_success: “[CCC] Successfully gave <amount> coin to <player>!”
    give_success_sub: “[CCC] Someone gave you <amount> coin!”
    ```
- **설정 성공 시 (Set)**
    ```jsx
    set_success: “[CCC] Successfully set <player>'s coin balance to <amount>!”
    set_success_sub: “[CCC] Someone set your coin balance to <amount>!”
    ```
- **회수 성공 시 (Take)**
    ```jsx
    take_success: “[CCC] Successfully took <amount> coin from <player>!”
    take_success_sub: “[CCC] Someone took your <amount> coin!”
    take_success_all: “[CCC] Someone took your all coin!”
    ```

**실패 메시지**
- 잘못된 명령어 형식:
    ```bash
    [CCC] Incorrect command format. Usage: /ccc btc (give/set/take) <player> <amount>
    ```
- 플레이어를 찾을 수 없음:
    ```bash
    [CCC] Player <player> not found!
    ```
- 잔고 부족 (Take 명령어):
    ```csharp
    [CCC] Insufficient Bitcoin in <player>'s balance!
    ```

---

#### **거래 내역 저장**
- **데이터베이스 저장**: 모든 거래 내역은 `SQLite` 데이터베이스에 저장됩니다. (기획서에서는 텍스트 파일로 계획되었으나, 실제 구현에서는 데이터베이스에 저장됨)
- **저장 예시**:
    ```yaml
    [2024-12-21 16:00:00] Admin: AdminUser | Target: Steve | Action: Give | Amount: 10 BTC
    [2024-12-21 16:05:00] Admin: AdminUser | Target: Alex | Action: Take | Amount: 5 BTC
    [2024-12-21 16:10:00] Admin: AdminUser | Target: Steve | Action: Set | Amount: 20 BTC
    ```

---

#### **권한 체크**
- **관리자 전용 여부 확인**: 명령어 실행 전에 플레이어가 관리자 권한을 보유하고 있는지 확인
- **권한 없음 시 처리**: 적절한 오류 메시지 출력

---

### 💰 Buy/Sell 명령어

```bash
/ccc btc buy <수량>: 비트코인 구매
/ccc btc sell <수량>: 비트코인 판매
