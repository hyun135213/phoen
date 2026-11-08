# Server Phone (Fabric, Minecraft 1.21.11)

- P 키: 폰 열기 (우하단에 손에 든 폰 표시)
- 앱: 메시지 / 지도 / 신 설정(OP 전용)
- 신 설정: 공격력·공격사거리·최대체력·이동속도·점프력을 개인 또는 전체에 적용, 대상에게 알림 표시, 공지 보내기
- OP 판정과 설정 적용은 서버에서 처리 (클라이언트 조작으로 우회 불가)
- 설정값은 config/serverphone.json 에 저장

빌드: `./gradlew build` → build/libs/serverphone-1.0.0.jar
(서버와 클라이언트 양쪽 mods 폴더에 jar + Fabric API 필요)
