import {
  AfterViewInit, Component, ElementRef, EventEmitter, Input, NgZone,
  OnDestroy, Output, ViewChild, inject
} from '@angular/core';
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { buildTerrainGrid, isWebglAvailable } from './heartbreak-3d.util';

@Component({
  selector: 'app-heartbreak-3d',
  standalone: true,
  template: `<canvas #canvas class="hb3d-canvas"></canvas>`,
  styleUrl: './heartbreak-3d.scss'
})
export class Heartbreak3d implements AfterViewInit, OnDestroy {
  /** Route polyline [lat,lng,ele][]; null/short → synthetic ridge. */
  @Input() polyline: [number, number, number][] | null = null;
  /** Emitted when WebGL is unavailable or init fails — parent shows the 2D fallback. */
  @Output() fallback = new EventEmitter<void>();

  @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  private readonly host = inject(ElementRef);
  private readonly zone = inject(NgZone);

  private renderer?: THREE.WebGLRenderer;
  private scene?: THREE.Scene;
  private camera?: THREE.PerspectiveCamera;
  private controls?: OrbitControls;
  private marker?: THREE.Mesh;
  private curve?: THREE.CatmullRomCurve3;
  private frameId = 0;
  private readonly clock = new THREE.Clock();
  private resizeObs?: ResizeObserver;

  ngAfterViewInit(): void {
    if (!isWebglAvailable()) {
      this.fallback.emit();
      return;
    }
    try {
      this.zone.runOutsideAngular(() => this.init());
    } catch {
      this.dispose();
      this.fallback.emit();
    }
  }

  private init(): void {
    const canvas = this.canvasRef.nativeElement;
    const el = this.host.nativeElement as HTMLElement;
    const w = el.clientWidth || 1200;
    const h = el.clientHeight || 600;

    this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    this.renderer.setSize(w, h, false);

    this.scene = new THREE.Scene();
    this.scene.fog = new THREE.FogExp2(0x0d1117, 0.013);

    this.camera = new THREE.PerspectiveCamera(50, w / h, 0.1, 1000);
    this.camera.position.set(10, 40, 78);

    this.scene.add(new THREE.AmbientLight(0xffffff, 0.55));
    const key = new THREE.DirectionalLight(0x8ffc2e, 1.15);
    key.position.set(-40, 70, 50);
    this.scene.add(key);
    const rim = new THREE.DirectionalLight(0x3fb0ff, 0.4);
    rim.position.set(60, 30, -40);
    this.scene.add(rim);

    // --- terrain ---
    const grid = buildTerrainGrid(this.polyline, 96, 48);
    const geo = new THREE.PlaneGeometry(grid.width, grid.depth, grid.segX, grid.segZ);
    geo.rotateX(-Math.PI / 2);
    const pos = geo.attributes['position'] as THREE.BufferAttribute;
    for (let i = 0; i < grid.heights.length; i++) {
      pos.setY(i, grid.heights[i]);
    }
    pos.needsUpdate = true;
    geo.computeVertexNormals();

    const terrain = new THREE.Mesh(
      geo,
      new THREE.MeshStandardMaterial({ color: 0x12331a, flatShading: true, metalness: 0, roughness: 1 })
    );
    this.scene.add(terrain);

    const wire = new THREE.Mesh(
      geo,
      new THREE.MeshBasicMaterial({ color: 0x8ffc2e, wireframe: true, transparent: true, opacity: 0.07 })
    );
    this.scene.add(wire);

    // --- glowing route + climbing marker ---
    const points = grid.routeXYZ.map(p => new THREE.Vector3(p[0], p[1], p[2]));
    if (points.length >= 2) {
      this.curve = new THREE.CatmullRomCurve3(points);
      const tube = new THREE.Mesh(
        new THREE.TubeGeometry(this.curve, 220, 0.5, 8, false),
        new THREE.MeshBasicMaterial({ color: 0x8ffc2e })
      );
      this.scene.add(tube);

      this.marker = new THREE.Mesh(
        new THREE.SphereGeometry(1.3, 16, 16),
        new THREE.MeshBasicMaterial({ color: 0xffffff })
      );
      this.scene.add(this.marker);
    }

    this.controls = new OrbitControls(this.camera, canvas);
    this.controls.enableZoom = false;
    this.controls.enablePan = false;
    this.controls.enableDamping = true;
    this.controls.autoRotate = true;
    this.controls.autoRotateSpeed = 0.55;
    this.controls.minPolarAngle = Math.PI * 0.18;
    this.controls.maxPolarAngle = Math.PI * 0.46;
    this.controls.target.set(0, 8, 0);
    this.controls.update();

    this.resizeObs = new ResizeObserver(() => this.onResize());
    this.resizeObs.observe(el);

    const animate = () => {
      this.frameId = requestAnimationFrame(animate);
      if (this.marker && this.curve) {
        const t = (this.clock.getElapsedTime() * 0.11) % 1;
        this.marker.position.copy(this.curve.getPointAt(t));
      }
      this.controls?.update();
      this.renderer!.render(this.scene!, this.camera!);
    };
    animate();
  }

  private onResize(): void {
    const el = this.host.nativeElement as HTMLElement;
    const w = el.clientWidth;
    const h = el.clientHeight;
    if (!w || !h || !this.renderer || !this.camera) {
      return;
    }
    this.renderer.setSize(w, h, false);
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
  }

  ngOnDestroy(): void {
    this.dispose();
  }

  private dispose(): void {
    cancelAnimationFrame(this.frameId);
    this.resizeObs?.disconnect();
    this.controls?.dispose();
    this.scene?.traverse(obj => {
      const mesh = obj as THREE.Mesh;
      mesh.geometry?.dispose?.();
      const mat = mesh.material as THREE.Material | THREE.Material[] | undefined;
      if (Array.isArray(mat)) {
        mat.forEach(m => m.dispose());
      } else {
        mat?.dispose?.();
      }
    });
    this.renderer?.dispose();
  }
}
