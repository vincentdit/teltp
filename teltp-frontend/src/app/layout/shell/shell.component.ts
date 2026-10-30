import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../core/services/auth.service';
import { TokenService } from '../../core/services/token.service';
import { I18nKey, I18nService } from '../../core/services/i18n.service';
import { Router } from '@angular/router';

interface NavItem { label: I18nKey; link: string; roles?: string[]; requiresAuth?: boolean; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly tokens = inject(TokenService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  readonly isAuthenticated = this.auth.isAuthenticated;
  readonly username = this.auth.username;

  private readonly allNav: NavItem[] = [
    { label: 'catalogue', link: '/catalog' },
    { label: 'home', link: '/dashboard', requiresAuth: true },
    { label: 'results', link: '/results', roles: ['STUDENT'] },
    { label: 'certificates', link: '/certificates', roles: ['STUDENT'] },
    { label: 'notifications', link: '/notifications', requiresAuth: true },
    { label: 'schedule', link: '/schedule', requiresAuth: true },
    { label: 'forum', link: '/forum', requiresAuth: true },
    { label: 'administration', link: '/admin', roles: ['ADMIN', 'INSTRUCTOR'] },
    { label: 'reports', link: '/admin/reporting', roles: ['ADMIN', 'FINANCE_OFFICER'] },
  ];

  readonly nav = computed<NavItem[]>(() => {
    this.i18n.lang();
    return this.allNav.filter((item) => {
      if (item.roles) return this.tokens.hasAnyRole(item.roles as never);
      if (item.requiresAuth) return this.isAuthenticated();
      return true;
    });
  });

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
