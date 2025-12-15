/**
 * [컴포넌트] frontend/src/shared/components/ReplayRenderer.tsx
 * 설명:
 *   - v0.13.0 리플레이 뷰어 전용 렌더러로, 기본 WebGL 경로와 Canvas2D 폴백을 자동 전환한다.
 *   - GPU가 없거나 초기화 실패 시 Canvas2D로 자연스럽게 내려가며, 현재 렌더러를 dev UI에서 확인할 수 있다.
 * 버전: v0.13.0
 * 관련 설계문서:
 *   - design/frontend/v0.13.0-webgl-replay-renderer.md
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { GameSnapshot } from '../types/game'

const COURT_WIDTH = 800
const COURT_HEIGHT = 480
const PADDLE_HEIGHT = 80
const PADDLE_WIDTH = 12
const BALL_SIZE = 12

interface WebGLState {
  gl: WebGLRenderingContext
  program: WebGLProgram
  positionBuffer: WebGLBuffer
  resolutionLocation: WebGLUniformLocation | null
  colorLocation: WebGLUniformLocation | null
  positionLocation: number
}

function safeGetContext(canvas: HTMLCanvasElement, type: string) {
  try {
    return canvas.getContext(type as never)
  } catch (_err) {
    return null
  }
}

function createShader(gl: WebGLRenderingContext, type: number, source: string) {
  const shader = gl.createShader(type)
  if (!shader) {
    throw new Error('셰이더 생성 실패')
  }
  gl.shaderSource(shader, source)
  gl.compileShader(shader)
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    throw new Error(gl.getShaderInfoLog(shader) ?? '셰이더 컴파일 실패')
  }
  return shader
}

function createProgram(gl: WebGLRenderingContext, vertexSource: string, fragmentSource: string) {
  const vertexShader = createShader(gl, gl.VERTEX_SHADER, vertexSource)
  const fragmentShader = createShader(gl, gl.FRAGMENT_SHADER, fragmentSource)
  const program = gl.createProgram()
  if (!program) {
    throw new Error('프로그램 생성 실패')
  }
  gl.attachShader(program, vertexShader)
  gl.attachShader(program, fragmentShader)
  gl.linkProgram(program)
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    throw new Error(gl.getProgramInfoLog(program) ?? '프로그램 링크 실패')
  }
  return program
}

function initWebGL(canvas: HTMLCanvasElement): WebGLState {
  const gl =
    safeGetContext(canvas, 'webgl2') ||
    (safeGetContext(canvas, 'webgl') as WebGLRenderingContext | null) ||
    (safeGetContext(canvas, 'experimental-webgl') as WebGLRenderingContext | null)
  if (!gl) {
    throw new Error('WebGL 컨텍스트를 가져올 수 없습니다.')
  }
  const vertexSource = `
    attribute vec2 a_position;
    uniform vec2 u_resolution;
    void main() {
      vec2 zeroToOne = a_position / u_resolution;
      vec2 zeroToTwo = zeroToOne * 2.0;
      vec2 clipSpace = zeroToTwo - 1.0;
      gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);
    }
  `
  const fragmentSource = `
    precision mediump float;
    uniform vec4 u_color;
    void main() {
      gl_FragColor = u_color;
    }
  `
  const program = createProgram(gl, vertexSource, fragmentSource)
  const positionLocation = gl.getAttribLocation(program, 'a_position')
  const resolutionLocation = gl.getUniformLocation(program, 'u_resolution')
  const colorLocation = gl.getUniformLocation(program, 'u_color')
  const positionBuffer = gl.createBuffer()
  if (!positionBuffer) {
    throw new Error('버퍼 생성 실패')
  }
  gl.enable(gl.BLEND)
  gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
  return { gl, program, positionBuffer, resolutionLocation, colorLocation, positionLocation }
}

function pushRect(glState: WebGLState, x: number, y: number, width: number, height: number, color: [number, number, number, number]) {
  const { gl, program, positionBuffer, resolutionLocation, colorLocation, positionLocation } = glState
  const x2 = x + width
  const y2 = y + height
  const vertices = new Float32Array([
    x, y,
    x2, y,
    x, y2,
    x, y2,
    x2, y,
    x2, y2,
  ])
  gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer)
  gl.bufferData(gl.ARRAY_BUFFER, vertices, gl.DYNAMIC_DRAW)
  gl.useProgram(program)
  gl.enableVertexAttribArray(positionLocation)
  gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0)
  gl.uniform2f(resolutionLocation, gl.canvas.width, gl.canvas.height)
  gl.uniform4f(colorLocation, color[0], color[1], color[2], color[3])
  gl.drawArrays(gl.TRIANGLES, 0, 6)
}

function drawWebGL(glState: WebGLState, snapshot: GameSnapshot) {
  const { gl } = glState
  gl.viewport(0, 0, gl.canvas.width, gl.canvas.height)
  gl.clearColor(12 / 255, 18 / 255, 28 / 255, 1)
  gl.clear(gl.COLOR_BUFFER_BIT)

  const scale = Math.min(gl.canvas.width / COURT_WIDTH, gl.canvas.height / COURT_HEIGHT)
  const offsetX = (gl.canvas.width - COURT_WIDTH * scale) / 2
  const offsetY = (gl.canvas.height - COURT_HEIGHT * scale) / 2

  pushRect(glState, 0, 0, gl.canvas.width, gl.canvas.height, [12 / 255, 18 / 255, 28 / 255, 1])
  pushRect(glState, gl.canvas.width / 2 - 2, 0, 4, gl.canvas.height, [60 / 255, 70 / 255, 85 / 255, 1])

  const paddleHeightPx = PADDLE_HEIGHT * scale
  pushRect(glState, offsetX + 24, offsetY + snapshot.leftPaddleY * scale, PADDLE_WIDTH, paddleHeightPx, [0.9, 0.9, 0.9, 1])
  pushRect(
    glState,
    gl.canvas.width - offsetX - 24 - PADDLE_WIDTH,
    offsetY + snapshot.rightPaddleY * scale,
    PADDLE_WIDTH,
    paddleHeightPx,
    [0.9, 0.9, 0.9, 1],
  )

  const ballLeft = offsetX + snapshot.ballX * scale - BALL_SIZE / 2
  const ballTop = offsetY + snapshot.ballY * scale - BALL_SIZE / 2
  pushRect(glState, ballLeft, ballTop, BALL_SIZE, BALL_SIZE, [1, 180 / 255, 90 / 255, 1])
}

function drawCanvas2D(ctx: CanvasRenderingContext2D, snapshot: GameSnapshot) {
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height)
  const scale = Math.min(ctx.canvas.width / COURT_WIDTH, ctx.canvas.height / COURT_HEIGHT)
  const offsetX = (ctx.canvas.width - COURT_WIDTH * scale) / 2
  const offsetY = (ctx.canvas.height - COURT_HEIGHT * scale) / 2

  ctx.fillStyle = 'rgb(12, 18, 28)'
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height)
  ctx.fillStyle = 'rgb(60, 70, 85)'
  ctx.fillRect(ctx.canvas.width / 2 - 2, 0, 4, ctx.canvas.height)

  const paddleHeightPx = PADDLE_HEIGHT * scale
  ctx.fillStyle = 'rgb(230, 230, 230)'
  ctx.fillRect(offsetX + 24, offsetY + snapshot.leftPaddleY * scale, PADDLE_WIDTH, paddleHeightPx)
  ctx.fillRect(
    ctx.canvas.width - offsetX - 24 - PADDLE_WIDTH,
    offsetY + snapshot.rightPaddleY * scale,
    PADDLE_WIDTH,
    paddleHeightPx,
  )

  ctx.fillStyle = 'rgb(255, 180, 90)'
  ctx.fillRect(offsetX + snapshot.ballX * scale - BALL_SIZE / 2, offsetY + snapshot.ballY * scale - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE)
}

export function ReplayRenderer({
  snapshot,
  width = 960,
  height = 540,
  preferredRenderer,
}: {
  snapshot: GameSnapshot
  width?: number
  height?: number
  preferredRenderer?: 'webgl' | 'canvas2d'
}) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const glStateRef = useRef<WebGLState | null>(null)
  const canvas2DRef = useRef<CanvasRenderingContext2D | null>(null)
  const [renderer, setRenderer] = useState<'webgl' | 'canvas2d'>(preferredRenderer ?? 'canvas2d')
  const size = useMemo(() => ({ width, height }), [width, height])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    canvas.width = size.width
    canvas.height = size.height
    if (preferredRenderer === 'canvas2d') {
      const ctx = safeGetContext(canvas, '2d')
      if (ctx) {
        canvas2DRef.current = ctx
        setRenderer('canvas2d')
      }
      return
    }
    try {
      glStateRef.current = initWebGL(canvas)
      setRenderer('webgl')
    } catch (err) {
      const ctx = safeGetContext(canvas, '2d')
      if (ctx) {
        canvas2DRef.current = ctx
        setRenderer('canvas2d')
      }
    }
  }, [preferredRenderer, size.width, size.height])

  useEffect(() => {
    if (renderer === 'webgl' && glStateRef.current) {
      try {
        drawWebGL(glStateRef.current, snapshot)
      } catch (err) {
        const canvas = canvasRef.current
        if (!canvas) return
        const ctx = safeGetContext(canvas, '2d')
        if (ctx) {
          canvas2DRef.current = ctx
          glStateRef.current = null
          setRenderer('canvas2d')
          drawCanvas2D(ctx, snapshot)
        }
      }
      return
    }
    if (renderer === 'canvas2d' && canvas2DRef.current) {
      drawCanvas2D(canvas2DRef.current, snapshot)
      return
    }
    const canvas = canvasRef.current
    if (canvas) {
      const ctx = safeGetContext(canvas, '2d')
      if (ctx) {
        canvas2DRef.current = ctx
        setRenderer('canvas2d')
        drawCanvas2D(ctx, snapshot)
      }
    }
  }, [renderer, snapshot])

  return (
    <div className="replay-renderer" data-renderer-path={renderer}>
      <canvas ref={canvasRef} width={size.width} height={size.height} aria-label="replay-canvas" />
      <p className="hint">현재 렌더링 경로: {renderer === 'webgl' ? 'WebGL (GPU)' : 'Canvas2D (폴백)'}</p>
    </div>
  )
}
