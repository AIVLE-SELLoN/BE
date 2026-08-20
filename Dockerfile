# 빌드 스테이지 - gradle wrapper로 bootJar만 만든다. 테스트는 CI(ci.yml)에서 이미 검증하므로 여기선 건너뛴다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# 실행 스테이지 - JDK가 아닌 JRE만 담아 이미지를 가볍게 유지한다.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd -r sellon && useradd -r -g sellon sellon
COPY --from=build /workspace/build/libs/*.jar app.jar
# file.upload-dir(./uploads)이 /app 밑에 상대경로로 생성되는데, WORKDIR로 만들어진 /app은 root 소유라
# app.jar만 chown하면 sellon 계정이 업로드 디렉터리를 만들 권한이 없다 - /app 자체를 넘겨준다.
RUN chown sellon:sellon /app app.jar
USER sellon

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
