# GitHub Actions 배포

PR에서는 `build`와 `test`가 서로 독립된 Ubuntu runner에서 병렬 실행된다. `main`/`develop` push 또는 해당 브랜치의 수동 실행은 두 작업이 성공한 뒤에만 배포한다. `main`은 `production`/`prod`, `develop`은 `development`/`dev`이다. 테스트 실패 시 만들어진 JAR도 배포하지 않는다.

한 번 빌드한 JAR를 Actions artifact로 전달하고 SHA-256을 확인한다. GitHub OIDC의 단기 자격증명으로 비공개 S3에 게시한 뒤, 지정된 SSM 문서와 인스턴스로만 배포 명령을 보낸다. 운영 서버에서는 Gradle이나 Git을 실행하지 않는다. 워크플로에 SSH 키나 AWS 장기 키를 저장하지 않는다.

## 최초 연결

이 저장소의 파일을 병합하는 것만으로 기존 EC2에 SSM/IAM 설정이 생기지는 않는다. 다음 설정을 먼저 완료해야 한다. 설정이 없으면 배포 job은 실패하며 성공으로 표시하지 않는다.

1. GitHub Environments `production`, `development`를 만든다. **Deployment branches and tags를 Selected branches로 설정하여 각각 `main`, `develop` 브랜치만 허용한다.** OIDC subject에는 environment가 들어가므로 IAM trust만으로 브랜치를 제한할 수 없다. production에는 운영자가 선택한 required reviewers를 설정하고, `main`/`develop`의 필수 검사로 `build`, `test`를 등록한다.
2. AWS IAM의 `https://token.actions.githubusercontent.com` OIDC provider를 확인하거나 등록한다. audience는 `sts.amazonaws.com`이다. 계정에 이미 있으면 재사용한다.
3. `infrastructure.yml`을 CloudFormation change set으로 검토한 뒤 적용한다. `ApplicationInstanceId`는 기존 **앱 EC2**, `GitHubRepository`는 실제 저장소, `GitHubOidcProviderArn`은 위 provider이다. `CAPABILITY_IAM`이 필요하다. 기존 DB/EC2 크기, 보안 그룹, 앱용 S3 권한은 변경하지 않는다.
4. 출력 `InstanceProfileName`을 앱 EC2에 연결하고 SSM Agent가 Online인지 확인한다. 기존 instance profile이 있다면 덮어쓰지 말고 필요한 권한을 병합한다. Ubuntu의 기존 SSM Agent가 있으면 이를 사용한다. 인스턴스에서 AWS HTTPS endpoint에 접근할 수 있어야 한다.
5. 앱 서버에 AWS CLI v2, curl, coreutils, util-linux의 `flock`을 준비한다. 검토한 커밋의 `deploy.sh`를 root 소유 `/usr/local/sbin/riku-deploy`로 설치한다. GitHub 배포 역할에는 이 스크립트나 SSM 문서를 수정할 권한을 주지 않는다.

```bash
sudo install -o root -g root -m 755 deploy/deploy.sh /usr/local/sbin/riku-deploy
sudo install -d -o root -g root -m 700 /etc/RIKU
# 아래 파일은 운영자가 root로 작성한다. 값은 CloudFormation 출력에 맞춘다.
# /etc/RIKU/deploy.conf (root:root, 600)
# ARTIFACT_BUCKET='실제-artifact-bucket'
# AWS_REGION='ap-northeast-2'
```

6. 기존 서비스가 `ubuntu`로 `/home/ubuntu/apps/{prod,dev}/app.jar`를 실행하는지 확인한다. `/etc/RIKU/{prod,dev}.env`는 root:root 600을 유지한다. `TimeoutStopSec`은 Spring의 graceful shutdown 30초보다 길게 둔다(기존 90초 유지 가능). management port `9090`, `9091`은 loopback에만 바인딩하며 nginx나 보안 그룹에서 공개하지 않는다.
7. 두 GitHub Environment에 아래 Variables를 각각 등록한다.

| Variable | 값 |
|---|---|
| `AWS_REGION` | `ap-northeast-2` |
| `ARTIFACT_BUCKET` | `ArtifactBucket` 출력 |
| `EC2_INSTANCE_ID` | 앱 EC2 ID |
| `AWS_DEPLOY_ROLE_ARN` | 해당 환경의 RoleArn 출력 |
| `DEPLOY_DOCUMENT_NAME` | 해당 환경의 DocumentName 출력 |

8. 먼저 development에서 성공 배포, 잘못된 체크섬, 기동 실패 및 rollback을 리허설한다. 실패 리허설은 운영에 하지 않는다. 실제 SSM 성공 결과, 앱 기능, nginx 경유 응답을 확인한 뒤 production을 연결한다. `workflow_dispatch`는 선택한 main/develop HEAD를 빌드한다. 과거 커밋을 자동 배포하는 기능은 없다.

## 실패와 복구

- 배포 잠금은 GitHub 환경별 concurrency와 서버의 `flock` 두 곳에서 적용한다. 실행 중인 배포를 새로운 실행이 취소하지 않는다.
- 다운로드·체크섬 실패는 현재 JAR를 바꾸거나 서비스를 재시작하지 않는다. S3의 `commit/run-attempt` 키는 conditional put으로 덮어쓰기를 막는다.
- JAR 포인터를 원자적으로 교체한 뒤 재시작하고 `/actuator/health/readiness`를 검사한다. 이 그룹에는 앱의 readiness와 **DB 연결**이 들어간다. DB 장애도 배포 실패다. S3 접근/비즈니스 기능까지 검증하는 종합 smoke test는 아니다.
- 재시작이나 readiness 실패는 이전 JAR로 복구하고 재기동을 확인한다. 복구 성공 여부와 무관하게 원래 배포는 실패로 끝난다. 복구 실패는 `ROLLBACK FAILED`를 남긴다. 최초 이전 JAR는 별도 legacy 파일로 보존하며, actuator가 없는 legacy에 한해서 이전에 확인한 `/v3/api-docs` 응답으로 복구를 확인한다.
- SSM 전달 제한은 120초, 실행 제한은 900초이다. CI는 최종 성공을 확인할 때까지 기다린다. runner의 타임아웃/취소는 서버 성공을 의미하지 않으므로 출력된 Command ID를 SSM에서 확인한다. SSH 접속을 끊거나 workflow를 취소하여 rollback을 대신하지 않는다.
- 서버에는 최근 5개 release 디렉터리와 직전 정상 JAR를 보존한다. 최초 legacy JAR도 보존한다. S3는 30일, 비현재 버전은 7일 후 만료된다. 서버 정리는 지정 release 디렉터리의 `app.jar`와 빈 디렉터리만 삭제하며, 현재/직전 JAR는 제외한다.
- 단일 JVM 재시작 구간에 짧은 서비스 중단이 있을 수 있다. 무중단 배포 구성은 아니다. DB 스키마 변경은 이 파이프라인에 포함하지 않는다. 향후 migration을 추가할 때는 이전 JAR와 호환되는 expand/contract 및 별도 복구 절차가 필요하다. 특히 기존 dev의 `ddl-auto:update`는 되돌릴 수 없는 변경을 만들 수 있으므로 스키마 변경 커밋에 JAR rollback만 의존하지 않는다.

## 권한 범위와 검증

CloudFormation은 비공개 암호화 artifact 버킷, EC2용 SSM/읽기 권한, 환경별 GitHub 역할, 고정 명령의 SSM 문서 두 개를 만든다. GitHub 역할은 해당 환경 prefix의 `PutObject`와 지정 문서/앱 EC2의 `SendCommand`, 결과 조회만 갖는다. 일반 `AWS-RunShellScript`, IAM 변경, DB EC2, S3 삭제 권한은 없다. SSM 문서 버전 1을 고정하므로 문서를 갱신할 때는 새 이름/버전 및 워크플로를 함께 검토한다. 앱 EC2의 instance profile 권한은 인스턴스 내부 프로세스에서도 접근 가능하다는 점을 고려한다.

```bash
bash ./gradlew --no-daemon test bootJar
bash -n deploy/deploy.sh
python3 -m unittest discover -s deploy/tests -v
actionlint .github/workflows/ci-cd.yml
cfn-lint deploy/infrastructure.yml
```

Java 테스트는 H2와 테스트 전용 JWT 키, mocked S3를 사용한다. shell 테스트는 OS/AWS 호출을 대체하여 checksum, 다운로드 실패, 잠금, 경로 검증, 재시작/health 실패, rollback 실패를 검증한다. 실제 AWS IAM/SSM 연결과 MySQL 배포 리허설은 최초 연결 단계에서 별도로 확인한다.

페이서 수정은 기존 참가자의 ID·가입 시각·출석 상태를 보존하며 그룹만 갱신한다. 페이서에서 제외해도 참가 취소로 간주하지 않는다. 새 참가자는 PENDING이며 출석을 임의로 부여하지 않는다. 출석 코드 생성 뒤 새로 추가된 페이서는 작성자의 기존 수동 출석 기능으로 처리한다. 동일 목록 저장 후 정규런/훈련 종료 포인트 정산까지 회귀 테스트한다.

참고: [GitHub AWS OIDC](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws), [배포 환경과 concurrency](https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/control-deployments).
