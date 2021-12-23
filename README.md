# Finder
## 무슨 프로그램인가?
주어진 root directory의 모든 하위 경로에 존재하는 압축파일을 찾아냄


## 왜 만들었는가?
1. 본인은 정보보호병(CERT)으로 군 복무를 수행했음
2. 주요 업무 중 하나가 외부에서 들여온 저장매체를 검사하는 것임
3. 당시 백신이 압축파일을 제대로 검사하지 못 해 압축파일은 따로 빼내서 압축을 풀고 검사해야 했음
4. 손으로 찾다 보니 파일이 누락되는 경우도 있었고, 압축파일도 너무 많고 디렉토리 구조가 너무 복잡한 경우도 있었음
5. 4.과 같은 애로사항이 동기가 되어 만들었음


## 압축파일을 완벽하게 찾아내는가?
구글에 "압축파일 확장명 종류"라고 검색해서 나온 것들을 최대한 포함한 것이므로 '모든' 압축파일을 다 찾아낸다고 확정할 수는 없음


## 완벽한 프로그램인가?
상용 프로그램이 아니고, 당시 속해있던 부서에서 '우리끼리 쓰는' 목적으로 만든 것이므로 이 역시 장담할 수 없음
그러나 핵심적인 기능, 즉 현재 설정되어 있는 확장자명에 따라 압축파일을 찾아내는 기능은 완벽함


## 한국인이고, 군 복무 중에 쓰려고 만든 것인데 왜 주석은 전부 영어인가?
당시 이 프로그램을 만든 PC는 군 인트라넷 PC였고, 코딩하기에 적절한 폰트는 한글을 지원하지 않았음
군 인트라넷 PC 특성 상 내가 원하는 폰트(개인적으로 D2Coding 선호)를 다운받을 수 없었고, 그렇다고 주석을 안 쓸 수는 없어서 있지도 않은 영어 실력 쥐어짜내서 영어로 주석을 달았음.
