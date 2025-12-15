import { describe, expect, it, vi, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { ReplayRenderer } from './ReplayRenderer'
import { GameSnapshot } from '../types/game'

const snapshot: GameSnapshot = {
  roomId: 'room',
  ballX: 100,
  ballY: 120,
  ballVelocityX: 0,
  ballVelocityY: 0,
  leftPaddleY: 50,
  rightPaddleY: 80,
  leftScore: 1,
  rightScore: 0,
  targetScore: 5,
  finished: false,
}

afterEach(() => {
  vi.restoreAllMocks()
})

function mockWebGLContext(canvas: HTMLCanvasElement) {
  const gl: any = {
    canvas,
    ARRAY_BUFFER: 0,
    FLOAT: 0,
    TRIANGLES: 0,
    BLEND: 0,
    SRC_ALPHA: 0,
    ONE_MINUS_SRC_ALPHA: 0,
    VERTEX_SHADER: 0,
    FRAGMENT_SHADER: 0,
    COMPILE_STATUS: true,
    LINK_STATUS: true,
    createShader: vi.fn(() => ({})),
    shaderSource: vi.fn(),
    compileShader: vi.fn(),
    getShaderParameter: vi.fn(() => true),
    getShaderInfoLog: vi.fn(() => ''),
    createProgram: vi.fn(() => ({})),
    attachShader: vi.fn(),
    linkProgram: vi.fn(),
    getProgramParameter: vi.fn(() => true),
    getProgramInfoLog: vi.fn(() => ''),
    getAttribLocation: vi.fn(() => 0),
    getUniformLocation: vi.fn(() => ({})),
    createBuffer: vi.fn(() => ({})),
    enable: vi.fn(),
    blendFunc: vi.fn(),
    bindBuffer: vi.fn(),
    bufferData: vi.fn(),
    useProgram: vi.fn(),
    enableVertexAttribArray: vi.fn(),
    vertexAttribPointer: vi.fn(),
    uniform2f: vi.fn(),
    uniform4f: vi.fn(),
    drawArrays: vi.fn(),
    viewport: vi.fn(),
    clearColor: vi.fn(),
    clear: vi.fn(),
  }
  return gl as unknown as WebGLRenderingContext
}

function mockCanvas2D(canvas: HTMLCanvasElement) {
  const ctx: any = {
    canvas,
    clearRect: vi.fn(),
    fillRect: vi.fn(),
  }
  return ctx as CanvasRenderingContext2D
}

describe('ReplayRenderer', () => {
  it('WebGL이 가능하면 GPU 경로를 표시한다', async () => {
    vi.spyOn(HTMLCanvasElement.prototype as any, 'getContext').mockImplementation(function (this: HTMLCanvasElement, type: string) {
      if (type === 'webgl2' || type === 'webgl' || type === 'experimental-webgl') {
        return mockWebGLContext(this)
      }
      if (type === '2d') {
        return mockCanvas2D(this)
      }
      return null
    })

    render(<ReplayRenderer snapshot={snapshot} preferredRenderer="webgl" />)

    await waitFor(() =>
      expect(screen.getByText(/현재 렌더링 경로/).parentElement).toHaveAttribute('data-renderer-path', 'webgl'),
    )
  })

  it('WebGL이 실패하면 Canvas2D로 폴백한다', async () => {
    vi.spyOn(HTMLCanvasElement.prototype as any, 'getContext').mockImplementation(function (this: HTMLCanvasElement, type: string) {
      if (type === '2d') {
        return mockCanvas2D(this)
      }
      return null
    })

    render(<ReplayRenderer snapshot={snapshot} />)

    await waitFor(() => expect(screen.getByText(/현재 렌더링 경로/).parentElement).toHaveAttribute('data-renderer-path', 'canvas2d'))
  })
})
