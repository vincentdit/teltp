import { Component, input } from '@angular/core';

@Component({
  selector: 'app-hero',
  standalone: true,
  template: `
    <header class="hero">
      <div class="hero-inner">
        @if (showLogo()) { <span class="hero-badge"><img src="/tirdo-logo.png" alt="TIRDO" /></span> }
        <div class="hero-copy">
          @if (eyebrow()) { <p class="eyebrow">{{ eyebrow() }}</p> }
          <h1>{{ title() }}</h1>
          @if (subtitle()) { <p class="lede">{{ subtitle() }}</p> }
          <ng-content></ng-content>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .hero {
      background:
        radial-gradient(1000px 220px at 12% -40%, rgba(230,163,0,0.16), transparent 60%),
        linear-gradient(135deg, var(--teltp-brand-dark), var(--teltp-brand));
      color: #fff;
      padding: 32px 20px 36px;
    }
    .hero-inner { max-width: var(--teltp-maxw); margin: 0 auto; display: flex; align-items: center; gap: 20px; }
    .hero-badge {
      flex: none; display: inline-flex; align-items: center; justify-content: center;
      background: #fff; border-radius: 14px; padding: 10px; box-shadow: 0 8px 22px rgba(0,0,0,0.22);
    }
    .hero-badge img { display: block; height: 54px; width: auto; }
    .hero-copy { min-width: 0; }
    .eyebrow { margin: 0 0 2px; font-size: 0.7rem; letter-spacing: 0.18em; text-transform: uppercase; color: var(--teltp-accent); font-weight: 700; }
    .hero h1 { margin: 0; font-family: 'Spectral', Georgia, serif; font-size: 1.9rem; line-height: 1.12; }
    .lede { margin: 6px 0 0; opacity: 0.85; }
    @media (max-width: 620px) {
      .hero-inner { flex-direction: column; align-items: flex-start; gap: 14px; }
      .hero h1 { font-size: 1.55rem; }
    }
  `],
})
export class HeroComponent {
  eyebrow = input('');
  title = input.required<string>();
  subtitle = input('');
  showLogo = input(false);
}
