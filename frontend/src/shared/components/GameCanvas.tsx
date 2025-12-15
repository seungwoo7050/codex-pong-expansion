import { GameSnapshot } from '../types/game'

/**
 * [컴포넌트] frontend/src/shared/components/GameCanvas.tsx
 * 설명:
 *   - v0.11.0에서 실시간 경기와 리플레이 화면 모두에서 재사용할 수 있는 읽기 전용 게임 코트 렌더러다.
 *   - 좌/우 패들과 공 위치, 점수 정보를 단순한 CSS 박스로 표현한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/frontend/v0.11.0-replay-browser-and-viewer.md
 */
export function GameCanvas({ snapshot }: { snapshot: GameSnapshot }) {
  const scale = 0.6
  const courtWidth = 800 * scale
  const courtHeight = 480 * scale
  const paddleHeight = 80 * scale
  const paddleWidth = 12
  const ballSize = 12

  return (
    <div className="game-canvas">
      <div className="court" style={{ width: courtWidth, height: courtHeight }}>
        <div
          className="paddle left"
          style={{ height: paddleHeight, width: paddleWidth, top: snapshot.leftPaddleY * scale }}
        />
        <div
          className="paddle right"
          style={{
            height: paddleHeight,
            width: paddleWidth,
            top: snapshot.rightPaddleY * scale,
            right: 0,
          }}
        />
        <div
          className="ball"
          style={{ width: ballSize, height: ballSize, left: snapshot.ballX * scale, top: snapshot.ballY * scale }}
        />
      </div>
      <div className="scoreboard">
        <div className="score">왼쪽: {snapshot.leftScore}</div>
        <div className="score">오른쪽: {snapshot.rightScore}</div>
        <div className="score">목표: {snapshot.targetScore}</div>
      </div>
    </div>
  )
}
