# Finder
### 기능
주어진 root directory의 모든 하위 경로에 존재하는 압축파일을 찾아낸다.<br>
<br>

### 만든 이유
1. 군 CERT에서 정보보호병으로 복무할 당시 주요 업무 중 하나가 외부에서 들여온 저장매체를 검사하는 것이었다.
2. 그런데 당시 군에서 사용하던 백신 프로그램이 이 압축파일을 제대로 검사하지 못 해 압축파일은 따로 빼내서 압축을 풀고 검사해야 했다.
3. 원래는 수작업으로 압축파일을 찾아냈으나 대체로 압축파일이 너무 많거나, 디렉토리 구조가 너무 복잡해 작업이 번거롭고 누락도 종종 발생했다.
4. 따라서 압축파일을 누락 없이 찾아낼 수 있는 도구가 필요했다.
<br>

### 진짜 모든 압축파일을 완벽하게 찾아내나요
압축파일 확장자명으로 알려진 것들로 필터링하는 수준에 그치고 있다.<br>
따라서 세상에 존재하는 모든 압축파일을 다 찾아낼 수 있다고 확언할 수는 없다.<br>
<br>

### 그럼 다른 건 문제 없나요
상용 프로그램으로 배포하는 목적으로 개발한 게 아니고, 당시 속해있던 부서에서 '우리끼리 쓰는' 목적으로 만든 것이므로 사용자의 실수에 대한 처리나 UI 등이 불완전하다.<br>
애초에 GUI는 없이 만들었다가, 만드는 김에 해보자 싶어서 만들어 본 것이다. <br>
원래는 JavaFX를 쓰고 싶었으나 인트라넷에서 Scene Builder를 구하기가 어려워 Swing을 사용했다.<br>
다만 이 프로그램을 6개월 정도 쓰다가 전역했는데, 사용 중에 문제가 발생하거나 전역 후에도 문제 있다는 얘기는 들은 적이 없다.<br>
개발적인 측면에서는 한 파일에 모든 코드가 작성되어 있다는 문제점이 있다(일단 사용하는 것이 우선이었기 때문에).<br>
언제쯤 손을 보게 될지 모르겠다....<br>
<br>

### 아쉬운 점
프로그램을 실행하면 총 몇 개의 압축파일을 찾았고 현재 파일이 몇 번째 파일인지, 그리고 그 파일의 src 경로가 출력된다.<br>
이 부분을 GUI 창에서 했으면 좋았겠지만 방법을 잘 모르겠어 그냥 console 출력을 사용했고, 때문에 출력을 보려면 콘솔이 제공되어야 한다.<br>
즉 Windows라면 명령 프롬프트를 통해 실행해야 한다(`java -jar finder.jar` 등).<br>
<br>


###### 2023/05/08 수정사항
- **주석 한글로 수정**<br>
  원래는 당시 군 내에 한글이 지원되면서 코딩하기 좋은 폰트가 없어서 주석을 영어로 썼었는데, 좀 창피해서 수정했음.<br>
  D2Coding을 쓰고 싶었지만 폰트 때문에 자료교환체계를 쓸 수도 없기 때문에...
- **Logger 클래스 생성자 private으로 설정**<br>
  원래 의도는 Singleton pattern이었으나 어이없게도 생성자가 public으로 되어 있었다....<br>
  사실 Logger 자체가 딱히 필요가 없어서 그냥 클래스 자체를 지워도 그만이긴 하다.
