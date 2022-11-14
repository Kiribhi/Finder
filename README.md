# Finder
## 무슨 프로그램인가?
주어진 root directory의 모든 하위 경로에 존재하는 압축파일을 찾아낸다.  
  
  
## 왜 만들었는가?
1. 본인은 정보보호병(CERT)으로 군 복무를 수행했음
2. 주요 업무 중 하나가 외부에서 들여온 저장매체를 검사하는 것이었음
3. 당시 백신이 압축파일을 제대로 검사하지 못 해 압축파일은 따로 빼내서 압축을 풀고 검사해야 했음
4. 대체로 압축파일이 너무 많거나, 디렉토리 구조가 너무 복잡해 누락이 발생했음
5. 따라서 압축파일을 누락 없이 찾아낼 수 있는 도구가 필요했음  
  
  
## 압축파일을 완벽하게 찾아내는가?
압축파일 확장명 종류를 검색해서 나온 것들을 최대한 포함한 것에 불과하다.  
따라서 모든 압축파일을 다 찾아낸다고 확정할 수는 없다.  
  
  
## 완벽한 프로그램인가?
상용 프로그램이 아닌데다가, 당시 속해있던 부서에서 '우리끼리 쓰는' 목적으로 만든 것이므로 사용자의 실수에 대한 처리나 UI 등이 불완전하다.  
애초에 GUI는 없이 만들었다가, 만드는 김에 해보자 싶어서 만들어봤다.  
사실 JavaFX로 만들고 싶었으나 Scene Builder도 없는데다가 익숙치 않은 탓에 그나마 익숙한 Swing으로 만들었다.  
다만 이 프로그램을 6개월 정도 쓰다가 전역했는데, 문제 있다는 얘기는 들은 게 없다.  
  
  
  ###여담
당시 이 프로그램을 만든 PC는 군 인트라넷 PC였고, 코딩하기에 적절한 폰트는 한글을 지원하지 않았다.  
군 인트라넷 PC 특성 상 내가 원하는 폰트(개인적으로 D2Coding 선호)를 다운받을 수 없었고,  
그렇다고 주석을 안 쓸 수는 없어서 (없는 실력을 짜내어) 영어로 주석을 써놨다. 그래서 이상한 문장이 많다.  
