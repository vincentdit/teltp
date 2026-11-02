import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { PortalSlide } from '../../../core/models/portal-slide.model';

@Component({
  selector: 'app-auth-frame',
  standalone: true,
  template: `
    <section class="auth-screen">
      <aside class="auth-brand">
        @if (slides().length === 0) {
          <img src="/tirdo-logo.png" alt="TIRDO" />
          <p class="eyebrow">Tanzania Industrial Research and Development Organization</p>
          <h2>A trusted place to learn, qualify, and certify.</h2>
          <ul>
            <li>National industrial skills programmes</li>
            <li>Certificates you can verify</li>
            <li>Accounts protected with secure sign-in</li>
          </ul>
        } @else {
          <div class="carousel" (mouseenter)="pause()" (mouseleave)="resume()" aria-roledescription="carousel"
               aria-label="TIRDO notices">
            @for (slide of slides(); track slide.uuid; let i = $index) {
              <article class="slide" [class.on]="i === index()" [attr.aria-hidden]="i !== index()">
                @if (slide.hasImage) {
                  <img [src]="imageUrl(slide)" [alt]="''" />
                }
                <div class="copy">
                  <p class="eyebrow">TIRDO</p>
                  <h2>{{ slide.title }}</h2>
                  @if (slide.body) { <p>{{ slide.body }}</p> }
                  @if (slide.linkUrl) {
                    <a [href]="slide.linkUrl" [attr.target]="external(slide) ? '_blank' : null"
                       [attr.rel]="external(slide) ? 'noopener noreferrer' : null">Read more</a>
                  }
                </div>
              </article>
            }
            @if (slides().length > 1) {
              <div class="controls">
                <button type="button" (click)="prev()" aria-label="Previous notice">‹</button>
                <div class="dots">
                  @for (slide of slides(); track slide.uuid; let i = $index) {
                    <button type="button" [class.on]="i === index()" (click)="go(i)"
                            [attr.aria-label]="'Show notice ' + (i + 1) + ': ' + slide.title"></button>
                  }
                </div>
                <button type="button" (click)="next()" aria-label="Next notice">›</button>
              </div>
            }
          </div>
        }
      </aside>

      <div class="auth-panel">
        <header>
          <h1>{{ title() }}</h1>
          @if (lead()) { <p>{{ lead() }}</p> }
        </header>
        <ng-content />
      </div>
    </section>
  `,
  styles: [`
    .auth-screen {
      display: grid;
      grid-template-columns: minmax(280px, 1fr) minmax(320px, 520px);
      gap: 28px;
      align-items: start;
      max-width: 1040px;
      margin: 28px auto 48px;
      padding: 0 20px;
    }
    .auth-brand {
      background: linear-gradient(165deg, var(--teltp-brand-dark), #1a4d98 70%);
      color: #f7f4ea;
      border-radius: 20px;
      padding: 36px 32px;
      display: flex;
      flex-direction: column;
      box-shadow: 0 18px 40px rgba(16, 49, 95, 0.18);
    }
    .auth-brand > img {
      width: 72px; height: 72px; object-fit: contain; background: #fff;
      border-radius: 16px; padding: 8px; margin-bottom: 28px;
    }
    .eyebrow { margin: 0 0 8px; font-size: 0.78rem; letter-spacing: 0.04em; text-transform: uppercase; color: #f0d48a; }
    .auth-brand h2 { margin: 0 0 12px; font-size: 2rem; line-height: 1.15; color: #fff; }
    .auth-brand ul { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 8px; }
    .auth-brand li { padding-left: 18px; position: relative; }
    .auth-brand li::before {
      content: ""; width: 8px; height: 8px; border-radius: 50%; background: var(--teltp-accent);
      position: absolute; left: 0; top: 0.45em;
    }
    .carousel { position: relative; min-height: 420px; margin: -36px -32px; border-radius: 20px; overflow: hidden; }
    .slide {
      position: absolute; inset: 0; opacity: 0; display: flex; align-items: flex-end;
      transition: opacity 0.6s ease; pointer-events: none;
    }
    .slide.on { opacity: 1; z-index: 1; pointer-events: auto; }
    .slide img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
    .copy {
      position: relative; z-index: 1; width: 100%; padding: 28px 28px 64px;
      background: linear-gradient(transparent, rgba(8, 24, 48, 0.92) 55%);
    }
    .copy p { margin: 0; color: #f7f4ea; }
    .copy a { color: #f0d48a; font-weight: 600; }
    .controls {
      position: absolute; z-index: 2; left: 16px; right: 16px; bottom: 14px;
      display: flex; align-items: center; gap: 8px;
    }
    .controls > button, .dots button {
      border: 0; background: rgba(255,255,255,0.2); color: #fff; border-radius: 999px; cursor: pointer;
    }
    .controls > button { width: 32px; height: 32px; font-size: 1.3rem; line-height: 1; }
    .dots { display: flex; gap: 6px; flex: 1; justify-content: center; }
    .dots button { width: 9px; height: 9px; padding: 0; }
    .dots button.on { background: var(--teltp-accent); }
    .auth-panel {
      background: #fff; border: 1px solid var(--teltp-line); border-radius: 20px; padding: 32px 28px;
      box-shadow: 0 10px 30px rgba(28, 31, 26, 0.05); animation: rise 0.35s ease;
    }
    header h1 { margin: 0 0 6px; font-size: 1.85rem; }
    header p { margin: 0 0 18px; color: var(--teltp-muted); }
    @media (max-width: 860px) {
      .auth-screen { grid-template-columns: 1fr; margin-top: 16px; }
      .auth-brand { padding: 22px 20px; }
      .auth-brand > img { width: 48px; height: 48px; margin-bottom: 16px; }
      .auth-brand h2 { font-size: 1.45rem; }
      .auth-brand ul { display: none; }
      .carousel { min-height: 280px; margin: -22px -20px; }
    }
    @media (prefers-reduced-motion: reduce) {
      .auth-panel, .slide { animation: none; transition: none; }
    }
    @keyframes rise {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: none; }
    }
  `],
})
export class AuthFrameComponent {
  private readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly title = input.required<string>();
  readonly lead = input('');
  readonly slides = signal<PortalSlide[]>([]);
  readonly index = signal(0);

  private timer: ReturnType<typeof setInterval> | null = null;
  private paused = false;

  constructor() {
    this.api.get<PortalSlide[]>('/portal/slides').subscribe({
      next: (rows) => {
        this.slides.set(rows ?? []);
        this.resume();
      },
      error: () => this.slides.set([]),
    });
    this.destroyRef.onDestroy(() => this.pause());
  }

  imageUrl(slide: PortalSlide): string {
    return `/api/portal/slides/${slide.uuid}/image?v=${slide.imageVersion ?? 0}`;
  }

  external(slide: PortalSlide): boolean {
    return !!slide.linkUrl && /^https?:\/\//.test(slide.linkUrl);
  }

  go(i: number): void {
    const count = this.slides().length;
    if (count === 0) return;
    this.index.set((i + count) % count);
  }

  next(): void { this.go(this.index() + 1); }
  prev(): void { this.go(this.index() - 1); }

  pause(): void {
    this.paused = true;
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }

  resume(): void {
    this.paused = false;
    if (this.timer || this.slides().length < 2) return;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    this.timer = setInterval(() => {
      if (!this.paused) this.next();
    }, 6000);
  }
}
