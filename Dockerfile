FROM eclipse-temurin:25-jdk-jammy AS build
RUN apt-get update && apt-get install -y --no-install-recommends maven unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -Pjpro -DskipTests jpro:release \
    && mkdir /jpro \
    && unzip -q target/ledger-jpro.zip -d /jpro \
    && launcher="$(find /jpro -type f -path '*/bin/start.sh' -print -quit)" \
    && test -n "$launcher" \
    && mkdir /release \
    && cp -a "${launcher%/bin/start.sh}/." /release/

FROM eclipse-temurin:25-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends \
      libgtk-3-0 libgl1 libx11-6 libxtst6 libxi6 libxrender1 libfontconfig1 \
      fonts-dejavu-core tesseract-ocr tesseract-ocr-eng gosu \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home ledger
WORKDIR /app
COPY --from=build --chown=ledger:ledger /release/ ./
COPY --chmod=755 deploy/start.sh /usr/local/bin/ledger-start
ENV LEDGER_BROWSER_DATA=/var/data/ledger PORT=8080
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/ledger-start"]
