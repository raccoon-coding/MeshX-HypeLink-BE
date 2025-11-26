<h1 align="center"> HypeLink </h1>
<div align="center"> 
 <img src="https://github.com/user-attachments/assets/807a5735-e104-4bbe-adf5-b7a47830b0cf" width="400"/>
</div>


> 이름 ‘HypeLink’는 “트렌드를 연결하다 (Hype + Link)”라는 의미를 담고 있습니다.  
> **브랜드와 소비자, 그리고 본사와 매장을 하나의 링크로 잇는** B2B 오프라인 매장 자동화 관리 플랫폼입니다.

---

## ✒️ 프로젝트 주요 성과 요약

저는 이 프로젝트에서 **상품·재고·발주 도메인, GPS 배송 API, MSA 전환 및 운영 안정화, 성능 테스트 및 장애 격리**를 중심으로  
백엔드 개발과 운영 전반을 담당했습니다.

- **Monolith → MSA 실시간 전환**을 수행해 서비스 중단 없이 구조를 개선했고  
- Redis 기반 **재고 동시성 제어**로 성능 병목을 제거했으며  
- Kafka **SAGA 패턴**으로 서비스 간 데이터 일관성을 확보하고  
- Kubernetes 운영 환경에서 발생한 **실제 장애를 분석·튜닝**하여  

전체 서비스 가용성 99.4%를 유지했습니다.

---
## 🫂 MeshX 팀원 소개 
<table align="center"> 
    <tbody> 
        <tr> 
            <td align="center"><a href="https://github.com/kbw07"><img src="https://github.com/user-attachments/assets/706e1875-8a3d-4d3e-9a19-d344d6866f23" width="100px;" alt=""/><br /><sub><b> 강병욱 </b></sub></a><br /></td>
            <td align="center"><a href="https://github.com/flionme"><img src="https://github.com/user-attachments/assets/08e896f8-c18f-454a-a44a-2337f585e77f" width="100px;" alt=""/><br /><sub><b> 김성인 </b></sub></a><br /></td>
            <td align="center"><a href="https://github.com/David9733"><img src="https://github.com/user-attachments/assets/4d6ad9a1-ac42-4f36-9259-2b988493cf85" width="100px;" alt=""/><br /><sub><b> 이시욱 </b></sub></a><br /></td>
            <td align="center"><a href="https://github.com/raccoon-coding"><img src="https://github.com/user-attachments/assets/90a33761-0bd8-4b73-a12a-1e24f0c5a6a9" width="100px;" alt=""/><br /><sub><b> 최민성 </b></sub></a><br /></td>
        </tr> 
    </tbody> 
</table>

# 🎬 주문·재고 관리 시스템

## 🎯 프로젝트 소개

HypeLink는 SPA/패션 브랜드의 오프라인 매장을 위한 **주문·재고·발주·물류 통합 관리 플랫폼**입니다.

특히 본 프로젝트에서는 **Monolith → MSA로 실시간 전환**하면서도 **서버 구동 시간(Availability) 99.4% 이상 유지**하는 안정적인 구조로 개선했습니다.

---
### 🔹 솔루션 개요 
MeshX 팀은 이러한 문제를 해결하기 위해 **GPS 기반 물류 추적, 재고 상태 자동화, 고객 데이터 분석, 실시간 소통 허브**를 통합한 **HypeLink 플랫폼**을 설계했습니다. 
- 🚚 **실시간 물류 추적**: GPS 기반 배송 경로·도착 예측 실시간 시각화 
- 📦 **재고·매출 관리 자동화**: 점포별 판매 현황 및 재고 상태 자동 동기화 
- 📊 **고객 데이터 분석**: POS 결제 기반 연령·패턴 분석 및 타겟 마케팅 
- 💬 **본사–직영점 커뮤니케이션**: 실시간 소통 및 불만 접수 시스템 

--- 
## 🧭 프로젝트 시나리오 
| 구분    | 시나리오 설명 | 기대 효과 | 
|-------|----------------|------------| 
| 물류 추적 | 배송 기사의 GPS 데이터를 기반으로 본사 대시보드에서 위치 및 도착 시간 실시간 모니터링 | 지연율 감소, 클레임 감소 | 
| 재고 관리 | POS 데이터 자동 집계 및 재고 대시보드 제공 | 재고 회전율 향상, 물류비 절감 | 
| 고객 분석 | 연령·구매 패턴 분석을 통한 맞춤형 프로모션 | 매출 및 재방문율 증가 | 
| 소통 허브 | 본사-직영점 간 실시간 공지 및 문의 시스템 | 협업 및 현장 대응력 강화 | 

--- 
## ✨ 핵심 기능 요약 
| 기능명 | 설명 | 
|--------|------| 
| 직영점 계정 관리 | 본사에서 각 매장의 계정 생성 및 권한 제어 | 
| 멤버십 관리 | 고객 가입·구매 데이터 통합 관리 | 
| 재고 관리 | 본사 및 직영점 재고 현황 실시간 동기화 | 
| 판매량 관리 | 매장별·제품별 매출 데이터 자동 집계 | 
| POS 관리 | 매장 내 결제 및 재고 파악용 POS 연동 | 
| 매장별 POS 제어 | 각 매장 POS 상태 및 동작 모니터링 | 
| GPS 물류 추적 | 배송 경로, 기사 위치, 도착 예측 실시간 추적 | 
| 본사–직영점 실시간 커뮤니케이션 | 공지·요청·이슈 공유 채널 제공 | 

---

## 🔧 시스템 아키텍처 <img width="9311" alt="MSA_" src="https://github.com/user-attachments/assets/3b30052e-3295-451b-85ab-cdb70c8d27d1" />

## ⚙️ CI/CD 파이프라인 문서 
- <a href="https://github.com/beyond-sw-camp/be17-fin-MeshX-HypeLink-BE/wiki/HypeLink-CI-CD-%ED%8C%8C%EC%9D%B4%ED%94%84%EB%9D%BC%EC%9D%B8-%EB%AC%B8%EC%84%9C"> CI/CD 파이프라인 문서</a> <br/>

# 🌱 제가 담당한 핵심 개발 영역 (Backend)

## 1️⃣ 상품·재고·발주 도메인 개발
- Item, ItemDetail(SKU), Category, Option 도메인 모델링
- 발주 → 출고 → 배송 → 입고 전체 플로우 API 설계
- 판매/입고/재고 증감 처리 로직 개발

### ✔ 해결한 문제
- DB Lock 기반 재고 처리 시 **성능 저하**
- 콜드 스타트 + 트래픽 증가로 인한 **DB Connection Pool 고갈 문제**  
  → **Redis RLock + 이벤트 기반 구조로 안정화**
- 하나의 기능에 트래픽이 몰린다면 다른 기능들한테도 장애가 전파
  → **MSA로 서버 구조를 전환하여 서버 안정화**

---

## 2️⃣ Redis 기반 재고 동시성 제어 (속도 문제 해결)
- `RLock("itemCode:" + itemDetailCode)` 구조로 SKU 단위 Lock 적용
- TTL + Watchdog 기반 Lock 유지
- MSA 환경에서 발생한 경쟁 상태를 제거하여  
  **재고 API의 성능을 Redis 도입 전 대비 크게 향상**

✔ 성능 비교 자료(그래프 & 영상) 첨부 예정
- Redis 도입 전:
  > Lock 경합, 요청 지연, Connection Pool 소진 발생 </br>
  > 평균 응답시간 : 약 2.6초 </br>
  > 최대 응답시간 : 약 11초
  
  https://github.com/user-attachments/assets/62108401-4078-4676-9fed-851f7af59fc4


- Redis 도입 후:
  > TPS 향상, 지연 제거, 안정적 재고 흐름 </br>
  > 평균 응답시간 : 약 1.2초 </br>
  > 최대 응답시간 : 약 5.9초

  https://github.com/user-attachments/assets/42aacf2b-b3e9-4181-9d0d-05422e7dc3dd

---

## 3️⃣ Kafka 기반 SAGA 패턴 (보상 트랜잭션)

### 문제 상황
Monolith → MSA로 분리되면서
- Item 데이터가 **두 시스템에 모두 존재해야** 했고
- 단 한 곳만 실패해도 데이터 불일치가 발생하는 구조였음

### 해결 전략
Kafka 기반 SAGA 패턴 적용  

item.created → monolith.item.sync → success/failure

↳ failure 시 item.rollback 보상 트랜잭션 실행


✔ Monolith·MSA 간 강결합 제거  
✔ 데이터 일관성 유지  
✔ 재고/상품 데이터 불일치 문제 해결

---

## 4️⃣ GPS 배송 추적 API 개발
- 배송기사 GPS 좌표 저장/조회 API
- 배송 상태(출발/배송중/도착)와 프로세스 연동
- 출고–배송–입고 단계 사이의 실시간 연동

---

## 5️⃣ MSA 전환 및 Kubernetes 운영 안정화

### 🔹 Monolith → MSA 실시간 전환 과정
- 서비스 중단 없이 점진적 전환 (Zero-downtime philosophy)
- 데이터 동기화 & 이벤트 흐름 적용
- Core 도메인(Item/Stock)부터 분리

### 🔹 서버 Availability 99.4% 유지
전환 과정에서 다음 요소 최적화:
- Eureka 등록 문제 수정
- LivenessProbe/ReadinessProbe 튜닝
- Pod CPU Limit/Request 조정
- CI/CD 구조 변경 (코드 수정이 일어난 서버만 무중단 배포 진행)

→ **실제 평균 가용성: 99.4% 유지**

<img width="1440" height="780" alt="서버 다운타임" src="https://github.com/user-attachments/assets/e8f3f04f-66b1-4164-9809-ff15cd43a496" />

---

### 🔹 해결한 Kubernetes 운영 문제
| 문제                           | 해결                                        |
|------------------------------|-------------------------------------------|
| Gateway CPU/Thread 부족        | Pod CPU Limit 조정                          |
| Eureka 등록 문제                 | Pod IP로 Eureka 등록하도록 수정                   |
| Probe 실패(Liveness/Readiness) | 초기 Delay & Threshold 최적화                  |
| Traffic 라우팅 지연               | K8s DNS propagation 개선                    |
| 무중단 배포 수정                    | 코드 수정이 발생한 코드만 무중단 배포하도록 Jenkins 파이프라인 수정 |


---

# 🎥 테스트 & 장애 격리 자료

### 📌 장애 격리 테스트
- 특정 서비스 장애 발생 시 전체 서비스 영향 최소화
- Kafka 소비 장애 시 DLQ 처리
- Redis 장애 시 fallback 전략

  https://github.com/user-attachments/assets/90079a1b-8663-41e3-a785-d4427da4f1f6

---

# 📎 전체 레포지토리

- **MSA 브랜치 (Swagger/MSA)**  
  https://github.com/beyond-sw-camp/be17-fin-MeshX-HypeLink-BE/tree/Swagger/MSA

- **Frontend Repository**  
  https://github.com/beyond-sw-camp/be17-fin-MeshX-HypeLink-FE

---

# 🎯 프로젝트를 통해 성장한 점

- **MSA per-DB 환경으로 실시간 전환을 수행하며 99.4% Availability 유지**
- Redis 기반 재고 트랜잭션 개선으로 **고부하에서도 일관된 처리 보장**
- Kafka SAGA로 **서비스 간 데이터 일관성 확보**
- Kubernetes 운영 환경에서 발생하는 **실제 장애를 분석·튜닝할 수 있는 역량 확보**
- Monolith–MSA 혼용 환경에서 **엔드투엔드 백엔드 구조 전반을 이해하고 개선**

---

# 👋 문의 또는 코드 리뷰 환영합니다!
